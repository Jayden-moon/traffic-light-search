package com.trafficlight.data.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.CountRequest;
import co.elastic.clients.elasticsearch.core.CountResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.trafficlight.domain.model.TrafficLight;
import com.trafficlight.domain.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ElasticsearchSearchAdapter implements SearchService {

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
    public List<TrafficLight> search(String query, Map<String, String> filters, int page, int size) {
        try {
            SearchRequest request = buildSearchRequest(query, filters, page, size);
            SearchResponse<Map> response = client.search(request, Map.class);
            return response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .map(source -> {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) source;
                        return TrafficLight.fromMap(map);
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Search failed", e);
        }
    }

    @Override
    public long count(String query, Map<String, String> filters) {
        try {
            Query esQuery = buildQuery(query, filters);
            CountResponse response = client.count(c -> c.index(indexName).query(esQuery));
            return response.count();
        } catch (IOException e) {
            throw new RuntimeException("Count failed", e);
        }
    }

    @Override
    public List<TrafficLight> geoSearch(double lat, double lon, String distance, int page, int size) {
        try {
            SearchResponse<Map> response = client.search(s -> s
                    .index(indexName)
                    .query(q -> q
                            .geoDistance(g -> g
                                    .field("location")
                                    .location(l -> l.latlon(ll -> ll.lat(lat).lon(lon)))
                                    .distance(distance)
                            )
                    )
                    .sort(so -> so
                            .geoDistance(gd -> gd
                                    .field("location")
                                    .location(l -> l.latlon(ll -> ll.lat(lat).lon(lon)))
                                    .order(SortOrder.Asc)
                            )
                    )
                    .from(page * size)
                    .size(size),
                    Map.class
            );
            return response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .map(source -> {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) source;
                        return TrafficLight.fromMap(map);
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Geo search failed", e);
        }
    }

    @Override
    public long geoCount(double lat, double lon, String distance) {
        try {
            CountResponse response = client.count(c -> c
                    .index(indexName)
                    .query(q -> q
                            .geoDistance(g -> g
                                    .field("location")
                                    .location(l -> l.latlon(ll -> ll.lat(lat).lon(lon)))
                                    .distance(distance)
                            )
                    )
            );
            return response.count();
        } catch (IOException e) {
            throw new RuntimeException("Geo count failed", e);
        }
    }

    @Override
    public List<String> getFilterOptions(String field, Map<String, String> parentFilter) {
        try {
            String esField = keywordField(field);
            SearchResponse<Void> response = client.search(s -> {
                s.index(indexName).size(0);
                if (parentFilter != null && !parentFilter.isEmpty()) {
                    s.query(q -> {
                        BoolQuery.Builder bool = new BoolQuery.Builder();
                        parentFilter.forEach((k, v) ->
                                bool.filter(f -> f.term(t -> t.field(keywordField(k)).value(v)))
                        );
                        return q.bool(bool.build());
                    });
                }
                s.aggregations("options", a -> a.terms(t -> t.field(esField).size(100)));
                return s;
            }, Void.class);

            return response.aggregations().get("options").sterms().buckets().array().stream()
                    .map(b -> b.key().stringValue())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Filter options failed", e);
        }
    }

    private SearchRequest buildSearchRequest(String query, Map<String, String> filters, int page, int size) {
        return SearchRequest.of(s -> s
                .index(indexName)
                .query(buildQuery(query, filters))
                .from(page * size)
                .size(size)
        );
    }

    private Query buildQuery(String query, Map<String, String> filters) {
        BoolQuery.Builder bool = new BoolQuery.Builder();
        boolean hasCondition = false;

        if (query != null && !query.isBlank()) {
            bool.must(m -> m.multiMatch(mm -> mm
                    .query(query)
                    .fields("roadNameAddress", "lotNumberAddress", "roadRouteName", "managementAgency")
            ));
            hasCondition = true;
        }

        if (filters != null) {
            for (Map.Entry<String, String> entry : filters.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    String esField = keywordField(entry.getKey());
                    String value = entry.getValue();
                    bool.filter(f -> f.term(t -> t.field(esField).value(value)));
                    hasCondition = true;
                }
            }
        }

        if (!hasCondition) {
            return Query.of(q -> q.matchAll(m -> m));
        }

        return Query.of(q -> q.bool(bool.build()));
    }

    private String keywordField(String field) {
        if (KEYWORD_FIELDS.contains(field)) {
            return field;
        }
        return field + ".keyword";
    }
}
