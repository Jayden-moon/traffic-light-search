package com.trafficlight.usecase;

import com.trafficlight.domain.model.TrafficLight;
import com.trafficlight.domain.service.SearchService;
import com.trafficlight.usecase.dto.GeoSearchCommand;
import com.trafficlight.usecase.dto.SearchCommand;
import com.trafficlight.usecase.dto.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchUseCaseTest {

    @Mock
    private SearchService searchService;

    private SearchUseCase searchUseCase;

    @BeforeEach
    void setUp() {
        searchUseCase = new SearchUseCase(searchService);
    }

    @Nested
    @DisplayName("search")
    class Search {

        @Test
        @DisplayName("검색어와 필터로 검색하고 결과를 반환한다")
        void searchWithQueryAndFilters() {
            TrafficLight tl = TrafficLight.builder()
                    .sidoName("서울특별시")
                    .sigunguName("강남구")
                    .build();

            when(searchService.search(eq("강남"), anyMap(), eq(0), eq(20)))
                    .thenReturn(List.of(tl));
            when(searchService.count(eq("강남"), anyMap()))
                    .thenReturn(1L);

            SearchCommand command = SearchCommand.builder()
                    .q("강남")
                    .sidoName("서울특별시")
                    .page(0)
                    .size(20)
                    .build();

            SearchResult result = searchUseCase.search(command);

            assertThat(result.getTotal()).isEqualTo(1);
            assertThat(result.getPage()).isEqualTo(0);
            assertThat(result.getSize()).isEqualTo(20);
            assertThat(result.getResults()).hasSize(1);
            assertThat(result.getResults().get(0).getSidoName()).isEqualTo("서울특별시");
        }

        @Test
        @DisplayName("필터가 비어있으면 빈 Map으로 전달된다")
        void searchWithoutFilters() {
            when(searchService.search(isNull(), eq(Collections.emptyMap()), eq(0), eq(20)))
                    .thenReturn(Collections.emptyList());
            when(searchService.count(isNull(), eq(Collections.emptyMap())))
                    .thenReturn(0L);

            SearchCommand command = SearchCommand.builder()
                    .page(0)
                    .size(20)
                    .build();

            SearchResult result = searchUseCase.search(command);

            assertThat(result.getTotal()).isEqualTo(0);
            assertThat(result.getResults()).isEmpty();
        }

        @Test
        @DisplayName("sidoName 필터가 올바르게 전달된다")
        void searchWithSidoNameFilter() {
            Map<String, String> expectedFilters = new HashMap<>();
            expectedFilters.put("sidoName", "서울특별시");

            when(searchService.search(isNull(), eq(expectedFilters), eq(0), eq(20)))
                    .thenReturn(Collections.emptyList());
            when(searchService.count(isNull(), eq(expectedFilters)))
                    .thenReturn(0L);

            SearchCommand command = SearchCommand.builder()
                    .sidoName("서울특별시")
                    .page(0)
                    .size(20)
                    .build();

            searchUseCase.search(command);

            verify(searchService).search(isNull(), eq(expectedFilters), eq(0), eq(20));
        }

        @Test
        @DisplayName("모든 필터가 올바르게 전달된다")
        void searchWithAllFilters() {
            Map<String, String> expectedFilters = new HashMap<>();
            expectedFilters.put("sidoName", "서울특별시");
            expectedFilters.put("sigunguName", "강남구");
            expectedFilters.put("roadType", "일반국도");
            expectedFilters.put("trafficLightCategory", "차량신호등");

            when(searchService.search(isNull(), eq(expectedFilters), eq(0), eq(10)))
                    .thenReturn(Collections.emptyList());
            when(searchService.count(isNull(), eq(expectedFilters)))
                    .thenReturn(0L);

            SearchCommand command = SearchCommand.builder()
                    .sidoName("서울특별시")
                    .sigunguName("강남구")
                    .roadType("일반국도")
                    .trafficLightCategory("차량신호등")
                    .page(0)
                    .size(10)
                    .build();

            searchUseCase.search(command);

            verify(searchService).search(isNull(), eq(expectedFilters), eq(0), eq(10));
        }

        @Test
        @DisplayName("빈 문자열 필터는 무시된다")
        void emptyStringFiltersIgnored() {
            when(searchService.search(isNull(), eq(Collections.emptyMap()), eq(0), eq(20)))
                    .thenReturn(Collections.emptyList());
            when(searchService.count(isNull(), eq(Collections.emptyMap())))
                    .thenReturn(0L);

            SearchCommand command = SearchCommand.builder()
                    .sidoName("")
                    .sigunguName("")
                    .roadType("")
                    .trafficLightCategory("")
                    .page(0)
                    .size(20)
                    .build();

            searchUseCase.search(command);

            verify(searchService).search(isNull(), eq(Collections.emptyMap()), eq(0), eq(20));
        }

        @Test
        @DisplayName("페이지네이션이 올바르게 전달된다")
        void paginationPassedCorrectly() {
            when(searchService.search(isNull(), anyMap(), eq(3), eq(50)))
                    .thenReturn(Collections.emptyList());
            when(searchService.count(isNull(), anyMap()))
                    .thenReturn(200L);

            SearchCommand command = SearchCommand.builder()
                    .page(3)
                    .size(50)
                    .build();

            SearchResult result = searchUseCase.search(command);

            assertThat(result.getPage()).isEqualTo(3);
            assertThat(result.getSize()).isEqualTo(50);
            assertThat(result.getTotal()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("geoSearch")
    class GeoSearch {

        @Test
        @DisplayName("위치 기반 검색이 올바르게 동작한다")
        void geoSearchReturnsResults() {
            TrafficLight tl = TrafficLight.builder()
                    .sidoName("서울특별시")
                    .latitude(37.5665)
                    .longitude(126.9780)
                    .build();

            when(searchService.geoSearch(37.5665, 126.9780, "1km", 0, 20))
                    .thenReturn(List.of(tl));
            when(searchService.geoCount(37.5665, 126.9780, "1km"))
                    .thenReturn(1L);

            GeoSearchCommand command = GeoSearchCommand.builder()
                    .lat(37.5665)
                    .lon(126.9780)
                    .distance("1km")
                    .page(0)
                    .size(20)
                    .build();

            SearchResult result = searchUseCase.geoSearch(command);

            assertThat(result.getTotal()).isEqualTo(1);
            assertThat(result.getResults()).hasSize(1);
        }

        @Test
        @DisplayName("거리 파라미터가 올바르게 전달된다")
        void distanceParameterPassedCorrectly() {
            when(searchService.geoSearch(eq(36.0), eq(127.0), eq("5km"), eq(0), eq(100)))
                    .thenReturn(Collections.emptyList());
            when(searchService.geoCount(eq(36.0), eq(127.0), eq("5km")))
                    .thenReturn(0L);

            GeoSearchCommand command = GeoSearchCommand.builder()
                    .lat(36.0)
                    .lon(127.0)
                    .distance("5km")
                    .page(0)
                    .size(100)
                    .build();

            searchUseCase.geoSearch(command);

            verify(searchService).geoSearch(36.0, 127.0, "5km", 0, 100);
            verify(searchService).geoCount(36.0, 127.0, "5km");
        }
    }

    @Nested
    @DisplayName("getFilterOptions")
    class GetFilterOptions {

        @Test
        @DisplayName("필터 옵션을 반환한다")
        void returnsFilterOptions() {
            when(searchService.getFilterOptions(eq("sidoName"), anyMap()))
                    .thenReturn(List.of("서울특별시", "부산광역시", "대구광역시"));

            List<String> options = searchUseCase.getFilterOptions("sidoName", Collections.emptyMap());

            assertThat(options).containsExactly("서울특별시", "부산광역시", "대구광역시");
        }

        @Test
        @DisplayName("부모 필터와 함께 하위 필터 옵션을 반환한다")
        void returnsFilterOptionsWithParentFilter() {
            Map<String, String> parentFilter = Map.of("sidoName", "서울특별시");

            when(searchService.getFilterOptions("sigunguName", parentFilter))
                    .thenReturn(List.of("강남구", "서초구", "송파구"));

            List<String> options = searchUseCase.getFilterOptions("sigunguName", parentFilter);

            assertThat(options).hasSize(3);
            verify(searchService).getFilterOptions("sigunguName", parentFilter);
        }
    }
}
