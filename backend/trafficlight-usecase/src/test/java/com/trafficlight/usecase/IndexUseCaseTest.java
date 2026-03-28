package com.trafficlight.usecase;

import com.trafficlight.domain.service.IndexService;
import com.trafficlight.usecase.dto.IndexStatusResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IndexUseCaseTest {

    @Mock
    private IndexService indexService;

    private IndexUseCase indexUseCase;

    @BeforeEach
    void setUp() {
        indexUseCase = new IndexUseCase(indexService);
    }

    @Nested
    @DisplayName("createIndex")
    class CreateIndex {

        @Test
        @DisplayName("인덱스 생성을 위임한다")
        void delegatesToIndexService() {
            indexUseCase.createIndex();

            verify(indexService).createIndex();
        }
    }

    @Nested
    @DisplayName("loadData")
    class LoadData {

        @Test
        @DisplayName("데이터 로드 후 상태를 반환한다")
        void returnsStatusAfterLoad() {
            when(indexService.loadData()).thenReturn(1000L);
            when(indexService.indexExists()).thenReturn(true);
            when(indexService.documentCount()).thenReturn(995L);

            IndexStatusResult result = indexUseCase.loadData();

            assertThat(result.getTotalRecords()).isEqualTo(1000);
            assertThat(result.getIndexed()).isEqualTo(995);
            assertThat(result.getFailed()).isEqualTo(5);
            assertThat(result.isIndexExists()).isTrue();
            assertThat(result.getDurationMs()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("실패 건수가 올바르게 계산된다")
        void failedCountCalculatedCorrectly() {
            when(indexService.loadData()).thenReturn(500L);
            when(indexService.indexExists()).thenReturn(true);
            when(indexService.documentCount()).thenReturn(500L);

            IndexStatusResult result = indexUseCase.loadData();

            assertThat(result.getFailed()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("deleteIndex")
    class DeleteIndex {

        @Test
        @DisplayName("인덱스 삭제를 위임한다")
        void delegatesToIndexService() {
            indexUseCase.deleteIndex();

            verify(indexService).deleteIndex();
        }
    }

    @Nested
    @DisplayName("getStatus")
    class GetStatus {

        @Test
        @DisplayName("인덱스가 존재할 때 문서 수를 반환한다")
        void returnsDocCountWhenIndexExists() {
            when(indexService.indexExists()).thenReturn(true);
            when(indexService.documentCount()).thenReturn(5000L);

            IndexStatusResult result = indexUseCase.getStatus();

            assertThat(result.getTotalRecords()).isEqualTo(5000);
            assertThat(result.getIndexed()).isEqualTo(5000);
            assertThat(result.getFailed()).isEqualTo(0);
            assertThat(result.getDurationMs()).isEqualTo(0);
            assertThat(result.isIndexExists()).isTrue();
        }

        @Test
        @DisplayName("인덱스가 없으면 0을 반환한다")
        void returnsZeroWhenIndexNotExists() {
            when(indexService.indexExists()).thenReturn(false);

            IndexStatusResult result = indexUseCase.getStatus();

            assertThat(result.getTotalRecords()).isEqualTo(0);
            assertThat(result.getIndexed()).isEqualTo(0);
            assertThat(result.isIndexExists()).isFalse();
            verify(indexService, never()).documentCount();
        }
    }
}
