package com.trafficlight.api.dto;

import com.trafficlight.domain.model.TrafficLight;
import com.trafficlight.usecase.dto.SearchResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SearchResponseTest {

    @Test
    @DisplayName("SearchResult에서 SearchResponse로 변환된다")
    void fromSearchResult() {
        TrafficLight tl = TrafficLight.builder()
                .sidoName("서울특별시")
                .build();

        SearchResult result = SearchResult.builder()
                .total(100)
                .page(2)
                .size(10)
                .results(List.of(tl))
                .build();

        SearchResponse response = SearchResponse.from(result);

        assertThat(response.getTotal()).isEqualTo(100);
        assertThat(response.getPage()).isEqualTo(2);
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().get(0).getSidoName()).isEqualTo("서울특별시");
    }

    @Test
    @DisplayName("빈 결과도 올바르게 변환된다")
    void fromEmptyResult() {
        SearchResult result = SearchResult.builder()
                .total(0)
                .page(0)
                .size(20)
                .results(Collections.emptyList())
                .build();

        SearchResponse response = SearchResponse.from(result);

        assertThat(response.getTotal()).isEqualTo(0);
        assertThat(response.getResults()).isEmpty();
    }
}
