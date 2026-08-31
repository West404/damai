package com.damai.servicelock.info;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LockTimeOutStrategy 单元测试
 */
class LockTimeOutStrategyTest {

    @Test
    @DisplayName("FAIL策略应抛出RuntimeException并携带锁名提示")
    void failStrategyThrowsRuntimeException() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> LockTimeOutStrategy.FAIL.handler("my-lock"));

        assertEquals("my-lock请求频繁", exception.getMessage());
    }
}
