package com.trafficlight.usecase.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoSearchCommand {
    private double lat;
    private double lon;
    @Builder.Default
    private String distance = "1km";
    @Builder.Default
    private int page = 0;
    @Builder.Default
    private int size = 20;
}
