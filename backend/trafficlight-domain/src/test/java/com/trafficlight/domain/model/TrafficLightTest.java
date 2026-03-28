package com.trafficlight.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TrafficLightTest {

    @Nested
    @DisplayName("hasValidLocation")
    class HasValidLocation {

        @Test
        @DisplayName("대한민국 범위 내 좌표는 유효하다")
        void validKoreaCoordinates() {
            TrafficLight tl = TrafficLight.builder()
                    .latitude(37.5665)
                    .longitude(126.9780)
                    .build();

            assertThat(tl.hasValidLocation()).isTrue();
        }

        @Test
        @DisplayName("위도 하한 경계값 33.0은 유효하다")
        void latitudeLowerBoundary() {
            TrafficLight tl = TrafficLight.builder()
                    .latitude(33.0)
                    .longitude(127.0)
                    .build();

            assertThat(tl.hasValidLocation()).isTrue();
        }

        @Test
        @DisplayName("위도 상한 경계값 43.0은 유효하다")
        void latitudeUpperBoundary() {
            TrafficLight tl = TrafficLight.builder()
                    .latitude(43.0)
                    .longitude(127.0)
                    .build();

            assertThat(tl.hasValidLocation()).isTrue();
        }

        @Test
        @DisplayName("경도 하한 경계값 124.0은 유효하다")
        void longitudeLowerBoundary() {
            TrafficLight tl = TrafficLight.builder()
                    .latitude(37.0)
                    .longitude(124.0)
                    .build();

            assertThat(tl.hasValidLocation()).isTrue();
        }

        @Test
        @DisplayName("경도 상한 경계값 132.0은 유효하다")
        void longitudeUpperBoundary() {
            TrafficLight tl = TrafficLight.builder()
                    .latitude(37.0)
                    .longitude(132.0)
                    .build();

            assertThat(tl.hasValidLocation()).isTrue();
        }

        @Test
        @DisplayName("위도가 범위 미만이면 유효하지 않다")
        void latitudeBelowRange() {
            TrafficLight tl = TrafficLight.builder()
                    .latitude(32.9)
                    .longitude(127.0)
                    .build();

            assertThat(tl.hasValidLocation()).isFalse();
        }

        @Test
        @DisplayName("위도가 범위 초과이면 유효하지 않다")
        void latitudeAboveRange() {
            TrafficLight tl = TrafficLight.builder()
                    .latitude(43.1)
                    .longitude(127.0)
                    .build();

            assertThat(tl.hasValidLocation()).isFalse();
        }

        @Test
        @DisplayName("경도가 범위 미만이면 유효하지 않다")
        void longitudeBelowRange() {
            TrafficLight tl = TrafficLight.builder()
                    .latitude(37.0)
                    .longitude(123.9)
                    .build();

            assertThat(tl.hasValidLocation()).isFalse();
        }

        @Test
        @DisplayName("경도가 범위 초과이면 유효하지 않다")
        void longitudeAboveRange() {
            TrafficLight tl = TrafficLight.builder()
                    .latitude(37.0)
                    .longitude(132.1)
                    .build();

            assertThat(tl.hasValidLocation()).isFalse();
        }

        @Test
        @DisplayName("좌표가 0이면 유효하지 않다")
        void zeroCoordinates() {
            TrafficLight tl = TrafficLight.builder()
                    .latitude(0.0)
                    .longitude(0.0)
                    .build();

            assertThat(tl.hasValidLocation()).isFalse();
        }
    }

    @Nested
    @DisplayName("fromMap - 영문 키")
    class FromMapEnglishKeys {

        @Test
        @DisplayName("영문 키로 기본 필드가 매핑된다")
        void mapsBasicFieldsFromEnglishKeys() {
            Map<String, Object> map = new HashMap<>();
            map.put("sidoName", "서울특별시");
            map.put("sigunguName", "강남구");
            map.put("roadType", "일반국도");
            map.put("trafficLightId", "TL-001");
            map.put("trafficLightCategory", "차량신호등");
            map.put("managementAgency", "서울시청");

            TrafficLight tl = TrafficLight.fromMap(map);

            assertThat(tl.getSidoName()).isEqualTo("서울특별시");
            assertThat(tl.getSigunguName()).isEqualTo("강남구");
            assertThat(tl.getRoadType()).isEqualTo("일반국도");
            assertThat(tl.getTrafficLightId()).isEqualTo("TL-001");
            assertThat(tl.getTrafficLightCategory()).isEqualTo("차량신호등");
            assertThat(tl.getManagementAgency()).isEqualTo("서울시청");
        }

        @Test
        @DisplayName("위도/경도 문자열이 double로 파싱된다")
        void parsesLatLonFromString() {
            Map<String, Object> map = new HashMap<>();
            map.put("latitude", "37.5665");
            map.put("longitude", "126.9780");

            TrafficLight tl = TrafficLight.fromMap(map);

            assertThat(tl.getLatitude()).isEqualTo(37.5665);
            assertThat(tl.getLongitude()).isEqualTo(126.9780);
        }

        @Test
        @DisplayName("빈 위도/경도는 0.0으로 유지된다")
        void emptyLatLonRemainsZero() {
            Map<String, Object> map = new HashMap<>();
            map.put("latitude", "");
            map.put("longitude", "");

            TrafficLight tl = TrafficLight.fromMap(map);

            assertThat(tl.getLatitude()).isEqualTo(0.0);
            assertThat(tl.getLongitude()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("숫자가 아닌 위도/경도는 무시된다")
        void invalidLatLonIgnored() {
            Map<String, Object> map = new HashMap<>();
            map.put("latitude", "invalid");
            map.put("longitude", "abc");

            TrafficLight tl = TrafficLight.fromMap(map);

            assertThat(tl.getLatitude()).isEqualTo(0.0);
            assertThat(tl.getLongitude()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("fromMap - 한글 키")
    class FromMapKoreanKeys {

        @Test
        @DisplayName("한글 키로 필드가 매핑된다")
        void mapsFieldsFromKoreanKeys() {
            Map<String, Object> map = new HashMap<>();
            map.put("시도명", "부산광역시");
            map.put("시군구명", "해운대구");
            map.put("도로종류", "시도");
            map.put("신호등관리번호", "TL-002");
            map.put("위도", "35.1631");
            map.put("경도", "129.1637");

            TrafficLight tl = TrafficLight.fromMap(map);

            assertThat(tl.getSidoName()).isEqualTo("부산광역시");
            assertThat(tl.getSigunguName()).isEqualTo("해운대구");
            assertThat(tl.getRoadType()).isEqualTo("시도");
            assertThat(tl.getTrafficLightId()).isEqualTo("TL-002");
            assertThat(tl.getLatitude()).isEqualTo(35.1631);
            assertThat(tl.getLongitude()).isEqualTo(129.1637);
        }

        @Test
        @DisplayName("영문 키가 한글 키보다 우선한다")
        void englishKeyTakesPrecedence() {
            Map<String, Object> map = new HashMap<>();
            map.put("sidoName", "서울특별시");
            map.put("시도명", "부산광역시");

            TrafficLight tl = TrafficLight.fromMap(map);

            assertThat(tl.getSidoName()).isEqualTo("서울특별시");
        }
    }

    @Nested
    @DisplayName("fromMap - location 객체")
    class FromMapLocationObject {

        @Test
        @DisplayName("location 객체에서 좌표가 파싱된다")
        void parsesLocationObject() {
            Map<String, Object> map = new HashMap<>();
            Map<String, Object> location = new HashMap<>();
            location.put("lat", "37.5665");
            location.put("lon", "126.9780");
            map.put("location", location);

            TrafficLight tl = TrafficLight.fromMap(map);

            assertThat(tl.getLatitude()).isEqualTo(37.5665);
            assertThat(tl.getLongitude()).isEqualTo(126.9780);
        }

        @Test
        @DisplayName("location 객체가 개별 위도/경도보다 우선한다")
        void locationObjectOverridesIndividualFields() {
            Map<String, Object> map = new HashMap<>();
            map.put("latitude", "35.0");
            map.put("longitude", "128.0");
            Map<String, Object> location = new HashMap<>();
            location.put("lat", "37.5665");
            location.put("lon", "126.9780");
            map.put("location", location);

            TrafficLight tl = TrafficLight.fromMap(map);

            assertThat(tl.getLatitude()).isEqualTo(37.5665);
            assertThat(tl.getLongitude()).isEqualTo(126.9780);
        }

        @Test
        @DisplayName("location이 Map이 아니면 무시된다")
        void nonMapLocationIgnored() {
            Map<String, Object> map = new HashMap<>();
            map.put("latitude", "37.0");
            map.put("longitude", "127.0");
            map.put("location", "invalid");

            TrafficLight tl = TrafficLight.fromMap(map);

            assertThat(tl.getLatitude()).isEqualTo(37.0);
            assertThat(tl.getLongitude()).isEqualTo(127.0);
        }
    }

    @Nested
    @DisplayName("fromMap - 엣지 케이스")
    class FromMapEdgeCases {

        @Test
        @DisplayName("빈 Map은 모든 필드가 null/0이다")
        void emptyMapReturnsDefaults() {
            Map<String, Object> map = new HashMap<>();

            TrafficLight tl = TrafficLight.fromMap(map);

            assertThat(tl.getSidoName()).isNull();
            assertThat(tl.getSigunguName()).isNull();
            assertThat(tl.getLatitude()).isEqualTo(0.0);
            assertThat(tl.getLongitude()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("null 값은 null로 매핑된다")
        void nullValuesMapToNull() {
            Map<String, Object> map = new HashMap<>();
            map.put("sidoName", null);

            TrafficLight tl = TrafficLight.fromMap(map);

            assertThat(tl.getSidoName()).isNull();
        }

        @Test
        @DisplayName("모든 필드가 올바르게 매핑된다")
        void allFieldsMapped() {
            Map<String, Object> map = new HashMap<>();
            map.put("sidoName", "서울특별시");
            map.put("sigunguName", "강남구");
            map.put("roadType", "일반국도");
            map.put("roadRouteNumber", "1");
            map.put("roadRouteName", "경부선");
            map.put("roadRouteDirection", "상행");
            map.put("roadNameAddress", "테헤란로 123");
            map.put("lotNumberAddress", "역삼동 123-4");
            map.put("signalInstallType", "기둥식");
            map.put("roadShape", "교차로");
            map.put("isMainRoad", "Y");
            map.put("trafficLightId", "TL-001");
            map.put("trafficLightCategory", "차량신호등");
            map.put("lightColorType", "3색");
            map.put("signalMethod", "점등");
            map.put("signalSequence", "녹-황-적");
            map.put("signalDuration", "60초");
            map.put("lightSourceType", "LED");
            map.put("signalControlMethod", "자동");
            map.put("signalTimeDecisionMethod", "정주기");
            map.put("flashingLightEnabled", "Y");
            map.put("flashingStartTime", "23:00");
            map.put("flashingEndTime", "06:00");
            map.put("hasPedestrianSignal", "Y");
            map.put("hasRemainingTimeDisplay", "Y");
            map.put("hasAudioSignal", "N");
            map.put("roadSignSerialNumber", "RS-001");
            map.put("managementAgency", "서울시청");
            map.put("managementPhone", "02-1234-5678");
            map.put("dataReferenceDate", "2024-01-01");
            map.put("providerCode", "3000000");
            map.put("providerName", "서울특별시");
            map.put("latitude", "37.5665");
            map.put("longitude", "126.9780");

            TrafficLight tl = TrafficLight.fromMap(map);

            assertThat(tl.getSidoName()).isEqualTo("서울특별시");
            assertThat(tl.getRoadRouteNumber()).isEqualTo("1");
            assertThat(tl.getRoadRouteName()).isEqualTo("경부선");
            assertThat(tl.getLightSourceType()).isEqualTo("LED");
            assertThat(tl.getFlashingStartTime()).isEqualTo("23:00");
            assertThat(tl.getProviderName()).isEqualTo("서울특별시");
            assertThat(tl.getLatitude()).isEqualTo(37.5665);
        }
    }
}