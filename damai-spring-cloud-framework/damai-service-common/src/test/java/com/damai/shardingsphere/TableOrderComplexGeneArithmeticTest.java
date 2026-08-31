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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 订单分表算法 {@link TableOrderComplexGeneArithmetic} 单元测试
 */
class TableOrderComplexGeneArithmeticTest {

    private static final int SHARDING_COUNT = 4;

    private static final String LOGIC_TABLE_NAME = "t_order";

    private static final List<String> ALL_TABLE_NAMES =
            Arrays.asList("t_order_0", "t_order_1", "t_order_2", "t_order_3");

    private TableOrderComplexGeneArithmetic arithmetic;

    @BeforeEach
    void setUp() {
        arithmetic = new TableOrderComplexGeneArithmetic();
        Properties props = new Properties();
        props.setProperty("sharding-count", String.valueOf(SHARDING_COUNT));
        arithmetic.init(props);
    }

    private ComplexKeysShardingValue<Long> buildShardingValue(Map<String, Collection<Long>> columnValueMap) {
        return new ComplexKeysShardingValue<>(LOGIC_TABLE_NAME, columnValueMap, new HashMap<>());
    }

    @Test
    @DisplayName("按 order_number 分表：表名后缀为 (表数量-1) & 分片键")
    void doShardingByOrderNumber() {
        long orderNumber = 123456789L;
        Map<String, Collection<Long>> columnValueMap = new HashMap<>();
        columnValueMap.put("order_number", Collections.singletonList(orderNumber));

        Collection<String> result = arithmetic.doSharding(ALL_TABLE_NAMES, buildShardingValue(columnValueMap));

        long expectedSuffix = (SHARDING_COUNT - 1) & orderNumber;
        assertEquals(1, result.size());
        assertEquals(LOGIC_TABLE_NAME + "_" + expectedSuffix, result.iterator().next());
        assertTrue(ALL_TABLE_NAMES.contains(result.iterator().next()));
    }

    @Test
    @DisplayName("按 user_id 分表：没有 order_number 时应使用 user_id 计算表下标")
    void doShardingByUserId() {
        long userId = 987654322L;
        Map<String, Collection<Long>> columnValueMap = new HashMap<>();
        columnValueMap.put("user_id", Collections.singletonList(userId));

        Collection<String> result = arithmetic.doSharding(ALL_TABLE_NAMES, buildShardingValue(columnValueMap));

        long expectedSuffix = (SHARDING_COUNT - 1) & userId;
        assertEquals(1, result.size());
        assertEquals(LOGIC_TABLE_NAME + "_" + expectedSuffix, result.iterator().next());
    }

    @Test
    @DisplayName("order_number 与 user_id 同时存在时优先使用 order_number")
    void orderNumberTakesPrecedenceOverUserId() {
        long orderNumber = 5L;
        long userId = 10L;
        Map<String, Collection<Long>> columnValueMap = new HashMap<>();
        columnValueMap.put("order_number", Collections.singletonList(orderNumber));
        columnValueMap.put("user_id", Collections.singletonList(userId));

        Collection<String> result = arithmetic.doSharding(ALL_TABLE_NAMES, buildShardingValue(columnValueMap));

        long expectedSuffix = (SHARDING_COUNT - 1) & orderNumber;
        assertEquals(1, result.size());
        assertEquals(LOGIC_TABLE_NAME + "_" + expectedSuffix, result.iterator().next());
    }

    @Test
    @DisplayName("分片键为空集合时返回全部表名")
    void doShardingWithEmptyMapReturnsAll() {
        Collection<String> result = arithmetic.doSharding(ALL_TABLE_NAMES, buildShardingValue(new HashMap<>()));

        assertEquals(ALL_TABLE_NAMES, result);
    }

    @Test
    @DisplayName("只包含无关列时返回全部表名")
    void doShardingWithUnrelatedColumnReturnsAll() {
        Map<String, Collection<Long>> columnValueMap = new HashMap<>();
        columnValueMap.put("program_id", Collections.singletonList(1L));

        Collection<String> result = arithmetic.doSharding(ALL_TABLE_NAMES, buildShardingValue(columnValueMap));

        assertEquals(ALL_TABLE_NAMES, result);
    }

    @Test
    @DisplayName("分表下标永远不超出实际表数量")
    void tableIndexNeverExceedsActualTables() {
        for (long key = 0L; key < 100L; key++) {
            Map<String, Collection<Long>> columnValueMap = new HashMap<>();
            columnValueMap.put("order_number", Collections.singletonList(key));
            Collection<String> result = arithmetic.doSharding(ALL_TABLE_NAMES, buildShardingValue(columnValueMap));
            assertEquals(1, result.size());
            assertTrue(ALL_TABLE_NAMES.contains(result.iterator().next()),
                    "路由结果必须是真实存在的表: " + result);
        }
    }
}
