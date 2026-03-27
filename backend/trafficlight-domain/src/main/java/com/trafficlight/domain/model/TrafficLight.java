package com.trafficlight.domain.model;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrafficLight {
    private String sidoName;
    private String sigunguName;
    private String roadType;
    private String roadRouteNumber;
    private String roadRouteName;
    private String roadRouteDirection;
    private String roadNameAddress;
    private String lotNumberAddress;
    private double latitude;
    private double longitude;
    private String signalInstallType;
    private String roadShape;
    private String isMainRoad;
    private String trafficLightId;
    private String trafficLightCategory;
    private String lightColorType;
    private String signalMethod;
    private String signalSequence;
    private String signalDuration;
    private String lightSourceType;
    private String signalControlMethod;
    private String signalTimeDecisionMethod;
    private String flashingLightEnabled;
    private String flashingStartTime;
    private String flashingEndTime;
    private String hasPedestrianSignal;
    private String hasRemainingTimeDisplay;
    private String hasAudioSignal;
    private String roadSignSerialNumber;
    private String managementAgency;
    private String managementPhone;
    private String dataReferenceDate;
    private String providerCode;
    private String providerName;

    public boolean hasValidLocation() {
        return latitude >= 33.0 && latitude <= 43.0
                && longitude >= 124.0 && longitude <= 132.0;
    }

    public static TrafficLight fromMap(Map<String, Object> map) {
        TrafficLight tl = new TrafficLight();
        tl.sidoName = getStr(map, "sidoName", "시도명");
        tl.sigunguName = getStr(map, "sigunguName", "시군구명");
        tl.roadType = getStr(map, "roadType", "도로종류");
        tl.roadRouteNumber = getStr(map, "roadRouteNumber", "도로노선번호");
        tl.roadRouteName = getStr(map, "roadRouteName", "도로노선명");
        tl.roadRouteDirection = getStr(map, "roadRouteDirection", "도로노선방향");
        tl.roadNameAddress = getStr(map, "roadNameAddress", "소재지도로명주소");
        tl.lotNumberAddress = getStr(map, "lotNumberAddress", "소재지지번주소");
        tl.signalInstallType = getStr(map, "signalInstallType", "신호기설치방식");
        tl.roadShape = getStr(map, "roadShape", "도로형태");
        tl.isMainRoad = getStr(map, "isMainRoad", "주도로여부");
        tl.trafficLightId = getStr(map, "trafficLightId", "신호등관리번호");
        tl.trafficLightCategory = getStr(map, "trafficLightCategory", "신호등구분");
        tl.lightColorType = getStr(map, "lightColorType", "신호등색종류");
        tl.signalMethod = getStr(map, "signalMethod", "신호등화방식");
        tl.signalSequence = getStr(map, "signalSequence", "신호등화순서");
        tl.signalDuration = getStr(map, "signalDuration", "신호등화시간");
        tl.lightSourceType = getStr(map, "lightSourceType", "광원종류");
        tl.signalControlMethod = getStr(map, "signalControlMethod", "신호제어방식");
        tl.signalTimeDecisionMethod = getStr(map, "signalTimeDecisionMethod", "신호시간결정방식");
        tl.flashingLightEnabled = getStr(map, "flashingLightEnabled", "점멸등운영여부");
        tl.flashingStartTime = getStr(map, "flashingStartTime", "점멸등운영시작시각");
        tl.flashingEndTime = getStr(map, "flashingEndTime", "점멸등운영종료시각");
        tl.hasPedestrianSignal = getStr(map, "hasPedestrianSignal", "보행자작동신호기유무");
        tl.hasRemainingTimeDisplay = getStr(map, "hasRemainingTimeDisplay", "잔여시간표시기유무");
        tl.hasAudioSignal = getStr(map, "hasAudioSignal", "시각장애인용음향신호기유무");
        tl.roadSignSerialNumber = getStr(map, "roadSignSerialNumber", "도로안내표지일련번호");
        tl.managementAgency = getStr(map, "managementAgency", "관리기관명");
        tl.managementPhone = getStr(map, "managementPhone", "관리기관전화번호");
        tl.dataReferenceDate = getStr(map, "dataReferenceDate", "데이터기준일자");
        tl.providerCode = getStr(map, "providerCode", "제공기관코드");
        tl.providerName = getStr(map, "providerName", "제공기관명");

        // Parse latitude/longitude
        String latStr = getStr(map, "latitude", "위도");
        String lonStr = getStr(map, "longitude", "경도");
        try {
            if (latStr != null && !latStr.isEmpty()) tl.latitude = Double.parseDouble(latStr);
            if (lonStr != null && !lonStr.isEmpty()) tl.longitude = Double.parseDouble(lonStr);
        } catch (NumberFormatException e) {
            // ignore
        }

        // Handle pre-parsed location object
        if (map.containsKey("location") && map.get("location") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> loc = (Map<String, Object>) map.get("location");
            try {
                tl.latitude = Double.parseDouble(String.valueOf(loc.get("lat")));
                tl.longitude = Double.parseDouble(String.valueOf(loc.get("lon")));
            } catch (Exception e) {
                // ignore
            }
        }

        return tl;
    }

    private static String getStr(Map<String, Object> map, String engKey, String korKey) {
        Object val = map.get(engKey);
        if (val == null) val = map.get(korKey);
        return val != null ? String.valueOf(val) : null;
    }
}
