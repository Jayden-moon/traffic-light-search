package com.trafficlight.domain.service;

import java.util.List;
import java.util.Map;

public interface DataCollectorService {

    /**
     * Fetch traffic light data from external API.
     * @param pageNo page number (1-based)
     * @param numOfRows number of rows per page
     * @return list of records with Korean field names
     */
    List<Map<String, Object>> fetchPage(int pageNo, int numOfRows);

    /**
     * @return total record count from the API
     */
    long getTotalCount();
}
