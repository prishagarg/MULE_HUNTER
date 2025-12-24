package com.mulehunter.backend.service;

import org.springframework.stereotype.Service;

import com.mulehunter.backend.model.Transaction;
import com.mulehunter.backend.model.TransactionRequest;
import com.mulehunter.backend.repository.TransactionRepository;

import reactor.core.publisher.Mono;

@Service
public class TransactionService {

    private final TransactionRepository repository;
    private final NodeEnrichedService nodeEnrichedService;

    public TransactionService(
            TransactionRepository repository,
            NodeEnrichedService nodeEnrichedService) {
        this.repository = repository;
        this.nodeEnrichedService = nodeEnrichedService;
    }

    public Mono<Transaction> createTransaction(TransactionRequest request) {

        Transaction tx = Transaction.from(request);

        Long sourceNodeId = Long.parseLong(tx.getSourceAccount());
        Long targetNodeId = Long.parseLong(tx.getTargetAccount());
        double amount = tx.getAmount().doubleValue();

        // Save transaction first (ground truth)
        return repository.save(tx)
                .flatMap(savedTx ->
                        // Update node features asynchronously
                        Mono.when(
                                nodeEnrichedService.handleOutgoing(sourceNodeId, amount),
                                nodeEnrichedService.handleIncoming(targetNodeId, amount)
                        ).thenReturn(savedTx)
                );
    }
}
