package com.baidu.fsg.uid.impl;

import com.baidu.fsg.uid.worker.WorkerIdAssigner;
import com.damai.toolkit.SnowflakeIdGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * CachedUidGenerator 缓存UID生成器单元测试（WorkerIdAssigner/SnowflakeIdGenerator 使用Mockito mock）
 */
@ExtendWith(MockitoExtension.class)
class CachedUidGeneratorTest {

    @Mock
    private WorkerIdAssigner workerIdAssigner;

    @Mock
    private SnowflakeIdGenerator snowflakeIdGenerator;

    private CachedUidGenerator cachedUidGenerator;

    @BeforeEach
    void setUp() {
        cachedUidGenerator = new CachedUidGenerator();
        // 缩小sequence位数（总位数仍需满足64），加快缓冲区初始化与填充
        cachedUidGenerator.setTimeBits(30);
        cachedUidGenerator.setWorkerBits(25);
        cachedUidGenerator.setSeqBits(8);
        cachedUidGenerator.setBoostPower(1);
        cachedUidGenerator.setWorkerIdAssigner(workerIdAssigner);
        cachedUidGenerator.setSnowflakeIdGenerator(snowflakeIdGenerator);
    }

    @AfterEach
    void tearDown() {
        if (cachedUidGenerator != null) {
            // 仅初始化过（afterPropertiesSet）才需要关闭填充线程池；
            // 纯 setter 校验类用例未初始化，destroy 会因 bufferPaddingExecutor 为空抛 NPE
            try {
                cachedUidGenerator.destroy();
            } catch (Exception ignored) {
                // 未初始化，跳过
            }
        }
    }

    @Test
    @DisplayName("getUid：初始化后可从RingBuffer中取出正数UID")
    void testGetUid() throws Exception {
        when(workerIdAssigner.assignWorkerId()).thenReturn(1L);
        cachedUidGenerator.afterPropertiesSet();

        long uid = cachedUidGenerator.getUid();
        assertTrue(uid > 0, "UID应为正数");
    }

    @Test
    @DisplayName("getUid：批量取出的UID应互不重复")
    void testGetUidUnique() throws Exception {
        when(workerIdAssigner.assignWorkerId()).thenReturn(1L);
        cachedUidGenerator.afterPropertiesSet();

        Set<Long> uidSet = ConcurrentHashMap.newKeySet();
        for (int i = 0; i < 500; i++) {
            uidSet.add(cachedUidGenerator.getUid());
        }
        assertEquals(500, uidSet.size(), "RingBuffer取出的UID不应重复");
    }

    @Test
    @DisplayName("parseUid：应能解析出RingBuffer产出UID的workerId")
    void testParseUid() throws Exception {
        when(workerIdAssigner.assignWorkerId()).thenReturn(3L);
        cachedUidGenerator.afterPropertiesSet();

        long uid = cachedUidGenerator.getUid();
        String parsed = cachedUidGenerator.parseUid(uid);

        assertTrue(parsed.contains("\"UID\":\"" + uid + "\""));
        assertTrue(parsed.contains("\"workerId\":\"3\""));
    }

    @Test
    @DisplayName("setBoostPower：非正数应抛出IllegalArgumentException")
    void testSetInvalidBoostPower() {
        assertThrows(IllegalArgumentException.class, () -> cachedUidGenerator.setBoostPower(0));
        assertThrows(IllegalArgumentException.class, () -> cachedUidGenerator.setBoostPower(-1));
    }

    @Test
    @DisplayName("setScheduleInterval：非正数应抛出IllegalArgumentException")
    void testSetInvalidScheduleInterval() {
        assertThrows(IllegalArgumentException.class, () -> cachedUidGenerator.setScheduleInterval(0));
    }

    @Test
    @DisplayName("setRejectedPutBufferHandler：null应抛出IllegalArgumentException")
    void testSetNullRejectedHandler() {
        assertThrows(IllegalArgumentException.class,
                () -> cachedUidGenerator.setRejectedPutBufferHandler(null));
        assertThrows(IllegalArgumentException.class,
                () -> cachedUidGenerator.setRejectedTakeBufferHandler(null));
    }
}
