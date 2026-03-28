package com.trafficlight.api.controller;

import com.trafficlight.domain.model.TrafficLight;
import com.trafficlight.usecase.SearchUseCase;
import com.trafficlight.usecase.dto.SearchCommand;
import com.trafficlight.usecase.dto.GeoSearchCommand;
import com.trafficlight.usecase.dto.SearchResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SearchController.class)
@AutoConfigureMockMvc(addFilters = false)
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchUseCase searchUseCase;

    @Nested
    @DisplayName("GET /api/search")
    class SearchEndpoint {

        @Test
        @DisplayName("검색어로 검색하면 200과 결과를 반환한다")
        void searchWithQuery() throws Exception {
            TrafficLight tl = TrafficLight.builder()
                    .sidoName("서울특별시")
                    .sigunguName("강남구")
                    .trafficLightId("TL-001")
                    .build();

            SearchResult result = SearchResult.builder()
                    .total(1)
                    .page(0)
                    .size(20)
                    .results(List.of(tl))
                    .build();

            when(searchUseCase.search(any(SearchCommand.class))).thenReturn(result);

            mockMvc.perform(get("/api/search")
                            .param("q", "강남"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(1))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(20))
                    .andExpect(jsonPath("$.results[0].sidoName").value("서울특별시"))
                    .andExpect(jsonPath("$.results[0].sigunguName").value("강남구"));
        }

        @Test
        @DisplayName("파라미터 없이 검색하면 200을 반환한다")
        void searchWithoutParams() throws Exception {
            SearchResult result = SearchResult.builder()
                    .total(0)
                    .page(0)
                    .size(20)
                    .results(Collections.emptyList())
                    .build();

            when(searchUseCase.search(any(SearchCommand.class))).thenReturn(result);

            mockMvc.perform(get("/api/search"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(0))
                    .andExpect(jsonPath("$.results").isArray());
        }

        @Test
        @DisplayName("필터와 페이지네이션 파라미터가 전달된다")
        void searchWithFiltersAndPagination() throws Exception {
            SearchResult result = SearchResult.builder()
                    .total(100)
                    .page(2)
                    .size(10)
                    .results(Collections.emptyList())
                    .build();

            when(searchUseCase.search(any(SearchCommand.class))).thenReturn(result);

            mockMvc.perform(get("/api/search")
                            .param("sidoName", "서울특별시")
                            .param("sigunguName", "강남구")
                            .param("roadType", "일반국도")
                            .param("trafficLightCategory", "차량신호등")
                            .param("page", "2")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page").value(2))
                    .andExpect(jsonPath("$.size").value(10));
        }
    }

    @Nested
    @DisplayName("GET /api/search/geo")
    class GeoSearchEndpoint {

        @Test
        @DisplayName("위치 기반 검색이 200을 반환한다")
        void geoSearch() throws Exception {
            TrafficLight tl = TrafficLight.builder()
                    .sidoName("서울특별시")
                    .latitude(37.5665)
                    .longitude(126.9780)
                    .build();

            SearchResult result = SearchResult.builder()
                    .total(1)
                    .page(0)
                    .size(20)
                    .results(List.of(tl))
                    .build();

            when(searchUseCase.geoSearch(any(GeoSearchCommand.class))).thenReturn(result);

            mockMvc.perform(get("/api/search/geo")
                            .param("lat", "37.5665")
                            .param("lon", "126.9780")
                            .param("distance", "1km"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(1))
                    .andExpect(jsonPath("$.results[0].latitude").value(37.5665));
        }

        @Test
        @DisplayName("필수 파라미터 lat 누락 시 400을 반환한다")
        void geoSearchMissingLat() throws Exception {
            mockMvc.perform(get("/api/search/geo")
                            .param("lon", "126.9780"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("필수 파라미터 lon 누락 시 400을 반환한다")
        void geoSearchMissingLon() throws Exception {
            mockMvc.perform(get("/api/search/geo")
                            .param("lat", "37.5665"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/search/filters/{field}")
    class FilterOptionsEndpoint {

        @Test
        @DisplayName("시도명 필터 옵션을 반환한다")
        void getFilterOptions() throws Exception {
            when(searchUseCase.getFilterOptions(eq("sidoName"), anyMap()))
                    .thenReturn(List.of("서울특별시", "부산광역시", "대구광역시"));

            mockMvc.perform(get("/api/search/filters/sidoName"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0]").value("서울특별시"))
                    .andExpect(jsonPath("$[1]").value("부산광역시"))
                    .andExpect(jsonPath("$[2]").value("대구광역시"));
        }

        @Test
        @DisplayName("부모 필터와 함께 시군구 옵션을 반환한다")
        void getFilterOptionsWithParent() throws Exception {
            when(searchUseCase.getFilterOptions(eq("sigunguName"), anyMap()))
                    .thenReturn(List.of("강남구", "서초구"));

            mockMvc.perform(get("/api/search/filters/sigunguName")
                            .param("sidoName", "서울특별시"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0]").value("강남구"));
        }
    }
}
