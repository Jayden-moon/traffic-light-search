package com.trafficlight.domain.service;

public interface IndexService {
    void createIndex();
    long loadData();
    void deleteIndex();
    boolean indexExists();
    long documentCount();

    /**
     * Bulk upsert records using trafficLightId as document ID.
     * Same ID = update, new ID = insert. No duplicates.
     * @param records list of records with English field names
     * @return number of successfully indexed documents
     */
    long bulkUpsert(java.util.List<java.util.Map<String, Object>> records);
}
