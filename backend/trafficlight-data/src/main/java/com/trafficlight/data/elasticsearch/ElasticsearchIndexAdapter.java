package com.trafficlight.data.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import com.trafficlight.data.loader.JsonDataLoader;
import com.trafficlight.domain.service.IndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchIndexAdapter implements IndexService {

    private final ElasticsearchClient client;
    private final RestClient restClient;

    @Value("${trafficlight.index-name:traffic-lights}")
    private String indexName;

    @Value("${trafficlight.data-file:./data/전국신호등표준데이터.json}")
    private String dataFile;

    @Value("${trafficlight.bulk-size:1000}")
    private int bulkSize;

    @Override
    public void createIndex() {
        try {
            boolean exists = client.indices().exists(e -> e.index(indexName)).value();
            if (exists) {
                log.info("Index {} already exists", indexName);
                return;
            }

            // Read mapping from classpath and create via RestClient (reliable for settings+mappings)
            ClassPathResource resource = new ClassPathResource("es-mapping.json");
            String mappingJson;
            try (InputStream is = resource.getInputStream()) {
                mappingJson = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }

            Request request = new Request("PUT", "/" + indexName);
            request.setEntity(new StringEntity(mappingJson, ContentType.APPLICATION_JSON));
            restClient.performRequest(request);
            log.info("Created index {}", indexName);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create index", e);
        }
    }

    @Override
    public long loadData() {
        try {
            JsonDataLoader loader = new JsonDataLoader();
            Iterable<Map<String, Object>> records = loader.loadRecords(dataFile);

            long totalIndexed = 0;
            List<Map<String, Object>> batch = new ArrayList<>();

            for (Map<String, Object> record : records) {
                // Add geo_point location
                addLocation(record);
                batch.add(record);

                if (batch.size() >= bulkSize) {
                    totalIndexed += bulkIndex(batch);
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                totalIndexed += bulkIndex(batch);
            }

            client.indices().refresh(r -> r.index(indexName));
            log.info("Indexed {} documents into {}", totalIndexed, indexName);
            return totalIndexed;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load data", e);
        }
    }

    @Override
    public void deleteIndex() {
        try {
            boolean exists = client.indices().exists(e -> e.index(indexName)).value();
            if (exists) {
                client.indices().delete(d -> d.index(indexName));
                log.info("Deleted index {}", indexName);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete index", e);
        }
    }

    @Override
    public boolean indexExists() {
        try {
            return client.indices().exists(e -> e.index(indexName)).value();
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public long documentCount() {
        try {
            return client.count(c -> c.index(indexName)).count();
        } catch (IOException e) {
            return 0;
        }
    }

    private void addLocation(Map<String, Object> record) {
        try {
            String latStr = String.valueOf(record.get("latitude"));
            String lonStr = String.valueOf(record.get("longitude"));
            if (latStr != null && !latStr.isEmpty() && !"null".equals(latStr)
                    && lonStr != null && !lonStr.isEmpty() && !"null".equals(lonStr)) {
                double lat = Double.parseDouble(latStr);
                double lon = Double.parseDouble(lonStr);
                if (lat >= 33.0 && lat <= 43.0 && lon >= 124.0 && lon <= 132.0) {
                    Map<String, Double> location = new HashMap<>();
                    location.put("lat", lat);
                    location.put("lon", lon);
                    record.put("location", location);
                    record.put("latitude", lat);
                    record.put("longitude", lon);
                }
            }
        } catch (NumberFormatException e) {
            // skip location for invalid coordinates
        }
    }

    private long bulkIndex(List<Map<String, Object>> batch) throws IOException {
        BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
        for (Map<String, Object> doc : batch) {
            bulkBuilder.operations(op -> op
                    .index(idx -> idx
                            .index(indexName)
                            .document(doc)
                    )
            );
        }

        BulkResponse response = client.bulk(bulkBuilder.build());
        long errors = 0;
        if (response.errors()) {
            for (BulkResponseItem item : response.items()) {
                if (item.error() != null) {
                    errors++;
                    log.warn("Bulk index error: {}", item.error().reason());
                }
            }
        }
        return batch.size() - errors;
    }

    @Override
    public long bulkUpsert(List<Map<String, Object>> records) {
        try {
            BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
            for (Map<String, Object> record : records) {
                addLocation(record);

                // Use trafficLightId + sidoName + sigunguName as unique document ID
                String docId = generateDocId(record);

                bulkBuilder.operations(op -> op
                        .index(idx -> idx
                                .index(indexName)
                                .id(docId)
                                .document(record)
                        )
                );
            }

            BulkResponse response = client.bulk(bulkBuilder.build());
            long errors = 0;
            if (response.errors()) {
                for (BulkResponseItem item : response.items()) {
                    if (item.error() != null) {
                        errors++;
                        log.warn("Bulk upsert error: {}", item.error().reason());
                    }
                }
            }
            return records.size() - errors;
        } catch (IOException e) {
            log.error("Bulk upsert failed: {}", e.getMessage());
            return 0;
        }
    }

    private String generateDocId(Map<String, Object> record) {
        String id = String.valueOf(record.getOrDefault("trafficLightId", ""));
        String sido = String.valueOf(record.getOrDefault("sidoName", ""));
        String sigungu = String.valueOf(record.getOrDefault("sigunguName", ""));
        return sido + "_" + sigungu + "_" + id;
    }
}
