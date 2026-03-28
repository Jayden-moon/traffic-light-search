package com.trafficlight.api.controller;

import com.trafficlight.usecase.DataSyncUseCase;
import com.trafficlight.usecase.dto.IndexStatusResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DataSyncController.class)
@AutoConfigureMockMvc(addFilters = false)
class DataSyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DataSyncUseCase dataSyncUseCase;

    @Test
    @DisplayName("POST /api/sync - API 동기화 결과를 반환한다")
    void syncFromApiReturnsResult() throws Exception {
        IndexStatusResult result = IndexStatusResult.builder()
                .totalRecords(50000)
                .indexed(49500)
                .failed(500)
                .durationMs(30000)
                .indexExists(true)
                .build();

        when(dataSyncUseCase.syncFromApi()).thenReturn(result);

        mockMvc.perform(post("/api/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecords").value(50000))
                .andExpect(jsonPath("$.indexed").value(49500))
                .andExpect(jsonPath("$.failed").value(500))
                .andExpect(jsonPath("$.durationMs").value(30000))
                .andExpect(jsonPath("$.indexExists").value(true));
    }
}
