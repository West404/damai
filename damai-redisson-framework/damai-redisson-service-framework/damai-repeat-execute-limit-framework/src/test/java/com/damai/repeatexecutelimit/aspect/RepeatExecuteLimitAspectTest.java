package com.damai.repeatexecutelimit.aspect;

import com.damai.constant.LockInfoType;
import com.damai.exception.DaMaiFrameException;
import com.damai.handle.RedissonDataHandle;
import com.damai.locallock.LocalLockCache;
import com.damai.lockinfo.LockInfoHandle;
import com.damai.lockinfo.factory.LockInfoHandleFactory;
import com.damai.repeatexecutelimit.annotion.RepeatExecuteLimit;
import com.damai.servicelock.LockType;
import com.damai.servicelock.ServiceLocker;
import com.damai.servicelock.factory.ServiceLockFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static com.damai.repeatexecutelimit.constant.RepeatExecuteLimitConstant.PREFIX_NAME;
import static com.damai.repeatexecutelimit.constant.RepeatExecuteLimitConstant.SUCCESS_FLAG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RepeatExecuteLimitAspect 防重复幂等切面单元测试（锁与 Redis 句柄全部 mock）
 */
@ExtendWith(MockitoExtension.class)
class RepeatExecuteLimitAspectTest {

    private static final String LOCK_NAME = "createOrder_1001";
    private static final String FLAG_NAME = PREFIX_NAME + LOCK_NAME;

    @Mock
    private LocalLockCache localLockCache;

    @Mock
    private LockInfoHandleFactory lockInfoHandleFactory;

    @Mock
    private ServiceLockFactory serviceLockFactory;

    @Mock
    private RedissonDataHandle redissonDataHandle;

    @Mock
    private LockInfoHandle lockInfoHandle;

    @Mock
    private ServiceLocker serviceLocker;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private RepeatExecuteLimit repeatLimit;

    private RepeatExecuteLimitAspect aspect;

    private ReentrantLock localLock;

    @BeforeEach
    void setUp() {
        aspect = new RepeatExecuteLimitAspect(localLockCache, lockInfoHandleFactory, serviceLockFactory, redissonDataHandle);
        localLock = new ReentrantLock();
        when(repeatLimit.durationTime()).thenReturn(60L);
        when(repeatLimit.message()).thenReturn("提交频繁，请稍后重试");
        when(repeatLimit.name()).thenReturn("createOrder");
        when(repeatLimit.keys()).thenReturn(new String[]{});
        when(lockInfoHandleFactory.getLockInfoHandle(LockInfoType.REPEAT_EXECUTE_LIMIT)).thenReturn(lockInfoHandle);
        when(lockInfoHandle.getLockName(eq(joinPoint), anyString(), any())).thenReturn(LOCK_NAME);
    }

    @Test
    @DisplayName("成功标记已存在时直接抛出防重复异常，不获取任何锁")
    void aroundThrowsWhenSuccessFlagExists() throws Throwable {
        when(redissonDataHandle.get(FLAG_NAME)).thenReturn(SUCCESS_FLAG);

        DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                () -> aspect.around(joinPoint, repeatLimit));

