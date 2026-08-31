package com.damai.redis;

import com.alibaba.fastjson.JSON;
import com.damai.core.RedisKeyManage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * @description: RedisCacheImpl redis方法实现测试，StringRedisTemplate 全部使用 Mockito mock，不连接真实redis
 * @author: 阿星不是程序员
 **/
@ExtendWith(MockitoExtension.class)
@DisplayName("RedisCacheImpl redis方法实现测试")
class RedisCacheImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private HashOperations<String, Object, Object> hashOperations;
    @Mock
    private ListOperations<String, String> listOperations;
    @Mock
    private SetOperations<String, String> setOperations;
    @Mock
    private ZSetOperations<String, String> zSetOperations;

    private RedisCacheImpl redisCache;

    private RedisKeyBuild redisKeyBuild;

    @BeforeAll
    static void initSpringUtil() {
        // 初始化 mock 的 ApplicationContext，让 key 前缀固定为 test
        SpringUtilTestSupport.initPrefixDistinctionName();
    }

    @BeforeEach
    void setUp() {
        redisCache = new RedisCacheImpl(redisTemplate);
        redisKeyBuild = RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM, 1L);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        lenient().when(redisTemplate.opsForList()).thenReturn(listOperations);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    @Test
    @DisplayName("get取String类型时直接返回缓存的字符串")
    void getStringValue() {
        when(valueOperations.get(redisKeyBuild.getRelKey())).thenReturn("hello");

        String result = redisCache.get(redisKeyBuild, String.class);

        assertEquals("hello", result);
    }

    @Test
    @DisplayName("get取对象类型时将JSON反序列化为对象")
    void getObjectValue() {
        TestPojo pojo = new TestPojo(1L, "大麦");
        when(valueOperations.get(redisKeyBuild.getRelKey())).thenReturn(JSON.toJSONString(pojo));

        TestPojo result = redisCache.get(redisKeyBuild, TestPojo.class);

        assertEquals(1L, result.getId());
        assertEquals("大麦", result.getName());
    }

    @Test
    @DisplayName("get缓存不存在时返回null")
    void getNullValue() {
        when(valueOperations.get(redisKeyBuild.getRelKey())).thenReturn(null);

        assertNull(redisCache.get(redisKeyBuild, TestPojo.class));
    }

    @Test
    @DisplayName("get带supplier时缓存命中则不回源、不写缓存")
    void getWithSupplierCacheHit() {
        when(valueOperations.get(redisKeyBuild.getRelKey())).thenReturn("cached");

        String result = redisCache.get(redisKeyBuild, String.class,
                () -> {
                    throw new IllegalStateException("缓存命中时不应调用supplier");
                }, 60, TimeUnit.SECONDS);

        assertEquals("cached", result);
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any());
    }

    @Test
    @DisplayName("get带supplier时缓存未命中则回源并写入缓存")
    void getWithSupplierCacheMiss() {
        when(valueOperations.get(redisKeyBuild.getRelKey())).thenReturn(null);

        String result = redisCache.get(redisKeyBuild, String.class,
                () -> "dbValue", 60, TimeUnit.SECONDS);

        assertEquals("dbValue", result);
        verify(valueOperations).set(redisKeyBuild.getRelKey(), "dbValue", 60L, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("get带supplier时回源也为空则返回null且不写缓存")
    void getWithSupplierEmptySource() {
        when(valueOperations.get(redisKeyBuild.getRelKey())).thenReturn(null);

        String result = redisCache.get(redisKeyBuild, String.class,
                () -> null, 60, TimeUnit.SECONDS);

        assertNull(result);
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any());
    }

    @Test
    @DisplayName("getValueIsList缓存为空时返回空列表")
    void getValueIsListEmpty() {
        when(valueOperations.get(redisKeyBuild.getRelKey())).thenReturn("");

        List<Integer> result = redisCache.getValueIsList(redisKeyBuild, Integer.class);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getValueIsList将JSON数组解析为对象列表")
    void getValueIsListParse() {
        when(valueOperations.get(redisKeyBuild.getRelKey())).thenReturn("[1,2,3]");

        List<Integer> result = redisCache.getValueIsList(redisKeyBuild, Integer.class);

        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    @DisplayName("set字符串类型时原样写入，不做JSON序列化")
    void setStringValue() {
        redisCache.set(redisKeyBuild, "abc");

        verify(valueOperations).set(redisKeyBuild.getRelKey(), "abc");
    }

    @Test
    @DisplayName("set对象类型时序列化为JSON写入")
    void setObjectValue() {
        TestPojo pojo = new TestPojo(2L, "测试");

        redisCache.set(redisKeyBuild, pojo);

        verify(valueOperations).set(redisKeyBuild.getRelKey(), JSON.toJSONString(pojo));
    }

    @Test
    @DisplayName("set带ttl时按指定过期时间写入")
    void setWithTtl() {
        redisCache.set(redisKeyBuild, "abc", 30, TimeUnit.MINUTES);

        verify(valueOperations).set(redisKeyBuild.getRelKey(), "abc", 30L, TimeUnit.MINUTES);
    }

    @Test
    @DisplayName("setIfAbsent返回redis的操作结果")
    void setIfAbsent() {
        when(valueOperations.setIfAbsent(redisKeyBuild.getRelKey(), "v")).thenReturn(true);

        assertTrue(redisCache.setIfAbsent(redisKeyBuild, "v"));
    }

    @Test
    @DisplayName("multiSet将Map中的对象序列化为JSON后批量写入")
    void multiSet() {
        RedisKeyBuild key1 = RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM, 1L);
        RedisKeyBuild key2 = RedisKeyBuild.createRedisKey(RedisKeyManage.PROGRAM, 2L);
        Map<RedisKeyBuild, Object> map = new LinkedHashMap<>();
        map.put(key1, "strValue");
        map.put(key2, new TestPojo(3L, "批量"));

        redisCache.multiSet(map);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(valueOperations).multiSet(captor.capture());
        Map<String, String> savedMap = captor.getValue();
        assertEquals("strValue", savedMap.get(key1.getRelKey()));
        assertEquals(JSON.toJSONString(new TestPojo(3L, "批量")), savedMap.get(key2.getRelKey()));
    }

    @Test
    @DisplayName("incrBy对key自增并返回自增后的值")
    void incrBy() {
        when(valueOperations.increment(redisKeyBuild.getRelKey(), 2L)).thenReturn(5L);

        assertEquals(5L, redisCache.incrBy(redisKeyBuild, 2L));
    }

    @Test
    @DisplayName("hasKey、expire、del透传到redisTemplate")
    void hasKeyExpireDel() {
        when(redisTemplate.hasKey(redisKeyBuild.getRelKey())).thenReturn(true);
        when(redisTemplate.expire(redisKeyBuild.getRelKey(), 60L, TimeUnit.SECONDS)).thenReturn(true);

        assertTrue(redisCache.hasKey(redisKeyBuild));
        assertTrue(redisCache.expire(redisKeyBuild, 60L, TimeUnit.SECONDS));

        redisCache.del(redisKeyBuild);
        verify(redisTemplate).delete(redisKeyBuild.getRelKey());
    }

    @Test
    @DisplayName("putHash对象类型时序列化为JSON写入hash")
    void putHashObject() {
        TestPojo pojo = new TestPojo(4L, "哈希");

        redisCache.putHash(redisKeyBuild, "hashKey", pojo);

        verify(hashOperations).put(redisKeyBuild.getRelKey(), "hashKey", JSON.toJSONString(pojo));
    }

    @Test
    @DisplayName("getForHash将hash中的JSON反序列化为对象")
    void getForHashObject() {
        TestPojo pojo = new TestPojo(5L, "哈希取值");
        when(hashOperations.get(redisKeyBuild.getRelKey(), "hashKey")).thenReturn(JSON.toJSONString(pojo));

        TestPojo result = redisCache.getForHash(redisKeyBuild, "hashKey", TestPojo.class);

        assertEquals(5L, result.getId());
        assertEquals("哈希取值", result.getName());
    }

    @Test
    @DisplayName("leftPushForList对象类型时序列化为JSON后左推入列表")
    void leftPushForList() {
        when(listOperations.leftPush(redisKeyBuild.getRelKey(), "v")).thenReturn(1L);

        assertEquals(1L, redisCache.leftPushForList(redisKeyBuild, "v"));
    }

    @Test
    @DisplayName("getAllForList将列表中的JSON元素解析为对象列表")
    void getAllForList() {
        when(listOperations.range(redisKeyBuild.getRelKey(), 0L, -1L)).thenReturn(Arrays.asList("1", "2"));

        List<Integer> result = redisCache.getAllForList(redisKeyBuild, Integer.class);

        assertEquals(Arrays.asList(1, 2), result);
    }

    @Test
    @DisplayName("addForSet与isMemberForSet透传到set操作")
    void setOperation() {
        when(setOperations.add(redisKeyBuild.getRelKey(), "v")).thenReturn(1L);
        when(setOperations.isMember(redisKeyBuild.getRelKey(), "v")).thenReturn(true);

        assertEquals(1L, redisCache.addForSet(redisKeyBuild, "v"));
        assertTrue(redisCache.isMemberForSet(redisKeyBuild, "v"));
    }

    @Test
    @DisplayName("addForSortedSet带ttl时先写入zset再设置过期时间")
    void addForSortedSetWithTtl() {
        redisCache.addForSortedSet(redisKeyBuild, "v", 1.0, 60L);

        verify(zSetOperations).add(redisKeyBuild.getRelKey(), "v", 1.0);
        verify(redisTemplate).expire(redisKeyBuild.getRelKey(), 60L, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("value为null时抛出参数缺失异常且不操作redis")
    void nullValueThrowException() {
        assertThrows(RuntimeException.class, () -> redisCache.leftPushForList(redisKeyBuild, null));
        verifyNoInteractions(listOperations);
    }

    @Test
    @DisplayName("hashKey为空时抛出参数缺失异常且不操作redis")
    void blankHashKeyThrowException() {
        assertThrows(RuntimeException.class, () -> redisCache.putHashIfAbsent(redisKeyBuild, "", "v"));
        verifyNoInteractions(hashOperations);
    }

    /**
     * 测试用POJO
     * */
    public static class TestPojo {

        private Long id;

        private String name;

        public TestPojo() {
        }

        public TestPojo(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
