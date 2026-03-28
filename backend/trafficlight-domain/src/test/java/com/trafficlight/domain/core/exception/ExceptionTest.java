package com.trafficlight.domain.core.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionTest {

    @Test
    @DisplayName("TrafficLightDomainException에 메시지가 설정된다")
    void domainExceptionMessage() {
        TrafficLightDomainException ex = new TrafficLightDomainException("test error");

        assertThat(ex.getMessage()).isEqualTo("test error");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("TrafficLightDomainException에 cause가 설정된다")
    void domainExceptionWithCause() {
        RuntimeException cause = new RuntimeException("root cause");
        TrafficLightDomainException ex = new TrafficLightDomainException("wrapped", cause);

        assertThat(ex.getMessage()).isEqualTo("wrapped");
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("IndexNotFoundException에 인덱스명이 포함된다")
    void indexNotFoundExceptionIncludesIndexName() {
        IndexNotFoundException ex = new IndexNotFoundException("traffic-lights");

        assertThat(ex.getMessage()).isEqualTo("Index not found: traffic-lights");
        assertThat(ex).isInstanceOf(TrafficLightDomainException.class);
    }

    @Test
    @DisplayName("DataLoadException에 메시지가 설정된다")
    void dataLoadExceptionMessage() {
        DataLoadException ex = new DataLoadException("file not found");

        assertThat(ex.getMessage()).isEqualTo("file not found");
        assertThat(ex).isInstanceOf(TrafficLightDomainException.class);
    }

    @Test
    @DisplayName("DataLoadException에 cause가 설정된다")
    void dataLoadExceptionWithCause() {
        Exception cause = new java.io.IOException("disk error");
        DataLoadException ex = new DataLoadException("load failed", cause);

        assertThat(ex.getMessage()).isEqualTo("load failed");
        assertThat(ex.getCause()).isEqualTo(cause);
    }
}
