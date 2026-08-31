package com.damai.redis;

import com.damai.core.RedisKeyManage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @description: CacheUtil 缓存工具测试
 * @author: 阿星不是程序员
 **/
@DisplayName("CacheUtil 缓存对象映射工具测试")
class CacheUtilTest {

    @BeforeAll
    static void setUp() {
        // getBatchKey 依赖 RedisKeyBuild，需要先初始化 key 前缀
        SpringUtilTestSupport.initPrefixDistinctionName();
    }

    @Test
    @DisplayName("默认过期时间单位为秒")
    void defaultTimeUnit() {
        assertEquals(TimeUnit.SECONDS, CacheUtil.DEFAULT_TIME_UNIT);
    }

    @Test
    @DisplayName("buildType传入null或空数组时返回null")
    void buildTypeEmpty() {
        assertNull(CacheUtil.buildType());
        assertNull(CacheUtil.buildType((Type[]) null));
    }

    @Test
    @DisplayName("buildType单个类型时rawType为该类型")
    void buildTypeSingle() {
        Type type = CacheUtil.buildType(String.class);
        assertTrue(type instanceof ParameterizedType);
        assertEquals(String.class, ((ParameterizedType) type).getRawType());
    }

    @Test
    @DisplayName("buildType多个类型时按嵌套泛型组装，如List<String>")
    void buildTypeNested() {
        Type type = CacheUtil.buildType(List.class, String.class);
        assertTrue(type instanceof ParameterizedType);
        ParameterizedType parameterizedType = (ParameterizedType) type;
        assertEquals(List.class, parameterizedType.getRawType());
        assertEquals(String.class, parameterizedType.getActualTypeArguments()[0]);
    }

    @Test
    @DisplayName("checkNotBlank传入正常字符串不抛异常")
    void checkNotBlankPass() {
        assertDoesNotThrow(() -> CacheUtil.checkNotBlank("a", "b"));
    }

    @Test
    @DisplayName("checkNotBlank传入null、空串、空白串时抛出运行时异常")
    void checkNotBlankFail() {
        assertThrows(RuntimeException.class, () -> CacheUtil.checkNotBlank("a", null));
        assertThrows(RuntimeException.class, () -> CacheUtil.checkNotBlank(""));
        assertThrows(RuntimeException.class, () -> CacheUtil.checkNotBlank("   "));
    }

    @Test
    @DisplayName("checkNotBlank传入正常RedisKeyBuild不抛异常")
    void checkNotBlankRedisKeyBuildPass() {
        RedisKeyBuild redisKeyBuild = RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM, 1L);
        assertDoesNotThrow(() -> CacheUtil.checkNotBlank(redisKeyBuild));
    }

    @Test
    @DisplayName("checkNotBlank传入含空元素的字符串集合时抛出异常")
    void checkNotBlankCollectionFail() {
        assertDoesNotThrow(() -> CacheUtil.checkNotBlank(Arrays.asList("a", "b")));
        assertThrows(RuntimeException.class, () -> CacheUtil.checkNotBlank(Arrays.asList("a", "")));
    }

    @Test
    @DisplayName("checkNotEmpty传入含null元素的集合时抛出异常")
    void checkNotEmptyCollectionFail() {
        assertDoesNotThrow(() -> CacheUtil.checkNotEmpty(Arrays.asList("a", 1)));
        assertThrows(RuntimeException.class, () -> CacheUtil.checkNotEmpty(Arrays.asList("a", null)));
    }

    @Test
    @DisplayName("checkNotEmpty传入null、空字符串、空集合时抛出异常")
    void checkNotEmptyObjectFail() {
        assertThrows(RuntimeException.class, () -> CacheUtil.checkNotEmpty((Object) null));
        assertThrows(RuntimeException.class, () -> CacheUtil.checkNotEmpty(""));
        assertThrows(RuntimeException.class, () -> CacheUtil.checkNotEmpty((Object) new ArrayList<>()));
        assertDoesNotThrow(() -> CacheUtil.checkNotEmpty("a"));
    }

    @Test
    @DisplayName("isEmpty正确判断null、空字符串、空集合")
    void isEmpty() {
        assertTrue(CacheUtil.isEmpty(null));
        assertTrue(CacheUtil.isEmpty(""));
        assertTrue(CacheUtil.isEmpty("  "));
        assertTrue(CacheUtil.isEmpty(new ArrayList<>()));
        assertFalse(CacheUtil.isEmpty("a"));
        assertFalse(CacheUtil.isEmpty(Collections.singletonList("a")));
        assertFalse(CacheUtil.isEmpty(1));
    }

    @Test
    @DisplayName("getBatchKey将RedisKeyBuild集合转换为真实key集合")
    void getBatchKey() {
        List<RedisKeyBuild> keyBuilds = Arrays.asList(
                RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM, 1L),
                RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM, 2L));
        List<String> batchKey = CacheUtil.getBatchKey(keyBuilds);
        assertEquals(Arrays.asList("test-d_mai_program_1", "test-d_mai_program_2"), batchKey);
    }

    @Test
    @DisplayName("optimizeRedisList对null、空集合、首元素为null的集合返回空集合，否则原样返回")
    void optimizeRedisList() {
        assertTrue(CacheUtil.optimizeRedisList(null).isEmpty());
        assertTrue(CacheUtil.optimizeRedisList(new ArrayList<>()).isEmpty());
        assertTrue(CacheUtil.optimizeRedisList(Collections.singletonList(null)).isEmpty());

        List<String> normal = Arrays.asList("a", "b");
        assertSame(normal, CacheUtil.optimizeRedisList(normal));
    }

    @Test
    @DisplayName("checkRedisListIsEmpty正确判断null、空集合、首元素为null的集合")
    void checkRedisListIsEmpty() {
        assertTrue(CacheUtil.checkRedisListIsEmpty(null));
        assertTrue(CacheUtil.checkRedisListIsEmpty(new ArrayList<>()));
        assertTrue(CacheUtil.checkRedisListIsEmpty(Collections.singletonList(null)));
        assertFalse(CacheUtil.checkRedisListIsEmpty(Collections.singletonList("a")));
    }
}
