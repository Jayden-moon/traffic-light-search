package com.trafficlight.usecase;

import com.trafficlight.domain.service.AggregationService;
import com.trafficlight.usecase.dto.AggregationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AggregationUseCaseTest {

    @Mock
    private AggregationService aggregationService;

    private AggregationUseCase aggregationUseCase;

    @BeforeEach
    void setUp() {
        aggregationUseCase = new AggregationUseCase(aggregationService);
    }

    private Map<String, Object> createBucketMap(String key, long count) {
        Map<String, Object> bucket = new HashMap<>();
        bucket.put("key", key);
        bucket.put("count", count);
        return bucket;
    }

    private Map<String, Object> createBucketMapWithSub(String key, long count, List<Map<String, Object>> subBuckets) {
        Map<String, Object> bucket = new HashMap<>();
        bucket.put("key", key);
        bucket.put("count", count);
        bucket.put("subBuckets", subBuckets);
        return bucket;
    }

    @Nested
    @DisplayName("aggregateByRegion")
    class AggregateByRegion {

        @Test
        @DisplayName("지역별 집계 결과를 변환한다")
        void convertsRegionAggregation() {
            Map<String, Object> raw = new HashMap<>();
            raw.put("buckets", List.of(
                    createBucketMap("서울특별시", 5000),
                    createBucketMap("부산광역시", 3000)
            ));

            when(aggregationService.aggregateByRegion()).thenReturn(raw);

            AggregationResult result = aggregationUseCase.aggregateByRegion();

            assertThat(result.getBuckets()).hasSize(2);
            assertThat(result.getBuckets().get(0).getKey()).isEqualTo("서울특별시");
            assertThat(result.getBuckets().get(0).getCount()).isEqualTo(5000);
            assertThat(result.getBuckets().get(1).getKey()).isEqualTo("부산광역시");
            assertThat(result.getBuckets().get(1).getCount()).isEqualTo(3000);
        }

        @Test
        @DisplayName("하위 버킷이 올바르게 변환된다")
        void convertsSubBuckets() {
            Map<String, Object> raw = new HashMap<>();
            raw.put("buckets", List.of(
                    createBucketMapWithSub("서울특별시", 5000, List.of(
                            createBucketMap("강남구", 2000),
                            createBucketMap("서초구", 1500)
                    ))
            ));

            when(aggregationService.aggregateByRegion()).thenReturn(raw);

            AggregationResult result = aggregationUseCase.aggregateByRegion();

            assertThat(result.getBuckets()).hasSize(1);
            List<AggregationResult.Bucket> subBuckets = result.getBuckets().get(0).getSubBuckets();
            assertThat(subBuckets).hasSize(2);
            assertThat(subBuckets.get(0).getKey()).isEqualTo("강남구");
            assertThat(subBuckets.get(0).getCount()).isEqualTo(2000);
        }

        @Test
        @DisplayName("빈 buckets 리스트를 처리한다")
        void handlesEmptyBuckets() {
            Map<String, Object> raw = new HashMap<>();
            raw.put("buckets", List.of());

            when(aggregationService.aggregateByRegion()).thenReturn(raw);

            AggregationResult result = aggregationUseCase.aggregateByRegion();

            assertThat(result.getBuckets()).isEmpty();
        }

        @Test
        @DisplayName("buckets 키가 없으면 빈 리스트를 반환한다")
        void handlesNoBucketsKey() {
            Map<String, Object> raw = new HashMap<>();

            when(aggregationService.aggregateByRegion()).thenReturn(raw);

            AggregationResult result = aggregationUseCase.aggregateByRegion();

            assertThat(result.getBuckets()).isEmpty();
        }
    }

    @Nested
    @DisplayName("aggregateByRoadType")
    class AggregateByRoadType {

        @Test
        @DisplayName("도로종류별 집계 결과를 변환한다")
        void convertsRoadTypeAggregation() {
            Map<String, Object> raw = new HashMap<>();
            raw.put("buckets", List.of(
                    createBucketMap("일반국도", 10000),
                    createBucketMap("시도", 8000),
                    createBucketMap("군도", 3000)
            ));

            when(aggregationService.aggregateByRoadType()).thenReturn(raw);

            AggregationResult result = aggregationUseCase.aggregateByRoadType();

            assertThat(result.getBuckets()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("aggregateBySignalType")
    class AggregateBySignalType {

        @Test
        @DisplayName("신호등구분별 집계 결과를 변환한다")
        void convertsSignalTypeAggregation() {
            Map<String, Object> raw = new HashMap<>();
            raw.put("buckets", List.of(
                    createBucketMap("차량신호등", 15000),
                    createBucketMap("보행자신호등", 12000)
            ));

            when(aggregationService.aggregateBySignalType()).thenReturn(raw);

            AggregationResult result = aggregationUseCase.aggregateBySignalType();

            assertThat(result.getBuckets()).hasSize(2);
            assertThat(result.getBuckets().get(0).getKey()).isEqualTo("차량신호등");
        }
    }

    @Nested
    @DisplayName("getSummary")
    class GetSummary {

        @Test
        @DisplayName("여러 집계를 요약하여 반환한다")
        void returnsSummaryOfMultipleAggregations() {
            Map<String, Object> regionAgg = new HashMap<>();
            regionAgg.put("buckets", List.of(createBucketMap("서울특별시", 5000)));

            Map<String, Object> roadTypeAgg = new HashMap<>();
            roadTypeAgg.put("buckets", List.of(createBucketMap("일반국도", 10000)));

            Map<String, Object> raw = new HashMap<>();
            raw.put("sidoName", regionAgg);
            raw.put("roadType", roadTypeAgg);

            when(aggregationService.getSummary()).thenReturn(raw);

            Map<String, AggregationResult> summary = aggregationUseCase.getSummary();

            assertThat(summary).hasSize(2);
            assertThat(summary).containsKey("sidoName");
            assertThat(summary).containsKey("roadType");
            assertThat(summary.get("sidoName").getBuckets()).hasSize(1);
            assertThat(summary.get("roadType").getBuckets()).hasSize(1);
        }

        @Test
        @DisplayName("Map이 아닌 값은 무시된다")
        void ignoresNonMapValues() {
            Map<String, Object> raw = new HashMap<>();
            raw.put("sidoName", "not a map");
            raw.put("count", 42L);

            when(aggregationService.getSummary()).thenReturn(raw);

            Map<String, AggregationResult> summary = aggregationUseCase.getSummary();

            assertThat(summary).isEmpty();
        }

        @Test
        @DisplayName("count가 Number가 아닌 경우 0으로 처리된다")
        void nonNumberCountTreatedAsZero() {
            Map<String, Object> bucket = new HashMap<>();
            bucket.put("key", "테스트");
            bucket.put("count", "not_a_number");

            Map<String, Object> agg = new HashMap<>();
            agg.put("buckets", List.of(bucket));

            Map<String, Object> raw = new HashMap<>();
            raw.put("test", agg);

            when(aggregationService.getSummary()).thenReturn(raw);

            Map<String, AggregationResult> summary = aggregationUseCase.getSummary();

            assertThat(summary.get("test").getBuckets().get(0).getCount()).isEqualTo(0);
        }
    }
}
