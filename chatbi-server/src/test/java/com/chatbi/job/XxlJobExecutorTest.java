package com.chatbi.job;

import com.chatbi.service.QueryExecutionService;
import com.chatbi.service.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * XXL-JOB 执行器单元测试
 */
@ExtendWith(MockitoExtension.class)
class XxlJobExecutorTest {

    @Mock
    private QueryExecutionService queryExecutionService;

    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private XxlJobExecutor xxlJobExecutor;

    @Test
    void dataSourceHealthCheckJob_shouldCallHealthCheck() {
        xxlJobExecutor.dataSourceHealthCheckJob();
        verify(queryExecutionService, times(1)).healthCheck();
    }

    @Test
    void subscriptionPushJob_shouldCallCheckAndPush() {
        xxlJobExecutor.subscriptionPushJob();
        verify(subscriptionService, times(1)).checkAndPush();
    }
}
