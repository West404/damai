package com.damai.pro.limit;

import com.damai.enums.BaseCode;
import com.damai.exception.DaMaiFrameException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @description: RateLimiter 基于信号量的限流器 单元测试
 **/
class RateLimiterTest {

    @Test
    @DisplayName("获取许可：许可充足时正常获取")
    void testAcquireSuccess() {
        RateLimiter rateLimiter = new RateLimiter(2);
        assertDoesNotThrow(rateLimiter::acquire);
        assertDoesNotThrow(rateLimiter::acquire);
    }

    @Test
    @DisplayName("获取许可：许可耗尽且1秒内无释放时抛出限流异常")
    void testAcquireExceedPermits() {
        RateLimiter rateLimiter = new RateLimiter(1);
        assertDoesNotThrow(rateLimiter::acquire);
        //不释放许可，再次获取需等待最多1秒后失败
        DaMaiFrameException exception = assertThrows(DaMaiFrameException.class, rateLimiter::acquire);
        assertEquals(BaseCode.OPERATION_IS_TOO_FREQUENT_PLEASE_TRY_AGAIN_LATER.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("释放许可：释放后可重新获取许可")
    void testReleaseThenAcquire() {
        RateLimiter rateLimiter = new RateLimiter(1);
        assertDoesNotThrow(rateLimiter::acquire);
        rateLimiter.release();
        assertDoesNotThrow(rateLimiter::acquire);
    }
}
