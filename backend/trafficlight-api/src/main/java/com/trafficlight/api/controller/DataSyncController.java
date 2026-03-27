package com.trafficlight.api.controller;

import com.trafficlight.api.dto.IndexStatusResponse;
import com.trafficlight.usecase.DataSyncUseCase;
import com.trafficlight.usecase.dto.IndexStatusResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class DataSyncController {

    private final DataSyncUseCase dataSyncUseCase;

    /**
     * Sync data from public API to Elasticsearch.
     * Uses upsert - safe to call multiple times without duplicates.
     */
    @PostMapping
    public ResponseEntity<IndexStatusResponse> syncFromApi() {
        IndexStatusResult result = dataSyncUseCase.syncFromApi();
        return ResponseEntity.ok(IndexStatusResponse.from(result));
    }
}
