package com.mulehunter.backend.controller;

import java.time.Instant;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.mulehunter.backend.repository.ShapExplanationRepository;
import com.mulehunter.backend.DTO.ShapExplanationDTO;
import com.mulehunter.backend.model.ShapExplanation;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;


@RestController
@RequestMapping("/backend/api/visual/shap-explanations")
public class ShapExplanationController {

    private final ShapExplanationRepository repository;

    public ShapExplanationController(ShapExplanationRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/batch")
    public Mono<String> saveBatch(@RequestBody List<ShapExplanationDTO> payload) {

        return Flux.fromIterable(payload)
                .flatMap(dto -> {
                    ShapExplanation e = new ShapExplanation();
                    e.setNodeId(dto.getNodeId());
                    e.setAnomalyScore(dto.getAnomalyScore());
                    e.setTopFactors(dto.getTopFactors());
                    e.setModel(dto.getModel());
                    e.setSource(dto.getSource());
                    e.setUpdatedAt(Instant.now());
                    return repository.save(e);
                })
                .then(Mono.just("SHAP explanations stored successfully"));
    }
}
