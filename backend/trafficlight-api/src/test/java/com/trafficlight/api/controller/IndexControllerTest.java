package com.trafficlight.api.controller;

import com.trafficlight.usecase.IndexUseCase;
import com.trafficlight.usecase.dto.IndexStatusResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IndexController.class)
@AutoConfigureMockMvc(addFilters = false)
class IndexControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IndexUseCase indexUseCase;

    @Nested
    @DisplayName("POST /api/index/create")
    class CreateIndex {

        @Test
        @DisplayName("인덱스 생성 성공 시 200과 메시지를 반환한다")
        void createIndexSuccess() throws Exception {
            doNothing().when(indexUseCase).createIndex();

            mockMvc.perform(post("/api/index/create"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Index created"));

            verify(indexUseCase).createIndex();
        }
    }

    @Nested
    @DisplayName("POST /api/index/load")
    class LoadData {

        @Test
        @DisplayName("데이터 로드 후 상태를 반환한다")
        void loadDataReturnsStatus() throws Exception {
            IndexStatusResult result = IndexStatusResult.builder()
                    .totalRecords(10000)
                    .indexed(9950)
                    .failed(50)
                    .durationMs(5000)
                    .indexExists(true)
                    .build();

            when(indexUseCase.loadData()).thenReturn(result);

            mockMvc.perform(post("/api/index/load"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalRecords").value(10000))
                    .andExpect(jsonPath("$.indexed").value(9950))
                    .andExpect(jsonPath("$.failed").value(50))
                    .andExpect(jsonPath("$.durationMs").value(5000))
                    .andExpect(jsonPath("$.indexExists").value(true));
        }
    }

    @Nested
    @DisplayName("DELETE /api/index/delete")
    class DeleteIndex {

        @Test
        @DisplayName("인덱스 삭제 성공 시 200과 메시지를 반환한다")
        void deleteIndexSuccess() throws Exception {
            doNothing().when(indexUseCase).deleteIndex();

            mockMvc.perform(delete("/api/index/delete"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Index deleted"));
        }
    }

    @Nested
    @DisplayName("GET /api/index/status")
    class GetStatus {

        @Test
        @DisplayName("인덱스 상태를 반환한다")
        void getStatusReturnsIndexInfo() throws Exception {
            IndexStatusResult result = IndexStatusResult.builder()
                    .totalRecords(5000)
                    .indexed(5000)
                    .failed(0)
                    .durationMs(0)
                    .indexExists(true)
                    .build();

            when(indexUseCase.getStatus()).thenReturn(result);

            mockMvc.perform(get("/api/index/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalRecords").value(5000))
                    .andExpect(jsonPath("$.indexed").value(5000))
                    .andExpect(jsonPath("$.indexExists").value(true));
        }

        @Test
        @DisplayName("인덱스가 없을 때 상태를 반환한다")
        void getStatusWhenNoIndex() throws Exception {
            IndexStatusResult result = IndexStatusResult.builder()
                    .totalRecords(0)
                    .indexed(0)
                    .failed(0)
                    .durationMs(0)
                    .indexExists(false)
                    .build();

            when(indexUseCase.getStatus()).thenReturn(result);

            mockMvc.perform(get("/api/index/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.indexExists").value(false))
                    .andExpect(jsonPath("$.totalRecords").value(0));
        }
    }
}
