package com.trafficlight.api.dto;

import com.trafficlight.usecase.dto.IndexStatusResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IndexStatusResponseTest {

    @Test
    @DisplayName("IndexStatusResult에서 IndexStatusResponse로 변환된다")
    void fromIndexStatusResult() {
        IndexStatusResult result = IndexStatusResult.builder()
                .totalRecords(10000)
                .indexed(9950)
                .failed(50)
                .durationMs(5000)
                .indexExists(true)
                .build();

        IndexStatusResponse response = IndexStatusResponse.from(result);

        assertThat(response.getTotalRecords()).isEqualTo(10000);
        assertThat(response.getIndexed()).isEqualTo(9950);
        assertThat(response.getFailed()).isEqualTo(50);
        assertThat(response.getDurationMs()).isEqualTo(5000);
        assertThat(response.isIndexExists()).isTrue();
    }

    @Test
    @DisplayName("인덱스가 없는 상태도 올바르게 변환된다")
    void fromResultWithNoIndex() {
        IndexStatusResult result = IndexStatusResult.builder()
                .totalRecords(0)
                .indexed(0)
                .failed(0)
                .durationMs(0)
                .indexExists(false)
                .build();

        IndexStatusResponse response = IndexStatusResponse.from(result);

        assertThat(response.isIndexExists()).isFalse();
        assertThat(response.getTotalRecords()).isEqualTo(0);
    }
}
