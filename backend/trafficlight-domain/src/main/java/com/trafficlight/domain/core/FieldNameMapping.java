package com.trafficlight.domain.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 공공데이터 한글 필드명 → 영문 필드명 매핑.
 * JsonDataLoader와 PublicDataApiAdapter에서 공통으로 사용한다.
 */
public final class FieldNameMapping {

    private FieldNameMapping() {}

    private static final Map<String, String> KOREAN_TO_ENGLISH;

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
        KOREAN_TO_ENGLISH = Collections.unmodifiableMap(m);
    }

    public static Map<String, String> getMapping() {
        return KOREAN_TO_ENGLISH;
    }

    /**
     * 한글 키를 영문 키로 변환. 매핑이 없으면 원본 키를 그대로 반환한다.
     */
    public static String toEnglish(String koreanKey) {
        return KOREAN_TO_ENGLISH.getOrDefault(koreanKey, koreanKey);
    }

    /**
     * Map의 한글 키를 영문 키로 일괄 변환한다.
     */
    public static Map<String, Object> translateRecord(Map<String, Object> record) {
        Map<String, Object> translated = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : record.entrySet()) {
            translated.put(toEnglish(entry.getKey()), entry.getValue());
        }
        return translated;
    }
}
