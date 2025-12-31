package com.mulehunter.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/visual")
@CrossOrigin(origins = "http://13.48.249.157:3000", allowCredentials = "true")
public class VisualStreamController {

        private final WebClient webClient;

        @Value("${visual.internal-api-key}")
        private String internalApiKey;

        public VisualStreamController(WebClient.Builder builder) {
                this.webClient = builder
                                .baseUrl("http://13.61.143.100:8000")
                                .build();
        }

        @GetMapping(value = "/stream/unsupervised", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        public Flux<ServerSentEvent<String>> streamUnsupervised(
                        @RequestParam String transactionId,
                        @RequestParam Long nodeId) {

                System.out.println("➡ SSE proxy → tx=" + transactionId + " node=" + nodeId);

                return webClient.get()
                                .uri(uriBuilder -> uriBuilder
                                                .path("/visual-analytics/api/visual/stream/unsupervised")
                                                .queryParam("transactionId", transactionId)
                                                .queryParam("nodeId", nodeId)
                                                .build())
                                .header("X-INTERNAL-API-KEY", internalApiKey)
                                .accept(MediaType.TEXT_EVENT_STREAM)
                                .retrieve()
                                .bodyToFlux(
                                                new ParameterizedTypeReference<ServerSentEvent<String>>() {
                                                });
        }
}
