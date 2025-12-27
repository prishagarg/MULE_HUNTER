package com.mulehunter.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.mulehunter.backend.model.Transaction;
import com.mulehunter.backend.model.TransactionRequest;
import com.mulehunter.backend.repository.TransactionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Mono;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for processing financial transactions.
 * It orchestrates the flow between the Database, Node Analytics, and the AI
 * Fraud Engine.
 */
@Service
public class TransactionService {

    private final TransactionRepository repository;
    private final NodeEnrichedService nodeEnrichedService;
    private final WebClient webClient;

    @Value("${visual.internal-api-key}")
    private String visualInternalApiKey;

    public TransactionService(TransactionRepository repository, NodeEnrichedService nodeEnrichedService) {
        this.repository = repository;
        this.nodeEnrichedService = nodeEnrichedService;

        // Initialize connection to the AI Engine
        this.webClient = WebClient.builder()
                .baseUrl("http://host.docker.internal:8000")
                .build();
    }

    public Mono<Transaction> createTransaction(TransactionRequest request) {
        System.out.println("🟢 TX RECEIVED :: " + request);
        Transaction tx = Transaction.from(request);
        Long sourceNodeId;
        Long targetNodeId;
        try {
            sourceNodeId = Long.parseLong(tx.getSourceAccount());
            targetNodeId = Long.parseLong(tx.getTargetAccount());
        } catch (Exception e) {
            System.err.println("❌ NODE ID PARSE FAILED");
            e.printStackTrace();
            return Mono.error(e);
        }
        double amount = tx.getAmount().doubleValue();

        // 1. Save Initial "Ground Truth"
        return repository.save(tx)
                .doOnSubscribe(s -> System.out.println("Saving transacation..."))
                .doOnSuccess(saved -> System.out.println("Transaction saved with ID:" + saved.getId()))
                .doOnError(e -> {
                    System.out.println("Transaction Save Failed");
                    e.printStackTrace();
                })
                .flatMap(savedTx ->
                // 2. Update Graph Analytics
                Mono.when(
                        nodeEnrichedService.handleOutgoing(sourceNodeId, amount).doOnError(e -> {
                            System.out.println("Handle Outgoing Failed");
                            e.printStackTrace();
                        }),
                        nodeEnrichedService.handleIncoming(targetNodeId, amount).doOnError(e -> {
                            System.out.println("Handle Incoming Failed");
                            e.printStackTrace();
                        }))
                        // 3. Call AI Model
                        .then(callAiModel(sourceNodeId, targetNodeId, amount)
                                .doOnSubscribe(s -> System.out.println("Calling AI Engine")))
                        .doOnError(e -> {
                            System.out.println("AI Model Call Failed");
                            e.printStackTrace();
                        }).defaultIfEmpty(null)
                        .flatMap(aiResponse -> triggerVisualMlPipeline(savedTx)
                                .doOnSubscribe(s -> System.out.println("Triggering Visual ML")).doOnError(e -> {
                                    System.out.println("VISUAL ML TRIGGER FAILED");
                                    e.printStackTrace();
                                })
                                .then(processAiResponse(savedTx, aiResponse))));

    }

    private Mono<JsonNode> callAiModel(Long source, Long target, double amount) {
        Map<String, Object> aiPayload = Map.of(
                "source_id", source,
                "target_id", target,
                "amount", amount,
                "timestamp", Instant.now().toString());

        return webClient.post()
                .uri("/analyze-transaction")
                .bodyValue(aiPayload)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .onErrorResume(e -> {
                    System.err.println("AI SERVICE CONNECTION FAILED: " + e.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<Void> triggerVisualMlPipeline(Transaction tx) {

        Map<String, Object> payload = Map.of(
                "trigger", "TRANSACTION_EVENT",
                "transactionId", tx.getId(),
                "nodes", List.of(
                        Map.of("nodeId", Long.parseLong(tx.getSourceAccount()), "role", "SOURCE"),
                        Map.of("nodeId", Long.parseLong(tx.getTargetAccount()), "role", "TARGET")));

        return webClient.post()
                .uri("/visual/reanalyze/nodes")
                .header("X-INTERNAL-API-KEY", visualInternalApiKey)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(e -> {
                    System.err.println("VISUAL ML PIPELINE TRIGGER FAILED: " + e.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<Transaction> processAiResponse(Transaction tx, JsonNode aiResponse) {
        System.out.println(" AI RESPONSE RECEIVED: " + aiResponse);
        if (aiResponse != null && aiResponse.has("risk_score")) {

            double riskScore = aiResponse.get("risk_score").asDouble();
            String verdict = aiResponse.get("verdict").asText();

            System.out.println(
                    "🤖 AI ORCHESTRATOR :: Transaction ID " + tx.getId()
                            + " | Risk Score: " + riskScore
                            + " | Verdict: " + verdict);

            tx.setRiskScore(riskScore);
            tx.setVerdict(verdict);
            tx.setSuspectedFraud(riskScore > 0.5);

            return repository.save(tx)
                    .doOnSuccess(t -> System.out.println(" AI RESULT SAVED"))
                    .doOnError(e -> {
                        System.err.println(" FAILED TO SAVE AI RESULT");
                        e.printStackTrace();
                    });
        }
        System.out.println("No AI result, returning base transaction");
        return Mono.just(tx);
    }

}