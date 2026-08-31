package com.damai.redis;

import com.damai.core.RedisKeyManage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * @description: RedisKeyBuild key包装测试
 * @author: 阿星不是程序员
 **/
@DisplayName("RedisKeyBuild redis key包装测试")
class RedisKeyBuildTest {

    @BeforeAll
    static void setUp() {
        // 初始化 mock 的 ApplicationContext，让 key 前缀固定为 test
        SpringUtilTestSupport.initPrefixDistinctionName();
    }

    @Test
    @DisplayName("带一个占位符的key应被正确格式化并加上前缀")
    void createRedisKeyWithSingleArg() {
        RedisKeyBuild redisKeyBuild = RedisKeyBuild.createRedisKey(RedisKeyManage.PRODUCT_STOCK, 1001L);
        assertEquals("test-product_stock:1001", redisKeyBuild.getRelKey());
    }

    @Test
    @DisplayName("带两个占位符的key应被正确格式化并加上前缀")
    void createRedisKeyWithTwoArgs() {
        RedisKeyBuild redisKeyBuild = RedisKeyBuild.createRedisKey(RedisKeyManage.USER_LOGIN, "18888888888", "android");
        assertEquals("test-user_login_18888888888_android", redisKeyBuild.getRelKey());
    }

    @Test
    @DisplayName("无占位符的key直接拼接前缀")
    void createRedisKeyWithoutArg() {
        RedisKeyBuild redisKeyBuild = RedisKeyBuild.createRedisKey(RedisKeyManage.ALL_RULE_HASH);
        assertEquals("test-all_rule_hash", redisKeyBuild.getRelKey());
    }

    @Test
    @DisplayName("getRedisKey返回带前缀的原始key模板")
    void getRedisKey() {
        String key = RedisKeyBuild.getRedisKey(RedisKeyManage.PRODUCT_STOCK);
        assertEquals("test-product_stock:%s", key);
    }

    @Test
    @DisplayName("相同参数的key相等且hashCode一致")
    void equalsAndHashCode() {
        RedisKeyBuild key1 = RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM, 1L);
        RedisKeyBuild key2 = RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM, 1L);
        RedisKeyBuild key3 = RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM, 2L);

        assertEquals(key1, key1);
        assertEquals(key1, key2);
        assertEquals(key1.hashCode(), key2.hashCode());
        assertNotEquals(key1, key3);
        assertNotEquals(key1, null);
        assertNotEquals(key1, "test-d_mai_program_1");
    }
}
