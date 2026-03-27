package com.trafficlight.data.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import com.trafficlight.domain.service.AggregationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ElasticsearchAggregationAdapter implements AggregationService {

    private final ElasticsearchClient client;

    @Value("${trafficlight.index-name:traffic-lights}")
    private String indexName;

    private static final Set<String> KEYWORD_FIELDS = Set.of(
            "sidoName", "sigunguName", "roadType", "roadRouteNumber",
            "roadRouteDirection", "signalInstallType", "roadShape", "isMainRoad",
            "trafficLightId", "trafficLightCategory", "lightColorType",
            "signalMethod", "signalDuration", "lightSourceType",
            "signalControlMethod", "signalTimeDecisionMethod",
            "flashingLightEnabled", "flashingStartTime", "flashingEndTime",
            "hasPedestrianSignal", "hasRemainingTimeDisplay", "hasAudioSignal",
            "roadSignSerialNumber", "managementPhone", "providerCode", "providerName"
    );

    @Override
    public Map<String, Object> aggregateByRegion() {
        try {
            SearchResponse<Void> response = client.search(s -> s
                    .index(indexName)
                    .size(0)
                    .aggregations("region", a -> a
                            .terms(t -> t.field("sidoName").size(20))
                            .aggregations("sub_region", sa -> sa
                                    .terms(t -> t.field("sigunguName").size(100))
                            )
                    ),
                    Void.class
            );

            List<Map<String, Object>> buckets = response.aggregations().get("region")
                    .sterms().buckets().array().stream()
                    .map(b -> {
                        Map<String, Object> bucket = new HashMap<>();
                        bucket.put("key", b.key().stringValue());
                        bucket.put("count", b.docCount());
                        List<Map<String, Object>> subBuckets = b.aggregations().get("sub_region")
                                .sterms().buckets().array().stream()
                                .map(sb -> {
                                    Map<String, Object> sub = new HashMap<>();
                                    sub.put("key", sb.key().stringValue());
                                    sub.put("count", sb.docCount());
                                    return sub;
                                })
                                .collect(Collectors.toList());
                        bucket.put("subBuckets", subBuckets);
                        return bucket;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("buckets", buckets);
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Region aggregation failed", e);
        }
    }

    @Override
    public Map<String, Object> aggregateByRoadType() {
        return simpleTermsAggregation("roadType");
    }

    @Override
    public Map<String, Object> aggregateBySignalType() {
        return simpleTermsAggregation("trafficLightCategory");
    }

    @Override
    public Map<String, Object> getSummary() {
        try {
            SearchResponse<Void> response = client.search(s -> s
                    .index(indexName)
                    .size(0)
                    .aggregations("sidoName", a -> a.terms(t -> t.field("sidoName").size(20)))
                    .aggregations("roadType", a -> a.terms(t -> t.field("roadType").size(20)))
                    .aggregations("trafficLightCategory", a -> a.terms(t -> t.field("trafficLightCategory").size(20)))
                    .aggregations("lightColorType", a -> a.terms(t -> t.field("lightColorType").size(20))),
                    Void.class
            );

            Map<String, Object> summary = new HashMap<>();
            for (String aggName : List.of("sidoName", "roadType", "trafficLightCategory", "lightColorType")) {
                List<Map<String, Object>> buckets = response.aggregations().get(aggName)
                        .sterms().buckets().array().stream()
                        .map(b -> {
                            Map<String, Object> bucket = new HashMap<>();
                            bucket.put("key", b.key().stringValue());
                            bucket.put("count", b.docCount());
                            return bucket;
                        })
                        .collect(Collectors.toList());
                Map<String, Object> aggResult = new HashMap<>();
                aggResult.put("buckets", buckets);
                summary.put(aggName, aggResult);
            }
            return summary;
        } catch (IOException e) {
            throw new RuntimeException("Summary aggregation failed", e);
        }
    }

    private Map<String, Object> simpleTermsAggregation(String field) {
        try {
            String esField = KEYWORD_FIELDS.contains(field) ? field : field + ".keyword";
            SearchResponse<Void> response = client.search(s -> s
                    .index(indexName)
                    .size(0)
                    .aggregations("agg", a -> a.terms(t -> t.field(esField).size(50))),
                    Void.class
            );

            List<Map<String, Object>> buckets = response.aggregations().get("agg")
                    .sterms().buckets().array().stream()
                    .map(b -> {
                        Map<String, Object> bucket = new HashMap<>();
                        bucket.put("key", b.key().stringValue());
                        bucket.put("count", b.docCount());
                        return bucket;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("buckets", buckets);
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Terms aggregation failed for " + field, e);
        }
    }
}
