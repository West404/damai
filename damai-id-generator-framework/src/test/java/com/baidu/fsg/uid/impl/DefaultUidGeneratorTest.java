package com.baidu.fsg.uid.impl;

import com.baidu.fsg.uid.exception.UidGenerateException;
import com.baidu.fsg.uid.worker.WorkerIdAssigner;
import com.damai.toolkit.SnowflakeIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DefaultUidGenerator 默认UID生成器单元测试（WorkerIdAssigner/SnowflakeIdGenerator 使用Mockito mock）
 */
@ExtendWith(MockitoExtension.class)
class DefaultUidGeneratorTest {

    @Mock
    private WorkerIdAssigner workerIdAssigner;

    @Mock
    private SnowflakeIdGenerator snowflakeIdGenerator;

    private DefaultUidGenerator uidGenerator;

    @BeforeEach
    void setUp() {
        uidGenerator = new DefaultUidGenerator();
        uidGenerator.setWorkerIdAssigner(workerIdAssigner);
        uidGenerator.setSnowflakeIdGenerator(snowflakeIdGenerator);
    }

    @Test
    @DisplayName("afterPropertiesSet：workerId超过最大值时应抛出RuntimeException")
    void testInitWithWorkerIdExceedsMax() {
        // 默认workerBits=22，最大workerId为 2^22-1
        when(workerIdAssigner.assignWorkerId()).thenReturn(Long.MAX_VALUE);

        assertThrows(RuntimeException.class, () -> uidGenerator.afterPropertiesSet());
    }

    @Test
    @DisplayName("getUid：连续生成的UID应为正数且互不重复")
    void testGetUidUnique() throws Exception {
        when(workerIdAssigner.assignWorkerId()).thenReturn(1L);
        uidGenerator.afterPropertiesSet();

        Set<Long> uidSet = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            long uid = uidGenerator.getUid();
            assertTrue(uid > 0, "UID应为正数（符号位为0）");
            uidSet.add(uid);
        }
        assertEquals(1000, uidSet.size(), "生成的UID不应重复");
    }

    @Test
    @DisplayName("parseUid：应能正确解析出UID中的workerId与sequence")
    void testParseUid() throws Exception {
        when(workerIdAssigner.assignWorkerId()).thenReturn(7L);
        uidGenerator.afterPropertiesSet();

        long uid = uidGenerator.getUid();
        String parsed = uidGenerator.parseUid(uid);

        assertTrue(parsed.contains("\"UID\":\"" + uid + "\""), "解析结果应包含原始UID");
        assertTrue(parsed.contains("\"workerId\":\"7\""), "解析结果应包含workerId");
        assertTrue(parsed.contains("\"sequence\":\"0\""), "新一秒的首个UID序列号应为0");
        assertTrue(parsed.contains("timestamp"), "解析结果应包含时间戳");
    }

    @Test
    @DisplayName("getUid：时钟回拨时应抛出UidGenerateException")
    void testGetUidWhenClockMovedBackwards() throws Exception {
        when(workerIdAssigner.assignWorkerId()).thenReturn(1L);
        uidGenerator.afterPropertiesSet();

        // 通过反射将lastSecond设置为未来时间，模拟时钟回拨
        long futureSecond = System.currentTimeMillis() / 1000 + 100;
        ReflectionTestUtils.setField(uidGenerator, "lastSecond", futureSecond);

        UidGenerateException exception = assertThrows(UidGenerateException.class, () -> uidGenerator.getUid());
        assertTrue(exception.getCause().getMessage().contains("Clock moved backwards"),
                "异常信息应提示时钟回拨");
    }

    @Test
    @DisplayName("getId：应委托给SnowflakeIdGenerator生成")
    void testGetIdDelegatesToSnowflake() {
        when(snowflakeIdGenerator.nextId()).thenReturn(123456789L);

        assertEquals(123456789L, uidGenerator.getId());
        verify(snowflakeIdGenerator).nextId();
    }

    @Test
    @DisplayName("getOrderNumber：应委托给SnowflakeIdGenerator并透传参数")
    void testGetOrderNumberDelegatesToSnowflake() {
        when(snowflakeIdGenerator.getOrderNumber(99L, 8L)).thenReturn(8888L);

        assertEquals(8888L, uidGenerator.getOrderNumber(99L, 8L));
        verify(snowflakeIdGenerator).getOrderNumber(99L, 8L);
    }

    @Test
    @DisplayName("同一秒内序列号应自增，不同UID的序列号不同")
    void testSequenceIncreaseWithinSameSecond() throws Exception {
        when(workerIdAssigner.assignWorkerId()).thenReturn(1L);
        uidGenerator.afterPropertiesSet();

        long uid1 = uidGenerator.getUid();
        long uid2 = uidGenerator.getUid();

        assertNotEquals(uid1, uid2);
        // 同一秒内后生成的UID更大（序列号递增）
        assertTrue(uid2 > uid1);
    }
}
