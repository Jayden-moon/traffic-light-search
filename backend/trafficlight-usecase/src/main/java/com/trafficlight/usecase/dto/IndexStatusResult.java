package com.trafficlight.usecase.dto;

import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Getter
@AllArgsConstructor
@Builder
public class IndexStatusResult {
    private final long totalRecords;
    private final long indexed;
    private final long failed;
    private final long durationMs;
    private final boolean indexExists;
}
