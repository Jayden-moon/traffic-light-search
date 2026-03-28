package com.trafficlight.api.dto;

import com.trafficlight.usecase.dto.AggregationResult;
import com.trafficlight.usecase.dto.AggregationResult.Bucket;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AggregationResponseTest {

    @Test
    @DisplayName("AggregationResult에서 AggregationResponse로 변환된다")
    void fromAggregationResult() {
        AggregationResult result = AggregationResult.builder()
                .buckets(List.of(
                        Bucket.builder().key("서울특별시").count(5000).build(),
                        Bucket.builder().key("부산광역시").count(3000).build()
                ))
                .build();

        AggregationResponse response = AggregationResponse.from(result);

        assertThat(response.getBuckets()).hasSize(2);
        assertThat(response.getBuckets().get(0).getKey()).isEqualTo("서울특별시");
        assertThat(response.getBuckets().get(0).getCount()).isEqualTo(5000);
    }

    @Test
    @DisplayName("하위 버킷이 재귀적으로 변환된다")
    void convertsSubBucketsRecursively() {
        AggregationResult result = AggregationResult.builder()
                .buckets(List.of(
                        Bucket.builder()
                                .key("서울특별시")
                                .count(5000)
                                .subBuckets(List.of(
                                        Bucket.builder().key("강남구").count(2000).build(),
                                        Bucket.builder().key("서초구").count(1500).build()
                                ))
                                .build()
                ))
                .build();

        AggregationResponse response = AggregationResponse.from(result);

        List<AggregationResponse.BucketResponse> subBuckets = response.getBuckets().get(0).getSubBuckets();
        assertThat(subBuckets).hasSize(2);
        assertThat(subBuckets.get(0).getKey()).isEqualTo("강남구");
        assertThat(subBuckets.get(1).getKey()).isEqualTo("서초구");
    }

    @Test
    @DisplayName("하위 버킷이 null이면 null로 유지된다")
    void nullSubBucketsRemainNull() {
        AggregationResult result = AggregationResult.builder()
                .buckets(List.of(
                        Bucket.builder().key("일반국도").count(10000).subBuckets(null).build()
                ))
                .build();

        AggregationResponse response = AggregationResponse.from(result);

        assertThat(response.getBuckets().get(0).getSubBuckets()).isNull();
    }
}
