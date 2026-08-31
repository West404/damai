package com.damai.rejectedexecutionhandler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * 线程池拒绝策略单元测试
 */
class ThreadPoolRejectedExecutionHandlerTest {

    @Test
    @DisplayName("BusinessAbortPolicy：任务被拒绝时应抛出 RejectedExecutionException，且异常信息包含任务描述")
    void businessAbortPolicyShouldThrowRejectedExecutionException() {
        ThreadPoolRejectedExecutionHandler.BusinessAbortPolicy policy =
                new ThreadPoolRejectedExecutionHandler.BusinessAbortPolicy();

        Runnable task = new Runnable() {
            @Override
            public void run() {
            }

            @Override
            public String toString() {
                return "test-task-marker";
            }
        };

        RejectedExecutionException exception = assertThrows(RejectedExecutionException.class,
                () -> policy.rejectedExecution(task, mock(ThreadPoolExecutor.class)));

        // 异常信息中应包含被拒绝任务的信息，便于排查
        assertTrue(exception.getMessage().contains("test-task-marker"));
        assertTrue(exception.getMessage().contains("rejected from"));
    }
}
