package com.trafficlight.api.dto;

import com.trafficlight.usecase.dto.IndexStatusResult;
import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Getter
@AllArgsConstructor
@Builder
public class IndexStatusResponse {
    private final long totalRecords;
    private final long indexed;
    private final long failed;
    private final long durationMs;
    private final boolean indexExists;

    public static IndexStatusResponse from(IndexStatusResult result) {
        return IndexStatusResponse.builder()
                .totalRecords(result.getTotalRecords())
                .indexed(result.getIndexed())
                .failed(result.getFailed())
                .durationMs(result.getDurationMs())
                .indexExists(result.isIndexExists())
                .build();
    }
}
