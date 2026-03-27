package com.trafficlight.api.controller;

import com.trafficlight.usecase.SearchUseCase;
import com.trafficlight.usecase.dto.SearchCommand;
import com.trafficlight.usecase.dto.GeoSearchCommand;
import com.trafficlight.usecase.dto.SearchResult;
import com.trafficlight.api.dto.SearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchUseCase searchUseCase;

    @GetMapping
    public ResponseEntity<SearchResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String sidoName,
            @RequestParam(required = false) String sigunguName,
            @RequestParam(required = false) String roadType,
            @RequestParam(required = false) String trafficLightCategory,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        SearchCommand command = SearchCommand.builder()
                .q(q)
                .sidoName(sidoName)
                .sigunguName(sigunguName)
                .roadType(roadType)
                .trafficLightCategory(trafficLightCategory)
                .page(page)
                .size(size)
                .build();

        SearchResult result = searchUseCase.search(command);
        return ResponseEntity.ok(SearchResponse.from(result));
    }

    @GetMapping("/geo")
    public ResponseEntity<SearchResponse> geoSearch(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "1km") String distance,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        GeoSearchCommand command = GeoSearchCommand.builder()
                .lat(lat)
                .lon(lon)
                .distance(distance)
                .page(page)
                .size(size)
                .build();

        SearchResult result = searchUseCase.geoSearch(command);
        return ResponseEntity.ok(SearchResponse.from(result));
    }

    @GetMapping("/filters/{field}")
    public ResponseEntity<List<String>> getFilterOptions(
            @PathVariable String field,
            @RequestParam(required = false) String sidoName) {

        Map<String, String> parentFilter = new HashMap<>();
        if (sidoName != null && !sidoName.isEmpty()) {
            parentFilter.put("sidoName", sidoName);
        }

        List<String> options = searchUseCase.getFilterOptions(field, parentFilter);
        return ResponseEntity.ok(options);
    }
}
