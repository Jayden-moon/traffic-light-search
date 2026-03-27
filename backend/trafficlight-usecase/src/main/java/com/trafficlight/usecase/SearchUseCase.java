package com.trafficlight.usecase;

import com.trafficlight.domain.service.SearchService;
import com.trafficlight.domain.model.TrafficLight;
import com.trafficlight.usecase.dto.SearchCommand;
import com.trafficlight.usecase.dto.GeoSearchCommand;
import com.trafficlight.usecase.dto.SearchResult;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class SearchUseCase {

    private final SearchService searchService;

    public SearchResult search(SearchCommand command) {
        Map<String, String> filters = new HashMap<>();
        if (command.getSidoName() != null && !command.getSidoName().isEmpty()) {
            filters.put("sidoName", command.getSidoName());
        }
        if (command.getSigunguName() != null && !command.getSigunguName().isEmpty()) {
            filters.put("sigunguName", command.getSigunguName());
        }
        if (command.getRoadType() != null && !command.getRoadType().isEmpty()) {
            filters.put("roadType", command.getRoadType());
        }
        if (command.getTrafficLightCategory() != null && !command.getTrafficLightCategory().isEmpty()) {
            filters.put("trafficLightCategory", command.getTrafficLightCategory());
        }

        List<TrafficLight> results = searchService.search(
                command.getQ(), filters, command.getPage(), command.getSize());
        long total = searchService.count(command.getQ(), filters);

        return SearchResult.builder()
                .total(total)
                .page(command.getPage())
                .size(command.getSize())
                .results(results)
                .build();
    }

    public SearchResult geoSearch(GeoSearchCommand command) {
        List<TrafficLight> results = searchService.geoSearch(
                command.getLat(), command.getLon(), command.getDistance(),
                command.getPage(), command.getSize());
        long total = searchService.geoCount(
                command.getLat(), command.getLon(), command.getDistance());

        return SearchResult.builder()
                .total(total)
                .page(command.getPage())
                .size(command.getSize())
                .results(results)
                .build();
    }

    public List<String> getFilterOptions(String field, Map<String, String> parentFilter) {
        return searchService.getFilterOptions(field, parentFilter);
    }
}
