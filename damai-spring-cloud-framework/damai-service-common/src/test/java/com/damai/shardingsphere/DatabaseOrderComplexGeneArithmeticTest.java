package com.damai.shardingsphere;

import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 订单分库算法 {@link DatabaseOrderComplexGeneArithmetic} 单元测试
 */
class DatabaseOrderComplexGeneArithmeticTest {

    private static final int SHARDING_COUNT = 2;

    private static final int TABLE_SHARDING_COUNT = 4;

    private static final List<String> ALL_DATABASE_NAMES = Arrays.asList("ds_0", "ds_1");

    private DatabaseOrderComplexGeneArithmetic arithmetic;

    @BeforeEach
    void setUp() {
        arithmetic = new DatabaseOrderComplexGeneArithmetic();
        Properties props = new Properties();
        props.setProperty("sharding-count", String.valueOf(SHARDING_COUNT));
        props.setProperty("table-sharding-count", String.valueOf(TABLE_SHARDING_COUNT));
        arithmetic.init(props);
    }

    private ComplexKeysShardingValue<Long> buildShardingValue(Map<String, Collection<Long>> columnValueMap) {
        return new ComplexKeysShardingValue<>("t_order", columnValueMap, new HashMap<>());
    }

    @Test
    @DisplayName("按 order_number 分片：应路由到与计算出的库下标匹配的库名")
    void doShardingByOrderNumber() {
        long orderNumber = 123456789L;
        Map<String, Collection<Long>> columnValueMap = new HashMap<>();
        columnValueMap.put("order_number", Collections.singletonList(orderNumber));

        Collection<String> result = arithmetic.doSharding(ALL_DATABASE_NAMES, buildShardingValue(columnValueMap));

        long expectedIndex = arithmetic.calculateDatabaseIndex(SHARDING_COUNT, orderNumber, TABLE_SHARDING_COUNT);
        assertEquals(1, result.size());
        assertEquals("ds_" + expectedIndex, result.iterator().next());
    }

    @Test
    @DisplayName("按 user_id 分片：没有 order_number 时应使用 user_id 计算库下标")
    void doShardingByUserId() {
        long userId = 987654321L;
        Map<String, Collection<Long>> columnValueMap = new HashMap<>();
        columnValueMap.put("user_id", Collections.singletonList(userId));

        Collection<String> result = arithmetic.doSharding(ALL_DATABASE_NAMES, buildShardingValue(columnValueMap));

        long expectedIndex = arithmetic.calculateDatabaseIndex(SHARDING_COUNT, userId, TABLE_SHARDING_COUNT);
        assertEquals(1, result.size());
        assertEquals("ds_" + expectedIndex, result.iterator().next());
    }

    @Test
    @DisplayName("order_number 与 user_id 同时存在时优先使用 order_number")
    void orderNumberTakesPrecedenceOverUserId() {
        long orderNumber = 100L;
        long userId = 999999L;
        Map<String, Collection<Long>> columnValueMap = new HashMap<>();
        columnValueMap.put("order_number", Collections.singletonList(orderNumber));
        columnValueMap.put("user_id", Collections.singletonList(userId));

        Collection<String> result = arithmetic.doSharding(ALL_DATABASE_NAMES, buildShardingValue(columnValueMap));

        long expectedIndex = arithmetic.calculateDatabaseIndex(SHARDING_COUNT, orderNumber, TABLE_SHARDING_COUNT);
        assertEquals(1, result.size());
        assertEquals("ds_" + expectedIndex, result.iterator().next());
    }

    @Test
    @DisplayName("分片键为空集合时返回全部库名")
    void doShardingWithEmptyMapReturnsAll() {
        Collection<String> result = arithmetic.doSharding(ALL_DATABASE_NAMES, buildShardingValue(new HashMap<>()));

        assertEquals(ALL_DATABASE_NAMES, result);
    }

    @Test
    @DisplayName("只包含无关列时返回全部库名")
    void doShardingWithUnrelatedColumnReturnsAll() {
        Map<String, Collection<Long>> columnValueMap = new HashMap<>();
        columnValueMap.put("program_id", Collections.singletonList(1L));

        Collection<String> result = arithmetic.doSharding(ALL_DATABASE_NAMES, buildShardingValue(columnValueMap));

        assertEquals(ALL_DATABASE_NAMES, result);
    }

    @Test
    @DisplayName("calculateDatabaseIndex：结果必须落在 [0, 库数量) 区间内（键的二进制长度需不小于 log2(表数量)，故从 4 开始）")
    void calculateDatabaseIndexInRange() {
        for (long key = 4L; key <= 1000L; key++) {
            long index = arithmetic.calculateDatabaseIndex(SHARDING_COUNT, key, TABLE_SHARDING_COUNT);
            assertTrue(index >= 0 && index < SHARDING_COUNT, "库下标越界: " + index);
        }
    }

    @Test
    @DisplayName("calculateDatabaseIndex：相同的键多次计算结果一致（确定性）")
    void calculateDatabaseIndexDeterministic() {
        long first = arithmetic.calculateDatabaseIndex(SHARDING_COUNT, 123456789L, TABLE_SHARDING_COUNT);
        long second = arithmetic.calculateDatabaseIndex(SHARDING_COUNT, 123456789L, TABLE_SHARDING_COUNT);
        assertEquals(first, second);
    }

    @Test
    @DisplayName("calculateDatabaseIndex：不同的键应能散列到多个库（分布性验证）")
    void calculateDatabaseIndexDistribution() {
        Collection<Long> indexes = new TreeSet<>();
        for (long key = 4L; key <= 100L; key++) {
            indexes.add(arithmetic.calculateDatabaseIndex(SHARDING_COUNT, key, TABLE_SHARDING_COUNT));
        }
        assertEquals(SHARDING_COUNT, indexes.size(), "100 个键应散列到全部 " + SHARDING_COUNT + " 个库");
    }

    @Test
    @DisplayName("log2N：计算以 2 为底的对数")
    void log2N() {
        assertEquals(0L, arithmetic.log2N(1));
        assertEquals(2L, arithmetic.log2N(4));
        assertEquals(3L, arithmetic.log2N(8));
    }
}
