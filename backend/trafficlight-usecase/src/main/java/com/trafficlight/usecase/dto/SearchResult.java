package com.trafficlight.usecase.dto;

import com.trafficlight.domain.model.TrafficLight;
import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class SearchResult {
    private final long total;
    private final int page;
    private final int size;
    private final List<TrafficLight> results;
}
