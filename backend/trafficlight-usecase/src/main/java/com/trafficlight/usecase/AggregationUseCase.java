package com.trafficlight.usecase;

import com.trafficlight.domain.service.AggregationService;
import com.trafficlight.usecase.dto.AggregationResult;
import com.trafficlight.usecase.dto.AggregationResult.Bucket;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class AggregationUseCase {

    private final AggregationService aggregationService;

    public AggregationResult aggregateByRegion() {
        return toAggregationResult(aggregationService.aggregateByRegion());
    }

    public AggregationResult aggregateByRoadType() {
        return toAggregationResult(aggregationService.aggregateByRoadType());
    }

    public AggregationResult aggregateBySignalType() {
        return toAggregationResult(aggregationService.aggregateBySignalType());
    }

    public Map<String, AggregationResult> getSummary() {
        Map<String, Object> raw = aggregationService.getSummary();
        Map<String, AggregationResult> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            if (entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> aggMap = (Map<String, Object>) entry.getValue();
                result.put(entry.getKey(), toAggregationResult(aggMap));
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private AggregationResult toAggregationResult(Map<String, Object> raw) {
        List<Bucket> buckets = new ArrayList<>();
        Object bucketsObj = raw.get("buckets");
        if (bucketsObj instanceof List) {
            for (Object item : (List<?>) bucketsObj) {
                if (item instanceof Map) {
                    buckets.add(toBucket((Map<String, Object>) item));
                }
            }
        }
        return AggregationResult.builder().buckets(buckets).build();
    }

    @SuppressWarnings("unchecked")
    private Bucket toBucket(Map<String, Object> raw) {
        String key = String.valueOf(raw.get("key"));
        long count = raw.get("count") instanceof Number ? ((Number) raw.get("count")).longValue() : 0;
        List<Bucket> subBuckets = null;
        if (raw.containsKey("subBuckets") && raw.get("subBuckets") instanceof List) {
            subBuckets = new ArrayList<>();
            for (Object sub : (List<?>) raw.get("subBuckets")) {
                if (sub instanceof Map) {
                    subBuckets.add(toBucket((Map<String, Object>) sub));
                }
            }
        }
        return Bucket.builder().key(key).count(count).subBuckets(subBuckets).build();
    }
}
