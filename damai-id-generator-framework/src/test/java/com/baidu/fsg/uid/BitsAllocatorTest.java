package com.baidu.fsg.uid;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BitsAllocator 位分配器单元测试
 */
class BitsAllocatorTest {

    @Test
    @DisplayName("构造器：总位数不等于64时应抛出IllegalArgumentException")
    void testConstructorWithInvalidTotalBits() {
        // 1(符号位) + 28 + 22 + 12 = 63，不足64位
        assertThrows(IllegalArgumentException.class, () -> new BitsAllocator(28, 22, 12));
    }

    @Test
    @DisplayName("构造器：合法的位数分配可以正常初始化并计算最大值与位移")
    void testConstructorWithValidBits() {
        BitsAllocator allocator = new BitsAllocator(28, 22, 13);

        assertEquals(1, allocator.getSignBits());
        assertEquals(28, allocator.getTimestampBits());
        assertEquals(22, allocator.getWorkerIdBits());
        assertEquals(13, allocator.getSequenceBits());

        // 最大值 = 2^bits - 1
        assertEquals((1L << 28) - 1, allocator.getMaxDeltaSeconds());
        assertEquals((1L << 22) - 1, allocator.getMaxWorkerId());
        assertEquals((1L << 13) - 1, allocator.getMaxSequence());

        // 位移量
        assertEquals(22 + 13, allocator.getTimestampShift());
        assertEquals(13, allocator.getWorkerIdShift());
    }

    @Test
    @DisplayName("allocate：deltaSeconds/workerId/sequence 应按位正确拼接成UID")
    void testAllocate() {
        BitsAllocator allocator = new BitsAllocator(28, 22, 13);

        long uid = allocator.allocate(1L, 2L, 3L);

        // 按位还原各段，验证拼接正确
        long sequence = uid & allocator.getMaxSequence();
        long workerId = (uid >>> allocator.getWorkerIdShift()) & allocator.getMaxWorkerId();
        long deltaSeconds = uid >>> allocator.getTimestampShift();

        assertEquals(3L, sequence);
        assertEquals(2L, workerId);
        assertEquals(1L, deltaSeconds);
    }

    @Test
    @DisplayName("allocate：各段全为0时UID为0，最高符号位始终为0")
    void testAllocateZeroAndSignBit() {
        BitsAllocator allocator = new BitsAllocator(28, 22, 13);

        assertEquals(0L, allocator.allocate(0L, 0L, 0L));

        // 各段都取最大值，符号位仍应为0（UID为正数）
        long uid = allocator.allocate(allocator.getMaxDeltaSeconds(),
                allocator.getMaxWorkerId(), allocator.getMaxSequence());
        assertTrue(uid > 0, "最高符号位应始终为0，UID应为正数");
        assertEquals(Long.MAX_VALUE, uid);
    }

    @Test
    @DisplayName("toString：应包含位分配信息")
    void testToString() {
        BitsAllocator allocator = new BitsAllocator(28, 22, 13);
        String str = allocator.toString();
        assertTrue(str.contains("timestampBits=28"));
        assertTrue(str.contains("workerIdBits=22"));
        assertTrue(str.contains("sequenceBits=13"));
    }
}
