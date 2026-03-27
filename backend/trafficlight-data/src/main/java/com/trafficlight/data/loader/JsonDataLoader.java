package com.trafficlight.data.loader;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

public class JsonDataLoader {

    private static final Map<String, String> FIELD_NAME_MAP;

    static {
        Map<String, String> m = new HashMap<>();
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

    public Iterable<Map<String, Object>> loadRecords(String filePath) throws IOException {
        return () -> {
            try {
                return new RecordIterator(filePath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to open file: " + filePath, e);
            }
        };
    }

    private static Map<String, Object> translateFieldNames(Map<String, Object> record) {
        Map<String, Object> translated = new HashMap<>();
        for (Map.Entry<String, Object> entry : record.entrySet()) {
            String engKey = FIELD_NAME_MAP.getOrDefault(entry.getKey(), entry.getKey());
            translated.put(engKey, entry.getValue());
        }
        return translated;
    }

    private static class RecordIterator implements Iterator<Map<String, Object>> {
        private final JsonParser parser;
        private final ObjectMapper objectMapper = new ObjectMapper();
        private Map<String, Object> nextRecord;
        private boolean done = false;

        RecordIterator(String filePath) throws IOException {
            InputStream is = new FileInputStream(filePath);
            JsonFactory factory = new JsonFactory();
            this.parser = factory.createParser(is);
            navigateToRecordsArray();
            advance();
        }

        private void navigateToRecordsArray() throws IOException {
            JsonToken token = parser.nextToken();
            if (token == JsonToken.START_ARRAY) {
                return; // top-level array
            }
            // Navigate to "records" or "data" field
            while (parser.nextToken() != null) {
                if (parser.currentToken() == JsonToken.FIELD_NAME) {
                    String fieldName = parser.getCurrentName();
                    if ("records".equals(fieldName) || "data".equals(fieldName)) {
                        parser.nextToken(); // START_ARRAY
                        return;
                    }
                }
                // Skip non-target arrays (e.g., "fields" array)
                if (parser.currentToken() == JsonToken.START_ARRAY) {
                    parser.skipChildren();
                }
            }
        }

        private void advance() {
            try {
                if (done) return;
                JsonToken token = parser.nextToken();
                if (token == JsonToken.START_OBJECT) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> record = objectMapper.readValue(parser, Map.class);
                    nextRecord = translateFieldNames(record);
                } else {
                    done = true;
                    nextRecord = null;
                    parser.close();
                }
            } catch (IOException e) {
                done = true;
                nextRecord = null;
            }
        }

        @Override
        public boolean hasNext() {
            return !done && nextRecord != null;
        }

        @Override
        public Map<String, Object> next() {
            if (!hasNext()) throw new NoSuchElementException();
            Map<String, Object> current = nextRecord;
            advance();
            return current;
        }
    }
}
