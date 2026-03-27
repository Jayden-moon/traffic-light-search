package com.trafficlight.domain.service;

import com.trafficlight.domain.model.TrafficLight;
import java.util.List;
import java.util.Map;

public interface SearchService {
    List<TrafficLight> search(String query, Map<String, String> filters, int page, int size);
    long count(String query, Map<String, String> filters);
    List<TrafficLight> geoSearch(double lat, double lon, String distance, int page, int size);
    long geoCount(double lat, double lon, String distance);
    List<String> getFilterOptions(String field, Map<String, String> parentFilter);
}
