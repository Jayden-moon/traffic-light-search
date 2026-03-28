package com.trafficlight.usecase;

import com.trafficlight.domain.service.DataCollectorService;
import com.trafficlight.domain.service.IndexService;
import com.trafficlight.usecase.dto.IndexStatusResult;
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
class DataSyncUseCaseTest {

    @Mock
    private DataCollectorService dataCollectorService;

    @Mock
    private IndexService indexService;

    private DataSyncUseCase dataSyncUseCase;

    @BeforeEach
    void setUp() {
        dataSyncUseCase = new DataSyncUseCase(dataCollectorService, indexService);
    }

    private Map<String, Object> createRecord(String id) {
        Map<String, Object> record = new HashMap<>();
        record.put("trafficLightId", id);
        record.put("sidoName", "서울특별시");
        return record;
    }

    @Nested
    @DisplayName("syncFromApi")
    class SyncFromApi {

        @Test
        @DisplayName("인덱스가 없으면 생성 후 동기화한다")
        void createsIndexIfNotExists() {
            when(indexService.indexExists()).thenReturn(false);
            when(dataCollectorService.getTotalCount()).thenReturn(100L);
            when(dataCollectorService.fetchPage(eq(1), eq(1000)))
                    .thenReturn(List.of(createRecord("TL-001")));
            when(indexService.bulkUpsert(anyList())).thenReturn(1L);

            dataSyncUseCase.syncFromApi();

            verify(indexService).createIndex();
        }

        @Test
        @DisplayName("인덱스가 이미 있으면 생성하지 않는다")
        void skipsIndexCreationIfExists() {
            when(indexService.indexExists()).thenReturn(true);
            when(dataCollectorService.getTotalCount()).thenReturn(100L);
            when(dataCollectorService.fetchPage(eq(1), eq(1000)))
                    .thenReturn(List.of(createRecord("TL-001")));
            when(indexService.bulkUpsert(anyList())).thenReturn(1L);

            dataSyncUseCase.syncFromApi();

            verify(indexService, never()).createIndex();
        }

        @Test
        @DisplayName("API에서 0건이면 즉시 반환한다")
        void returnsImmediatelyWhenZeroCount() {
            when(indexService.indexExists()).thenReturn(true);
            when(dataCollectorService.getTotalCount()).thenReturn(0L);

            IndexStatusResult result = dataSyncUseCase.syncFromApi();

            assertThat(result.getTotalRecords()).isEqualTo(0);
            assertThat(result.getIndexed()).isEqualTo(0);
            assertThat(result.isIndexExists()).isTrue();
            verify(dataCollectorService, never()).fetchPage(anyInt(), anyInt());
        }

        @Test
        @DisplayName("여러 페이지를 순차적으로 처리한다")
        void processesMultiplePages() {
            when(indexService.indexExists()).thenReturn(true);
            when(dataCollectorService.getTotalCount()).thenReturn(2500L);

            List<Map<String, Object>> page1 = List.of(createRecord("TL-001"), createRecord("TL-002"));
            List<Map<String, Object>> page2 = List.of(createRecord("TL-003"), createRecord("TL-004"));
            List<Map<String, Object>> page3 = List.of(createRecord("TL-005"));

            when(dataCollectorService.fetchPage(1, 1000)).thenReturn(page1);
            when(dataCollectorService.fetchPage(2, 1000)).thenReturn(page2);
            when(dataCollectorService.fetchPage(3, 1000)).thenReturn(page3);

            when(indexService.bulkUpsert(page1)).thenReturn(2L);
            when(indexService.bulkUpsert(page2)).thenReturn(2L);
            when(indexService.bulkUpsert(page3)).thenReturn(1L);

            IndexStatusResult result = dataSyncUseCase.syncFromApi();

            assertThat(result.getTotalRecords()).isEqualTo(2500);
            assertThat(result.getIndexed()).isEqualTo(5);
            assertThat(result.getFailed()).isEqualTo(0);
            assertThat(result.isIndexExists()).isTrue();
            assertThat(result.getDurationMs()).isGreaterThanOrEqualTo(0);

            verify(dataCollectorService).fetchPage(1, 1000);
            verify(dataCollectorService).fetchPage(2, 1000);
            verify(dataCollectorService).fetchPage(3, 1000);
        }

        @Test
        @DisplayName("빈 페이지가 반환되면 중단한다")
        void stopsOnEmptyPage() {
            when(indexService.indexExists()).thenReturn(true);
            when(dataCollectorService.getTotalCount()).thenReturn(3000L);

            when(dataCollectorService.fetchPage(1, 1000))
                    .thenReturn(List.of(createRecord("TL-001")));
            when(dataCollectorService.fetchPage(2, 1000))
                    .thenReturn(Collections.emptyList());

            when(indexService.bulkUpsert(anyList())).thenReturn(1L);

            IndexStatusResult result = dataSyncUseCase.syncFromApi();

            assertThat(result.getIndexed()).isEqualTo(1);
            verify(dataCollectorService, never()).fetchPage(eq(3), anyInt());
        }

        @Test
        @DisplayName("인덱싱 실패 건수가 올바르게 집계된다")
        void failedCountAccumulatedCorrectly() {
            when(indexService.indexExists()).thenReturn(true);
            when(dataCollectorService.getTotalCount()).thenReturn(1000L);

            List<Map<String, Object>> page = List.of(
                    createRecord("TL-001"), createRecord("TL-002"),
                    createRecord("TL-003"), createRecord("TL-004"),
                    createRecord("TL-005")
            );
            when(dataCollectorService.fetchPage(1, 1000)).thenReturn(page);
            when(indexService.bulkUpsert(page)).thenReturn(3L); // 5 sent, 3 succeeded

            IndexStatusResult result = dataSyncUseCase.syncFromApi();

            assertThat(result.getIndexed()).isEqualTo(3);
            assertThat(result.getFailed()).isEqualTo(2);
        }
    }
}
