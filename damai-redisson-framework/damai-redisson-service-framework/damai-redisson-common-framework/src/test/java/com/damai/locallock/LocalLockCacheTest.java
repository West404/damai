package com.damai.locallock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 本地锁缓存 LocalLockCache 单元测试
 */
class LocalLockCacheTest {

    private LocalLockCache localLockCache;

    @BeforeEach
    void setUp() {
        localLockCache = new LocalLockCache();
        // @Value 注入的字段在纯单元测试中为空，这里通过反射设置过期时间（小时）
        ReflectionTestUtils.setField(localLockCache, "durationTime", 1);
        // 手动触发 @PostConstruct 初始化
        localLockCache.localLockCacheInit();
    }

    @Test
    @DisplayName("同一个lockKey多次获取应返回同一把锁实例")
    void sameKeyReturnsSameLock() {
        ReentrantLock lock1 = localLockCache.getLock("order-lock", true);
        ReentrantLock lock2 = localLockCache.getLock("order-lock", false);
        assertNotNull(lock1);
        assertSame(lock1, lock2);
    }

    @Test
    @DisplayName("不同的lockKey应返回不同的锁实例")
    void differentKeyReturnsDifferentLock() {
        ReentrantLock lock1 = localLockCache.getLock("key-a", false);
        ReentrantLock lock2 = localLockCache.getLock("key-b", false);
        assertNotSame(lock1, lock2);
    }

    @Test
    @DisplayName("fair参数为true时创建的锁应为公平锁")
    void fairLockIsCreatedWhenFairIsTrue() {
        ReentrantLock fairLock = localLockCache.getLock("fair-key", true);
        ReentrantLock unfairLock = localLockCache.getLock("unfair-key", false);
        assertTrue(fairLock.isFair());
        assertFalse(unfairLock.isFair());
    }

    @Test
    @DisplayName("获取到的锁可以正常加锁和解锁")
    void lockCanBeUsed() {
        ReentrantLock lock = localLockCache.getLock("use-key", false);
        assertTrue(lock.tryLock());
        assertTrue(lock.isLocked());
        lock.unlock();
        assertFalse(lock.isLocked());
    }
}
