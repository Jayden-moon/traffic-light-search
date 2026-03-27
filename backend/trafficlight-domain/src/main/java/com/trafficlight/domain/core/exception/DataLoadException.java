package com.trafficlight.domain.core.exception;

public class DataLoadException extends TrafficLightDomainException {
    public DataLoadException(String message) {
        super(message);
    }
    public DataLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
