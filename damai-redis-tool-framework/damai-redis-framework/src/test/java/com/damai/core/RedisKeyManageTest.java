package com.damai.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @description: RedisKeyManage 枚举测试
 * @author: 阿星不是程序员
 **/
@DisplayName("RedisKeyManage redis key管理枚举测试")
class RedisKeyManageTest {

    @Test
    @DisplayName("根据key值能查到对应的枚举")
    void getRcHit() {
        RedisKeyManage result = RedisKeyManage.getRc("product_stock:%s");
        assertEquals(RedisKeyManage.PRODUCT_STOCK, result);
    }

    @Test
    @DisplayName("key值不存在时getRc返回null")
    void getRcMiss() {
        assertNull(RedisKeyManage.getRc("not_exist_key"));
    }

    @Test
    @DisplayName("枚举的key、说明、作者字段与定义一致")
    void enumFields() {
        assertEquals("user_login_%s_%s", RedisKeyManage.USER_LOGIN.getKey());
        assertEquals("user_login", RedisKeyManage.USER_LOGIN.getKeyIntroduce());
        assertEquals("value为UserVo类型", RedisKeyManage.USER_LOGIN.getValueIntroduce());
        assertEquals("k", RedisKeyManage.USER_LOGIN.getAuthor());

        assertEquals("distributed_datacenter_id:%s", RedisKeyManage.DISTRIBUTED_DATACENTER_ID.getKey());
        assertEquals("lk", RedisKeyManage.DISTRIBUTED_DATACENTER_ID.getAuthor());
    }

    @Test
    @DisplayName("枚举的key、keyIntroduce、valueIntroduce均不为空")
    void allEnumFieldsNotBlank() {
        for (RedisKeyManage redisKeyManage : RedisKeyManage.values()) {
            assertNotNull(redisKeyManage.getKey(), redisKeyManage.name() + " 的 key 不应为空");
            assertTrue(!redisKeyManage.getKey().isEmpty(), redisKeyManage.name() + " 的 key 不应为空字符串");
            assertNotNull(redisKeyManage.getKeyIntroduce(), redisKeyManage.name() + " 的 keyIntroduce 不应为空");
            assertNotNull(redisKeyManage.getValueIntroduce(), redisKeyManage.name() + " 的 valueIntroduce 不应为空");
        }
    }

    @Test
    @DisplayName("每个枚举都能通过getRc被反查回来，保证key不重复")
    void everyEnumCanBeFoundByKey() {
        for (RedisKeyManage redisKeyManage : RedisKeyManage.values()) {
            assertEquals(redisKeyManage, RedisKeyManage.getRc(redisKeyManage.getKey()),
                    redisKeyManage.name() + " 的 key 存在重复");
        }
    }
}
