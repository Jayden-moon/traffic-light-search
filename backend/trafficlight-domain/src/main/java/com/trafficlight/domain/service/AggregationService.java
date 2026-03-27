package com.trafficlight.domain.service;

import java.util.List;
import java.util.Map;

public interface AggregationService {
    Map<String, Object> aggregateByRegion();
    Map<String, Object> aggregateByRoadType();
    Map<String, Object> aggregateBySignalType();
    Map<String, Object> getSummary();
}
