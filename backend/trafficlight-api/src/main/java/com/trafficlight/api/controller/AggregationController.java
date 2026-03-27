package com.trafficlight.api.controller;

import com.trafficlight.usecase.AggregationUseCase;
import com.trafficlight.usecase.dto.AggregationResult;
import com.trafficlight.api.dto.AggregationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/aggregations")
@RequiredArgsConstructor
public class AggregationController {

    private final AggregationUseCase aggregationUseCase;

    @GetMapping("/by-region")
    public ResponseEntity<AggregationResponse> byRegion() {
        AggregationResult result = aggregationUseCase.aggregateByRegion();
        return ResponseEntity.ok(AggregationResponse.from(result));
    }

    @GetMapping("/by-road-type")
    public ResponseEntity<AggregationResponse> byRoadType() {
        AggregationResult result = aggregationUseCase.aggregateByRoadType();
        return ResponseEntity.ok(AggregationResponse.from(result));
    }

    @GetMapping("/by-signal-type")
    public ResponseEntity<AggregationResponse> bySignalType() {
        AggregationResult result = aggregationUseCase.aggregateBySignalType();
        return ResponseEntity.ok(AggregationResponse.from(result));
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, AggregationResponse>> summary() {
        Map<String, AggregationResult> results = aggregationUseCase.getSummary();
        Map<String, AggregationResponse> response = new HashMap<>();
        results.forEach((k, v) -> response.put(k, AggregationResponse.from(v)));
        return ResponseEntity.ok(response);
    }
}
