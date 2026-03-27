package com.trafficlight.api.dto;

import com.trafficlight.usecase.dto.AggregationResult;
import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
@Builder
public class AggregationResponse {
    private final List<BucketResponse> buckets;

    @Getter
    @AllArgsConstructor
    @Builder
    public static class BucketResponse {
        private final String key;
        private final long count;
        private final List<BucketResponse> subBuckets;
    }

    public static AggregationResponse from(AggregationResult result) {
        List<BucketResponse> buckets = result.getBuckets().stream()
                .map(AggregationResponse::toBucketResponse)
                .collect(Collectors.toList());
        return AggregationResponse.builder().buckets(buckets).build();
    }

    private static BucketResponse toBucketResponse(AggregationResult.Bucket bucket) {
        List<BucketResponse> subBuckets = null;
        if (bucket.getSubBuckets() != null) {
            subBuckets = bucket.getSubBuckets().stream()
                    .map(AggregationResponse::toBucketResponse)
                    .collect(Collectors.toList());
        }
        return BucketResponse.builder()
                .key(bucket.getKey())
                .count(bucket.getCount())
                .subBuckets(subBuckets)
                .build();
    }
}
