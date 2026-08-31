package com.damai.handle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * RedissonDataHandle 单元测试（RedissonClient 使用 Mockito mock，无需真实Redis）
 */
@ExtendWith(MockitoExtension.class)
class RedissonDataHandleTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RBucket<Object> bucket;

    @InjectMocks
    private RedissonDataHandle redissonDataHandle;

    @Test
    @DisplayName("get方法应通过getBucket获取key对应的值")
    void getReturnsBucketValue() {
        when(redissonClient.getBucket("test-key")).thenReturn(bucket);
        when(bucket.get()).thenReturn("test-value");

        String result = redissonDataHandle.get("test-key");

        assertEquals("test-value", result);
        verify(redissonClient).getBucket("test-key");
    }

    @Test
    @DisplayName("set方法(无过期时间)应调用bucket的set")
    void setWithoutTtl() {
        when(redissonClient.getBucket("test-key")).thenReturn(bucket);

        redissonDataHandle.set("test-key", "test-value");

        verify(bucket).set("test-value");
    }

    @Test
    @DisplayName("set方法(带过期时间)应将TimeUnit转换为Duration后调用bucket的set")
    void setWithTtlConvertsToDuration() {
        when(redissonClient.getBucket("test-key")).thenReturn(bucket);

        redissonDataHandle.set("test-key", "test-value", 5, TimeUnit.MINUTES);

        verify(bucket).set("test-value", Duration.ofMinutes(5));
    }

    @Test
    @DisplayName("getDuration应正确转换分钟/小时/天/秒")
    void getDurationConvertsTimeUnit() {
        assertEquals(Duration.ofMinutes(3), redissonDataHandle.getDuration(3, TimeUnit.MINUTES));
        assertEquals(Duration.ofHours(3), redissonDataHandle.getDuration(3, TimeUnit.HOURS));
        assertEquals(Duration.ofDays(3), redissonDataHandle.getDuration(3, TimeUnit.DAYS));
        // 其余单位（如SECONDS、MILLISECONDS）默认按秒处理
        assertEquals(Duration.ofSeconds(3), redissonDataHandle.getDuration(3, TimeUnit.SECONDS));
        assertEquals(Duration.ofSeconds(3), redissonDataHandle.getDuration(3, TimeUnit.MILLISECONDS));
    }
}
