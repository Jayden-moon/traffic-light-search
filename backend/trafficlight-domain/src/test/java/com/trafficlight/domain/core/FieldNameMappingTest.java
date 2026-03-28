package com.trafficlight.domain.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FieldNameMappingTest {

    @Test
    @DisplayName("한글 키를 영문 키로 변환한다")
    void translatesKoreanToEnglish() {
        assertThat(FieldNameMapping.toEnglish("시도명")).isEqualTo("sidoName");
        assertThat(FieldNameMapping.toEnglish("시군구명")).isEqualTo("sigunguName");
        assertThat(FieldNameMapping.toEnglish("위도")).isEqualTo("latitude");
        assertThat(FieldNameMapping.toEnglish("경도")).isEqualTo("longitude");
        assertThat(FieldNameMapping.toEnglish("신호등관리번호")).isEqualTo("trafficLightId");
    }

    @Test
    @DisplayName("매핑에 없는 키는 그대로 반환한다")
    void unmappedKeyReturnedAsIs() {
        assertThat(FieldNameMapping.toEnglish("unknownField")).isEqualTo("unknownField");
        assertThat(FieldNameMapping.toEnglish("sidoName")).isEqualTo("sidoName");
    }

    @Test
    @DisplayName("전체 매핑 개수는 34개이다")
    void mappingContains34Entries() {
        assertThat(FieldNameMapping.getMapping()).hasSize(34);
    }

    @Test
    @DisplayName("매핑은 불변이다")
    void mappingIsUnmodifiable() {
        Map<String, String> mapping = FieldNameMapping.getMapping();
        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> mapping.put("test", "test")
        );
    }

    @Test
    @DisplayName("레코드 전체를 일괄 변환한다")
    void translatesEntireRecord() {
        Map<String, Object> record = new HashMap<>();
        record.put("시도명", "서울특별시");
        record.put("시군구명", "강남구");
        record.put("위도", "37.5665");
        record.put("unknownKey", "value");

        Map<String, Object> translated = FieldNameMapping.translateRecord(record);

        assertThat(translated.get("sidoName")).isEqualTo("서울특별시");
        assertThat(translated.get("sigunguName")).isEqualTo("강남구");
        assertThat(translated.get("latitude")).isEqualTo("37.5665");
        assertThat(translated.get("unknownKey")).isEqualTo("value");
        assertThat(translated).doesNotContainKey("시도명");
    }

    @Test
    @DisplayName("빈 레코드는 빈 결과를 반환한다")
    void emptyRecordReturnsEmpty() {
        Map<String, Object> result = FieldNameMapping.translateRecord(new HashMap<>());
        assertThat(result).isEmpty();
    }
}
