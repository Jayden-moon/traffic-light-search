package com.trafficlight.usecase.dto;

import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
@Builder
public class AggregationResult {
    private final List<Bucket> buckets;

    @Getter
    @AllArgsConstructor
    @Builder
    public static class Bucket {
        private final String key;
        private final long count;
        private final List<Bucket> subBuckets;
    }
}
