package com.trafficlight.api.controller;

import com.trafficlight.usecase.AggregationUseCase;
import com.trafficlight.usecase.dto.AggregationResult;
import com.trafficlight.usecase.dto.AggregationResult.Bucket;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AggregationController.class)
@AutoConfigureMockMvc(addFilters = false)
class AggregationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AggregationUseCase aggregationUseCase;

    private AggregationResult createSampleResult() {
        return AggregationResult.builder()
                .buckets(List.of(
                        Bucket.builder().key("서울특별시").count(5000).build(),
                        Bucket.builder().key("부산광역시").count(3000).build()
                ))
                .build();
    }

    @Nested
    @DisplayName("GET /api/aggregations/by-region")
    class ByRegion {

        @Test
        @DisplayName("지역별 집계 결과를 반환한다")
        void returnsRegionAggregation() throws Exception {
            AggregationResult result = AggregationResult.builder()
                    .buckets(List.of(
                            Bucket.builder()
                                    .key("서울특별시")
                                    .count(5000)
                                    .subBuckets(List.of(
                                            Bucket.builder().key("강남구").count(2000).build()
                                    ))
                                    .build()
                    ))
                    .build();

            when(aggregationUseCase.aggregateByRegion()).thenReturn(result);

            mockMvc.perform(get("/api/aggregations/by-region"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.buckets[0].key").value("서울특별시"))
                    .andExpect(jsonPath("$.buckets[0].count").value(5000))
                    .andExpect(jsonPath("$.buckets[0].subBuckets[0].key").value("강남구"))
                    .andExpect(jsonPath("$.buckets[0].subBuckets[0].count").value(2000));
        }
    }

    @Nested
    @DisplayName("GET /api/aggregations/by-road-type")
    class ByRoadType {

        @Test
        @DisplayName("도로종류별 집계 결과를 반환한다")
        void returnsRoadTypeAggregation() throws Exception {
            when(aggregationUseCase.aggregateByRoadType()).thenReturn(createSampleResult());

            mockMvc.perform(get("/api/aggregations/by-road-type"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.buckets").isArray())
                    .andExpect(jsonPath("$.buckets.length()").value(2));
        }
    }

    @Nested
    @DisplayName("GET /api/aggregations/by-signal-type")
    class BySignalType {

        @Test
        @DisplayName("신호등구분별 집계 결과를 반환한다")
        void returnsSignalTypeAggregation() throws Exception {
            when(aggregationUseCase.aggregateBySignalType()).thenReturn(createSampleResult());

            mockMvc.perform(get("/api/aggregations/by-signal-type"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.buckets[0].key").value("서울특별시"));
        }
    }

    @Nested
    @DisplayName("GET /api/aggregations/summary")
    class Summary {

        @Test
        @DisplayName("요약 집계를 반환한다")
        void returnsSummary() throws Exception {
            AggregationResult regionResult = AggregationResult.builder()
                    .buckets(List.of(Bucket.builder().key("서울특별시").count(5000).build()))
                    .build();
            AggregationResult roadTypeResult = AggregationResult.builder()
                    .buckets(List.of(Bucket.builder().key("일반국도").count(10000).build()))
                    .build();

            when(aggregationUseCase.getSummary())
                    .thenReturn(Map.of(
                            "sidoName", regionResult,
                            "roadType", roadTypeResult
                    ));

            mockMvc.perform(get("/api/aggregations/summary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sidoName.buckets[0].key").value("서울특별시"))
                    .andExpect(jsonPath("$.roadType.buckets[0].key").value("일반국도"));
        }
    }
}
