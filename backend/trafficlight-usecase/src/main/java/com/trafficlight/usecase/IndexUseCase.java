package com.trafficlight.usecase;

import com.trafficlight.domain.service.IndexService;
import com.trafficlight.usecase.dto.IndexStatusResult;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class IndexUseCase {

    private final IndexService indexService;

    public void createIndex() {
        indexService.createIndex();
    }

    public IndexStatusResult loadData() {
        long startTime = System.currentTimeMillis();
        long indexed = indexService.loadData();
        long durationMs = System.currentTimeMillis() - startTime;
        boolean exists = indexService.indexExists();
        long docCount = indexService.documentCount();

        return IndexStatusResult.builder()
                .totalRecords(indexed)
                .indexed(docCount)
                .failed(indexed - docCount)
                .durationMs(durationMs)
                .indexExists(exists)
                .build();
    }

    public void deleteIndex() {
        indexService.deleteIndex();
    }

    public IndexStatusResult getStatus() {
        boolean exists = indexService.indexExists();
        long docCount = exists ? indexService.documentCount() : 0;

        return IndexStatusResult.builder()
                .totalRecords(docCount)
                .indexed(docCount)
                .failed(0)
                .durationMs(0)
                .indexExists(exists)
                .build();
    }
}
