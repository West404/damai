package com.baidu.fsg.uid.buffer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BufferPaddingExecutor 缓冲区填充执行器单元测试
 */
class BufferPaddingExecutorTest {

    private BufferPaddingExecutor executor;

    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("paddingBuffer：应循环调用UID提供者直至填满RingBuffer")
    void testPaddingBufferFillRingBuffer() {
        RingBuffer ringBuffer = new RingBuffer(8, 50);
        // 每秒提供4个UID
        BufferedUidProvider provider = second -> Arrays.asList(second * 10, second * 10 + 1, second * 10 + 2, second * 10 + 3);
        executor = new BufferPaddingExecutor(ringBuffer, provider, false);

        executor.paddingBuffer();

        // 填满8个槽位，tail指向7
        assertEquals(7L, ringBuffer.getTail());
        assertFalse(executor.isRunning(), "填充结束后running标志应复位");
    }

    @Test
    @DisplayName("paddingBuffer：已在运行中时应直接返回，不重复填充")
    void testPaddingBufferWhenAlreadyRunning() {
        RingBuffer ringBuffer = new RingBuffer(8, 50);
        AtomicInteger providerCallCount = new AtomicInteger(0);
        BufferedUidProvider provider = second -> {
            providerCallCount.incrementAndGet();
            return List.of(1L);
        };
        executor = new BufferPaddingExecutor(ringBuffer, provider, false);

        // 通过反射将running标志置为true，模拟正在填充中
        AtomicBoolean running = (AtomicBoolean) ReflectionTestUtils.getField(executor, "running");
        running.set(true);

        executor.paddingBuffer();

        assertEquals(0, providerCallCount.get(), "运行中的填充任务不应再次调用UID提供者");
        assertEquals(-1L, ringBuffer.getTail(), "RingBuffer不应被填充");
    }

    @Test
    @DisplayName("asyncPadding：异步填充最终填满RingBuffer")
    void testAsyncPadding() throws InterruptedException {
        RingBuffer ringBuffer = new RingBuffer(8, 50);
        BufferedUidProvider provider = second -> Arrays.asList(second * 10, second * 10 + 1, second * 10 + 2, second * 10 + 3);
        executor = new BufferPaddingExecutor(ringBuffer, provider, false);

        executor.asyncPadding();

        // 轮询等待异步填充完成
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5);
        while (ringBuffer.getTail() < 7 && System.currentTimeMillis() < deadline) {
            Thread.sleep(10L);
        }
        assertEquals(7L, ringBuffer.getTail(), "异步填充应在超时前填满RingBuffer");
    }

    @Test
    @DisplayName("paddingBuffer：lastSecond随每次provide调用递增")
    void testLastSecondIncrement() {
        RingBuffer ringBuffer = new RingBuffer(8, 50);
        // 每次只提供1个UID，填满8个槽需要调用8次
        BufferedUidProvider provider = List::of;
        executor = new BufferPaddingExecutor(ringBuffer, provider, false);

        AtomicLong lastSecondField = (AtomicLong) ReflectionTestUtils.getField(executor, "lastSecond");
        long before = lastSecondField.get();
        executor.paddingBuffer();
        long after = lastSecondField.get();

        // 填满8个槽需8次成功put，循环还会再多调一次provider（该次put因缓冲满被拒）才退出
        assertEquals(before + 9, after, "每提供一个UID批次lastSecond应递增1");
    }

    @Test
    @DisplayName("setScheduleInterval：非正数应抛出IllegalArgumentException")
    void testSetInvalidScheduleInterval() {
        RingBuffer ringBuffer = new RingBuffer(8, 50);
        executor = new BufferPaddingExecutor(ringBuffer, second -> List.of(1L), false);

        assertThrows(IllegalArgumentException.class, () -> executor.setScheduleInterval(0));
        assertThrows(IllegalArgumentException.class, () -> executor.setScheduleInterval(-1));
    }

    @Test
    @DisplayName("shutdown：可安全关闭且可重复调用")
    void testShutdown() {
        RingBuffer ringBuffer = new RingBuffer(8, 50);
        executor = new BufferPaddingExecutor(ringBuffer, second -> List.of(1L), true);

        executor.shutdown();
        executor.shutdown();
        // 无异常即通过
        assertTrue(true);
    }
}
