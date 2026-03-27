package com.trafficlight.usecase;

import com.trafficlight.domain.service.DataCollectorService;
import com.trafficlight.domain.service.IndexService;
import com.trafficlight.usecase.dto.IndexStatusResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class DataSyncUseCase {

    private final DataCollectorService dataCollectorService;
    private final IndexService indexService;

    private static final int PAGE_SIZE = 1000;

    /**
     * Sync all data from public API to Elasticsearch.
     * Uses upsert (document ID based) to avoid duplicates.
     */
    public IndexStatusResult syncFromApi() {
        long startTime = System.currentTimeMillis();

        // Ensure index exists
        if (!indexService.indexExists()) {
            indexService.createIndex();
        }

        long totalCount = dataCollectorService.getTotalCount();
        if (totalCount == 0) {
            log.warn("API returned 0 total count");
            return IndexStatusResult.builder()
                    .totalRecords(0).indexed(0).failed(0)
                    .durationMs(System.currentTimeMillis() - startTime)
                    .indexExists(true).build();
        }

        int totalPages = (int) Math.ceil((double) totalCount / PAGE_SIZE);
        long totalIndexed = 0;
        long totalFailed = 0;

        log.info("Starting API sync: {} total records, {} pages", totalCount, totalPages);

        for (int page = 1; page <= totalPages; page++) {
            List<Map<String, Object>> records = dataCollectorService.fetchPage(page, PAGE_SIZE);

            if (records.isEmpty()) {
                log.warn("Page {} returned empty, stopping", page);
                break;
            }

            long indexed = indexService.bulkUpsert(records);
            totalIndexed += indexed;
            totalFailed += (records.size() - indexed);

            if (page % 10 == 0) {
                log.info("Progress: page {}/{}, indexed so far: {}", page, totalPages, totalIndexed);
            }
        }

        long durationMs = System.currentTimeMillis() - startTime;
        log.info("API sync complete: {} indexed, {} failed, {}ms", totalIndexed, totalFailed, durationMs);

        return IndexStatusResult.builder()
                .totalRecords(totalCount)
                .indexed(totalIndexed)
                .failed(totalFailed)
                .durationMs(durationMs)
                .indexExists(true)
                .build();
    }
}
