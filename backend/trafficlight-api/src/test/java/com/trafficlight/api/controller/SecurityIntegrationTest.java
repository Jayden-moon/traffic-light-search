package com.trafficlight.api.controller;

import com.trafficlight.usecase.AggregationUseCase;
import com.trafficlight.usecase.DataSyncUseCase;
import com.trafficlight.usecase.IndexUseCase;
import com.trafficlight.usecase.SearchUseCase;
import com.trafficlight.usecase.dto.IndexStatusResult;
import com.trafficlight.usecase.dto.SearchResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.trafficlight.api.config.SecurityConfig;
import com.trafficlight.api.config.CorsConfig;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * [SEC-002] A01/A07: 인증/인가 통합 테스트
 * 관리 엔드포인트는 X-Admin-Api-Key 헤더 필수, 공개 엔드포인트는 인증 불필요 검증.
 */
@WebMvcTest(controllers = {SearchController.class, IndexController.class, AggregationController.class, DataSyncController.class})
@Import({SecurityConfig.class, CorsConfig.class})
@TestPropertySource(properties = {
    "trafficlight.security.admin-key=test-secret-key",
    "trafficlight.cors.allowed-origins=http://localhost:3000"
})
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchUseCase searchUseCase;
    @MockBean
    private IndexUseCase indexUseCase;
    @MockBean
    private AggregationUseCase aggregationUseCase;
    @MockBean
    private DataSyncUseCase dataSyncUseCase;

    private static final String ADMIN_KEY_HEADER = "X-Admin-Api-Key";

    @Nested
    @DisplayName("공개 엔드포인트 - 인증 불필요")
    class PublicEndpoints {

        @Test
        @DisplayName("GET /api/search는 인증 없이 접근 가능하다")
        void searchIsPublic() throws Exception {
            SearchResult result = SearchResult.builder()
                    .total(0).page(0).size(20).results(Collections.emptyList()).build();
            when(searchUseCase.search(any())).thenReturn(result);

            mockMvc.perform(get("/api/search"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /api/index/status는 인증 없이 접근 가능하다")
        void indexStatusIsPublic() throws Exception {
            IndexStatusResult result = IndexStatusResult.builder()
                    .totalRecords(0).indexed(0).failed(0).durationMs(0).indexExists(false).build();
            when(indexUseCase.getStatus()).thenReturn(result);

            mockMvc.perform(get("/api/index/status"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("관리 엔드포인트 - 인증 필요")
    class AdminEndpoints {

        @Test
        @DisplayName("POST /api/index/create는 인증 없이 403을 반환한다")
        void createIndexRequiresAuth() throws Exception {
            mockMvc.perform(post("/api/index/create"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("DELETE /api/index/delete는 인증 없이 403을 반환한다")
        void deleteIndexRequiresAuth() throws Exception {
            mockMvc.perform(delete("/api/index/delete"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("POST /api/sync는 인증 없이 403을 반환한다")
        void syncRequiresAuth() throws Exception {
            mockMvc.perform(post("/api/sync"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("올바른 API Key로 인덱스 생성 가능하다")
        void createIndexWithValidKey() throws Exception {
            mockMvc.perform(post("/api/index/create")
                            .header(ADMIN_KEY_HEADER, "test-secret-key"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("잘못된 API Key로 403을 반환한다")
        void createIndexWithInvalidKey() throws Exception {
            mockMvc.perform(post("/api/index/create")
                            .header(ADMIN_KEY_HEADER, "wrong-key"))
                    .andExpect(status().isForbidden());
        }
    }
}
