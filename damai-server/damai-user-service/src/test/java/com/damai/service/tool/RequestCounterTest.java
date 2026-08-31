package com.damai.service.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RequestCounter 纯逻辑单元测试：每秒请求数限流计数器
 */
class RequestCounterTest {

    private RequestCounter requestCounter;

    @BeforeEach
    void setUp() {
        requestCounter = new RequestCounter();
        // 将每秒阈值设置为 3，便于测试
        ReflectionTestUtils.setField(requestCounter, "maxRequestsPerSecond", 3);
    }

    @Test
    @DisplayName("请求数未超过阈值时不触发限流")
    void onRequestBelowThreshold() {
        assertFalse(requestCounter.onRequest());
        assertFalse(requestCounter.onRequest());
        assertFalse(requestCounter.onRequest());
    }

    @Test
    @DisplayName("请求数超过阈值时触发限流并返回true")
    void onRequestExceedThreshold() {
        requestCounter.onRequest();
        requestCounter.onRequest();
        requestCounter.onRequest();
        // 第4次请求超过阈值3，应返回true
        assertTrue(requestCounter.onRequest());
    }

    @Test
    @DisplayName("触发限流后计数器清零，后续请求重新计数")
    void onRequestResetAfterExceed() {
        requestCounter.onRequest();
        requestCounter.onRequest();
        requestCounter.onRequest();
        assertTrue(requestCounter.onRequest());
        // 触发限流后计数已清零，接下来3次请求不应再触发限流
        assertFalse(requestCounter.onRequest());
        assertFalse(requestCounter.onRequest());
        assertFalse(requestCounter.onRequest());
    }

    @Test
    @DisplayName("超过1秒时间窗口后计数器自动重置")
    void onRequestResetByTimeWindow() throws InterruptedException {
        ReflectionTestUtils.setField(requestCounter, "maxRequestsPerSecond", 1);
        requestCounter.onRequest();
        // 第2次请求超过阈值1，触发限流
        assertTrue(requestCounter.onRequest());
        // 计数清零后第一次请求
        assertFalse(requestCounter.onRequest());
        // 等待超过1秒的时间窗口
        Thread.sleep(1100);
        // 时间窗口过期，计数重置，不应触发限流
        assertFalse(requestCounter.onRequest());
    }
}
