package com.damai;

import com.damai.constant.Constant;
import com.damai.threadlocal.BaseParameterHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BusinessThreadPool 跨线程的 MDC/ThreadLocal 上下文传递单元测试
 */
class BusinessThreadPoolTest {

    @AfterEach
    void cleanUp() {
        // 清理主线程上下文，避免污染其他用例
        MDC.clear();
        BaseParameterHolder.removeParameterMap();
    }

    @Test
    @DisplayName("submit：提交任务时应将主线程的 MDC traceId 和 BaseParameterHolder 参数传递到线程池工作线程")
    void submitShouldPropagateMdcAndHolderToWorkerThread() throws Exception {
        MDC.put(Constant.TRACE_ID, "test-trace-id-001");
        BaseParameterHolder.setParameter("userId", "user-123");

        Future<String[]> future = BusinessThreadPool.submit(() -> new String[]{
                MDC.get(Constant.TRACE_ID),
                BaseParameterHolder.getParameter("userId")
        });

        String[] result = future.get(10, TimeUnit.SECONDS);

        // 工作线程中应能读到主线程提交任务时的上下文
        assertEquals("test-trace-id-001", result[0]);
        assertEquals("user-123", result[1]);
    }

    @Test
    @DisplayName("execute：执行任务时应传递上下文，且任务结束后不影响主线程自身上下文")
    void executeShouldPropagateContextAndKeepCallerContextIntact() throws Exception {
        MDC.put(Constant.TRACE_ID, "test-trace-id-002");
        BaseParameterHolder.setParameter("orderNo", "order-456");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> traceIdInWorker = new AtomicReference<>();
        AtomicReference<String> orderNoInWorker = new AtomicReference<>();

        BusinessThreadPool.execute(() -> {
            traceIdInWorker.set(MDC.get(Constant.TRACE_ID));
            orderNoInWorker.set(BaseParameterHolder.getParameter("orderNo"));
            latch.countDown();
        });

        assertTrue(latch.await(10, TimeUnit.SECONDS), "线程池任务未在超时时间内执行完成");

        assertEquals("test-trace-id-002", traceIdInWorker.get());
        assertEquals("order-456", orderNoInWorker.get());

        // 主线程上下文不被线程池任务污染
        assertEquals("test-trace-id-002", MDC.get(Constant.TRACE_ID));
        assertEquals("order-456", BaseParameterHolder.getParameter("orderNo"));
    }

    @Test
    @DisplayName("submit：主线程无上下文时，工作线程内也不应有 traceId")
    void submitWithoutContextShouldNotLeakOtherContext() throws Exception {
        Future<String> future = BusinessThreadPool.submit(() -> MDC.get(Constant.TRACE_ID));

        String traceId = future.get(10, TimeUnit.SECONDS);

        assertNull(traceId);
    }
}
