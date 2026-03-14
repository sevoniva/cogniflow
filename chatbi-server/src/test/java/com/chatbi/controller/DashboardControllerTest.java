package com.chatbi.controller;

import com.chatbi.service.BusinessInsightService;
import com.chatbi.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardController 测试")
class DashboardControllerTest {

    @Mock
    private DashboardService dashboardService;

    @Mock
    private BusinessInsightService businessInsightService;

    @InjectMocks
    private DashboardController dashboardController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(dashboardController).build();
    }

    @Test
    @DisplayName("发布仪表板 - 默认发布状态")
    void testPublishDefaultStatus() throws Exception {
        mockMvc.perform(patch("/api/dashboards/{id}/publish", 12L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(dashboardService).togglePublish(12L, 1);
    }

    @Test
    @DisplayName("发布仪表板 - 指定状态")
    void testPublishSpecifiedStatus() throws Exception {
        mockMvc.perform(patch("/api/dashboards/{id}/publish", 12L).param("status", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(dashboardService).togglePublish(12L, 0);
    }
}
