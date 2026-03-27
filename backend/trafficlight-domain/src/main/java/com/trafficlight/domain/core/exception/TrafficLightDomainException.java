package com.trafficlight.domain.core.exception;

public class TrafficLightDomainException extends RuntimeException {
    public TrafficLightDomainException(String message) {
        super(message);
    }
    public TrafficLightDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
