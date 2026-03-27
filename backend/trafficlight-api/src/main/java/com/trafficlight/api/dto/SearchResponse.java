package com.trafficlight.api.dto;

import com.trafficlight.domain.model.TrafficLight;
import com.trafficlight.usecase.dto.SearchResult;
import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class SearchResponse {
    private final long total;
    private final int page;
    private final int size;
    private final List<TrafficLight> results;

    public static SearchResponse from(SearchResult result) {
        return SearchResponse.builder()
                .total(result.getTotal())
                .page(result.getPage())
                .size(result.getSize())
                .results(result.getResults())
                .build();
    }
}
