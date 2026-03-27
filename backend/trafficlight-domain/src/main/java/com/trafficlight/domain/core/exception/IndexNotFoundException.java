package com.trafficlight.domain.core.exception;

public class IndexNotFoundException extends TrafficLightDomainException {
    public IndexNotFoundException(String indexName) {
        super("Index not found: " + indexName);
    }
}
