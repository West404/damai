package com.damai.handler;

import com.damai.config.BloomFilterProperties;
import com.damai.core.SpringUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * BloomFilterHandler 布隆过滤器单元测试（Redisson 客户端使用 Mockito mock）
 */
@ExtendWith(MockitoExtension.class)
class BloomFilterHandlerTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RBloomFilter<String> bloomFilter;

    private BloomFilterHandler bloomFilterHandler;

    @BeforeAll
    static void initSpringUtil() {
        // BloomFilterHandler 构造时通过 SpringUtil 读取 key 前缀
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        MockEnvironment environment = new MockEnvironment().withProperty("prefix.distinction.name", "test");
        when(context.getEnvironment()).thenReturn(environment);
        new SpringUtil().initialize(context);
    }

    @BeforeEach
    void setUp() {
        BloomFilterProperties properties = new BloomFilterProperties();
        properties.setName("program-detail-bloom-filter");
        properties.setExpectedInsertions(1000L);
        properties.setFalseProbability(0.01);
        when(redissonClient.<String>getBloomFilter("test-program-detail-bloom-filter")).thenReturn(bloomFilter);
        bloomFilterHandler = new BloomFilterHandler(redissonClient, properties);
    }

    @Test
    @DisplayName("构造时按服务前缀拼接过滤器名称并初始化容量参数")
    void constructInitializesBloomFilter() {
        verify(redissonClient).getBloomFilter("test-program-detail-bloom-filter");
        verify(bloomFilter).tryInit(1000L, 0.01);
    }

    @Test
    @DisplayName("add/contains 委托给 RBloomFilter")
    void addAndContainsDelegate() {
        when(bloomFilter.add("user-1")).thenReturn(true);
        when(bloomFilter.contains("user-1")).thenReturn(true);
        when(bloomFilter.contains("user-2")).thenReturn(false);

        assertTrue(bloomFilterHandler.add("user-1"));
        assertTrue(bloomFilterHandler.contains("user-1"));
        assertFalse(bloomFilterHandler.contains("user-2"));
    }

    @Test
    @DisplayName("容量/误判率/槽位数等属性透传")
    void propertiesDelegate() {
        when(bloomFilter.getExpectedInsertions()).thenReturn(1000L);
        when(bloomFilter.getFalseProbability()).thenReturn(0.01);
        when(bloomFilter.getSize()).thenReturn(128L);
        when(bloomFilter.getHashIterations()).thenReturn(7);
        when(bloomFilter.count()).thenReturn(42L);

        assertEquals(1000L, bloomFilterHandler.getExpectedInsertions());
        assertEquals(0.01, bloomFilterHandler.getFalseProbability());
        assertEquals(128L, bloomFilterHandler.getSize());
        assertEquals(7, bloomFilterHandler.getHashIterations());
        assertEquals(42L, bloomFilterHandler.count());
    }
}
