package com.baidu.fsg.uid.buffer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * RingBuffer 环形缓冲区单元测试
 */
@ExtendWith(MockitoExtension.class)
class RingBufferTest {

    @Mock
    private BufferPaddingExecutor bufferPaddingExecutor;

    private RingBuffer ringBuffer;

    @BeforeEach
    void setUp() {
        // bufferSize=8，paddingFactor=50 -> 填充阈值为4
        ringBuffer = new RingBuffer(8, 50);
        ringBuffer.setBufferPaddingExecutor(bufferPaddingExecutor);
    }

    @Test
    @DisplayName("构造器：bufferSize非2的幂时应抛出IllegalArgumentException")
    void testConstructorWithNonPowerOfTwo() {
        assertThrows(IllegalArgumentException.class, () -> new RingBuffer(7));
    }

    @Test
    @DisplayName("构造器：paddingFactor不在(0,100)区间时应抛出IllegalArgumentException")
    void testConstructorWithInvalidPaddingFactor() {
        assertThrows(IllegalArgumentException.class, () -> new RingBuffer(8, 0));
        assertThrows(IllegalArgumentException.class, () -> new RingBuffer(8, 100));
    }

    @Test
    @DisplayName("put/take：按放入顺序先进先出取出UID")
    void testPutAndTakeInFifoOrder() {
        assertTrue(ringBuffer.put(100L));
        assertTrue(ringBuffer.put(200L));
        assertTrue(ringBuffer.put(300L));

        assertEquals(100L, ringBuffer.take());
        assertEquals(200L, ringBuffer.take());
        assertEquals(300L, ringBuffer.take());

        // tail与cursor同步前进
        assertEquals(2L, ringBuffer.getTail());
        assertEquals(2L, ringBuffer.getCursor());
    }

    @Test
    @DisplayName("put：缓冲区满时应拒绝放入并返回false")
    void testPutRejectedWhenBufferFull() {
        AtomicInteger rejectCount = new AtomicInteger(0);
        ringBuffer.setRejectedPutHandler((buffer, uid) -> rejectCount.incrementAndGet());

        // bufferSize=8，最多可容纳8个UID
        for (int i = 0; i < 8; i++) {
            assertTrue(ringBuffer.put(i), "第" + i + "个UID应放入成功");
        }

        // 缓冲区已满，第9个放入被拒绝
        assertFalse(ringBuffer.put(999L));
        assertEquals(1, rejectCount.get(), "拒绝处理器应被调用一次");
        assertEquals(7L, ringBuffer.getTail(), "拒绝后tail不应移动");
    }

    @Test
    @DisplayName("take：缓冲区为空时默认策略抛出RuntimeException")
    void testTakeRejectedWhenBufferEmpty() {
        assertThrows(RuntimeException.class, () -> ringBuffer.take());
    }

    @Test
    @DisplayName("take：缓冲区为空时触发自定义拒绝策略，策略中补充的UID可被取出")
    void testTakeWithCustomRejectedHandler() {
        AtomicInteger rejectCount = new AtomicInteger(0);
        // 自定义策略：不抛异常，仅计数并填满缓冲区
        ringBuffer.setRejectedTakeHandler(buffer -> {
            rejectCount.incrementAndGet();
            for (int i = 0; i < 8; i++) {
                buffer.put(1000L + i);
            }
        });

        // 空缓冲区take触发拒绝策略；策略填满后，take继续读取到刚放入的UID
        long uid = ringBuffer.take();
        assertEquals(1, rejectCount.get());
        assertEquals(1007L, uid);
    }

    @Test
    @DisplayName("take：剩余UID低于填充阈值时应触发异步填充")
    void testTakeTriggerAsyncPadding() {
        // 填满8个UID
        for (int i = 0; i < 8; i++) {
            ringBuffer.put(i);
        }

        // 阈值为4，取第5个时 rest = 7 - 4 = 3 < 4，应触发异步填充
        for (int i = 0; i < 8; i++) {
            ringBuffer.take();
        }

        verify(bufferPaddingExecutor, atLeastOnce()).asyncPadding();
    }

    @Test
    @DisplayName("calSlotIndex：槽位下标等于序列号对bufferSize取模")
    void testCalSlotIndex() {
        assertEquals(0, ringBuffer.calSlotIndex(0));
        assertEquals(7, ringBuffer.calSlotIndex(7));
        assertEquals(0, ringBuffer.calSlotIndex(8));
        assertEquals(3, ringBuffer.calSlotIndex(11));
    }

    @Test
    @DisplayName("环绕：take完后可再次put，槽位循环复用")
    void testRingWrapAround() {
        // 填满再全部取空
        for (int i = 0; i < 8; i++) {
            ringBuffer.put(i);
        }
        for (int i = 0; i < 8; i++) {
            assertEquals(i, ringBuffer.take());
        }

        // 环绕一圈后仍可正常放入/取出
        assertTrue(ringBuffer.put(888L));
        assertEquals(888L, ringBuffer.take());
    }

    @Test
    @DisplayName("getBufferSize/toString：基本属性正确")
    void testBasicProperties() {
        assertEquals(8, ringBuffer.getBufferSize());
        assertEquals(-1L, ringBuffer.getTail());
        assertEquals(-1L, ringBuffer.getCursor());
        assertTrue(ringBuffer.toString().contains("bufferSize=8"));
    }
}
