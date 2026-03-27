package com.trafficlight.data.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trafficlight.domain.service.DataCollectorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Slf4j
@Component
public class PublicDataApiAdapter implements DataCollectorService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${trafficlight.api.base-url:http://api.data.go.kr/openapi/tn_pubr_public_traffic_light_api}")
    private String baseUrl;

    @Value("${trafficlight.api.service-key:}")
    private String serviceKey;

    private static final Map<String, String> FIELD_NAME_MAP;

    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("시도명", "sidoName");
        m.put("시군구명", "sigunguName");
        m.put("도로종류", "roadType");
        m.put("도로노선번호", "roadRouteNumber");
        m.put("도로노선명", "roadRouteName");
        m.put("도로노선방향", "roadRouteDirection");
        m.put("소재지도로명주소", "roadNameAddress");
        m.put("소재지지번주소", "lotNumberAddress");
        m.put("위도", "latitude");
        m.put("경도", "longitude");
        m.put("신호기설치방식", "signalInstallType");
        m.put("도로형태", "roadShape");
        m.put("주도로여부", "isMainRoad");
        m.put("신호등관리번호", "trafficLightId");
        m.put("신호등구분", "trafficLightCategory");
        m.put("신호등색종류", "lightColorType");
        m.put("신호등화방식", "signalMethod");
        m.put("신호등화순서", "signalSequence");
        m.put("신호등화시간", "signalDuration");
        m.put("광원종류", "lightSourceType");
        m.put("신호제어방식", "signalControlMethod");
        m.put("신호시간결정방식", "signalTimeDecisionMethod");
        m.put("점멸등운영여부", "flashingLightEnabled");
        m.put("점멸등운영시작시각", "flashingStartTime");
        m.put("점멸등운영종료시각", "flashingEndTime");
        m.put("보행자작동신호기유무", "hasPedestrianSignal");
        m.put("잔여시간표시기유무", "hasRemainingTimeDisplay");
        m.put("시각장애인용음향신호기유무", "hasAudioSignal");
        m.put("도로안내표지일련번호", "roadSignSerialNumber");
        m.put("관리기관명", "managementAgency");
        m.put("관리기관전화번호", "managementPhone");
        m.put("데이터기준일자", "dataReferenceDate");
        m.put("제공기관코드", "providerCode");
        m.put("제공기관명", "providerName");
        FIELD_NAME_MAP = Collections.unmodifiableMap(m);
    }

    public PublicDataApiAdapter() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<Map<String, Object>> fetchPage(int pageNo, int numOfRows) {
        try {
            String url = buildUrl(pageNo, numOfRows);
            log.debug("Fetching page {} (rows={})", pageNo, numOfRows);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("API returned status {}: {}", response.statusCode(), response.body());
                return Collections.emptyList();
            }

            return parseResponse(response.body());
        } catch (IOException | InterruptedException e) {
            log.error("Failed to fetch page {}: {}", pageNo, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public long getTotalCount() {
        try {
            String url = buildUrl(1, 1);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode body = root.path("response").path("body");
            return body.path("totalCount").asLong(0);
        } catch (Exception e) {
            log.error("Failed to get total count: {}", e.getMessage());
            return 0;
        }
    }

    private String buildUrl(int pageNo, int numOfRows) {
        // 공공데이터포털 키는 인코딩 없이 그대로 사용 (이중 인코딩 방지)
        return baseUrl
                + "?serviceKey=" + serviceKey
                + "&pageNo=" + pageNo
                + "&numOfRows=" + numOfRows
                + "&type=json";
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseResponse(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode header = root.path("response").path("header");

            String resultCode = header.path("resultCode").asText();
            if (!"00".equals(resultCode)) {
                log.error("API error: {} - {}", resultCode, header.path("resultMsg").asText());
                return Collections.emptyList();
            }

            JsonNode items = root.path("response").path("body").path("items");
            if (items.isMissingNode() || !items.isArray()) {
                return Collections.emptyList();
            }

            List<Map<String, Object>> results = new ArrayList<>();
            for (JsonNode item : items) {
                Map<String, Object> raw = objectMapper.convertValue(item, Map.class);
                results.add(translateFieldNames(raw));
            }
            return results;
        } catch (Exception e) {
            log.error("Failed to parse response: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private Map<String, Object> translateFieldNames(Map<String, Object> record) {
        Map<String, Object> translated = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : record.entrySet()) {
            String engKey = FIELD_NAME_MAP.getOrDefault(entry.getKey(), entry.getKey());
            translated.put(engKey, entry.getValue());
        }
        return translated;
    }
}