        // 注意：DaMaiFrameException(String) 构造器只设置 Throwable 的 message，自身 message 字段为 null
        assertNull(exception.getMessage());
        verify(localLockCache, never()).getLock(anyString(), eq(true));
    }

    @Test
    @DisplayName("本地锁被占用时抛出防重复异常")
    void aroundThrowsWhenLocalLockOccupied() throws Throwable {
        when(redissonDataHandle.get(FLAG_NAME)).thenReturn(null);
        // ReentrantLock 可重入，同线程持有无法模拟占用，这里用 mock 的锁返回获取失败
        ReentrantLock occupiedLock = mock(ReentrantLock.class);
        when(occupiedLock.tryLock()).thenReturn(false);
        when(localLockCache.getLock(LOCK_NAME, true)).thenReturn(occupiedLock);

        assertThrows(DaMaiFrameException.class, () -> aspect.around(joinPoint, repeatLimit));

        verify(serviceLockFactory, never()).getLock(LockType.Fair);
    }

    @Test
    @DisplayName("分布式锁获取失败时抛出异常且释放本地锁")
    void aroundThrowsWhenDistributedLockFailed() throws Throwable {
        when(redissonDataHandle.get(FLAG_NAME)).thenReturn(null);
        when(localLockCache.getLock(LOCK_NAME, true)).thenReturn(localLock);
        when(serviceLockFactory.getLock(LockType.Fair)).thenReturn(serviceLocker);
        when(serviceLocker.tryLock(LOCK_NAME, TimeUnit.SECONDS, 0)).thenReturn(false);

        assertThrows(DaMaiFrameException.class, () -> aspect.around(joinPoint, repeatLimit));

        // finally 中应释放本地锁
        assertFalse(localLock.isLocked(), "分布式锁失败后本地锁必须释放");
        verify(joinPoint, never()).proceed();
    }

    @Test
    @DisplayName("双重检查：获取分布式锁后再次发现成功标记则抛出异常")
    void aroundThrowsWhenFlagAppearsAfterDoubleCheck() throws Throwable {
        // 第一次检查无标记，第二次检查（锁内）发现标记
        when(redissonDataHandle.get(FLAG_NAME)).thenReturn(null, SUCCESS_FLAG);
        when(localLockCache.getLock(LOCK_NAME, true)).thenReturn(localLock);
        when(serviceLockFactory.getLock(LockType.Fair)).thenReturn(serviceLocker);
        when(serviceLocker.tryLock(LOCK_NAME, TimeUnit.SECONDS, 0)).thenReturn(true);

        assertThrows(DaMaiFrameException.class, () -> aspect.around(joinPoint, repeatLimit));

        verify(joinPoint, never()).proceed();
        verify(serviceLocker).unlock(LOCK_NAME);
        assertFalse(localLock.isLocked());
    }

    @Test
    @DisplayName("正常执行：返回业务结果、写入成功标记并释放两把锁")
    void aroundProceedsNormally() throws Throwable {
        when(redissonDataHandle.get(FLAG_NAME)).thenReturn(null);
        when(localLockCache.getLock(LOCK_NAME, true)).thenReturn(localLock);
        when(serviceLockFactory.getLock(LockType.Fair)).thenReturn(serviceLocker);
        when(serviceLocker.tryLock(LOCK_NAME, TimeUnit.SECONDS, 0)).thenReturn(true);
        Object result = new Object();
        when(joinPoint.proceed()).thenReturn(result);

        Object actual = aspect.around(joinPoint, repeatLimit);

        assertSame(result, actual);
        verify(redissonDataHandle).set(FLAG_NAME, SUCCESS_FLAG, 60L, TimeUnit.SECONDS);
        verify(serviceLocker).unlock(LOCK_NAME);
        assertFalse(localLock.isLocked());
    }

    @Test
    @DisplayName("durationTime不大于0时不写入成功标记")
    void aroundDoesNotSetFlagWhenDurationNotPositive() throws Throwable {
        when(repeatLimit.durationTime()).thenReturn(0L);
        when(redissonDataHandle.get(FLAG_NAME)).thenReturn(null);
        when(localLockCache.getLock(LOCK_NAME, true)).thenReturn(localLock);
        when(serviceLockFactory.getLock(LockType.Fair)).thenReturn(serviceLocker);
        when(serviceLocker.tryLock(LOCK_NAME, TimeUnit.SECONDS, 0)).thenReturn(true);
        when(joinPoint.proceed()).thenReturn("ok");

        Object actual = aspect.around(joinPoint, repeatLimit);

        assertEquals("ok", actual);
        verify(redissonDataHandle, never()).set(anyString(), anyString(), eq(0L), any());
    }
}
