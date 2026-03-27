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
public class SearchCommand {
    private String q;
    private String sidoName;
    private String sigunguName;
    private String roadType;
    private String trafficLightCategory;
    @Builder.Default
    private int page = 0;
    @Builder.Default
    private int size = 20;
}
