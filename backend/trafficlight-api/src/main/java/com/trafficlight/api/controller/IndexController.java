package com.trafficlight.api.controller;

import com.trafficlight.usecase.IndexUseCase;
import com.trafficlight.usecase.dto.IndexStatusResult;
import com.trafficlight.api.dto.IndexStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/index")
@RequiredArgsConstructor
public class IndexController {

    private final IndexUseCase indexUseCase;

    @PostMapping("/create")
    public ResponseEntity<String> createIndex() {
        indexUseCase.createIndex();
        return ResponseEntity.ok("Index created");
    }

    @PostMapping("/load")
    public ResponseEntity<IndexStatusResponse> loadData() {
        IndexStatusResult result = indexUseCase.loadData();
        return ResponseEntity.ok(IndexStatusResponse.from(result));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteIndex() {
        indexUseCase.deleteIndex();
        return ResponseEntity.ok("Index deleted");
    }

    @GetMapping("/status")
    public ResponseEntity<IndexStatusResponse> getStatus() {
        IndexStatusResult result = indexUseCase.getStatus();
        return ResponseEntity.ok(IndexStatusResponse.from(result));
    }
}
