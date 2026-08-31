package com.damai.service;

import com.damai.client.BaseDataClient;
import com.damai.common.ApiResponse;
import com.damai.core.SpringUtil;
import com.damai.dto.GetChannelDataByCodeDto;
import com.damai.enums.BaseCode;
import com.damai.exception.ArgumentException;
import com.damai.exception.DaMaiFrameException;
import com.damai.redis.RedisCache;
import com.damai.redis.RedisKeyBuild;
import com.damai.vo.GetChannelDataVo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @description: ChannelDataService 渠道数据获取 单元测试（Feign客户端与Redis使用Mockito mock）
 **/
@ExtendWith(MockitoExtension.class)
class ChannelDataServiceTest {

    @Mock
    private BaseDataClient baseDataClient;

    @Mock
    private RedisCache redisCache;

    private ChannelDataService channelDataService;

    private ThreadPoolExecutor threadPoolExecutor;

    @BeforeAll
    static void initSpringUtil() {
        // RedisKeyBuild 依赖 SpringUtil 静态环境读取 key 前缀
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        MockEnvironment environment = new MockEnvironment().withProperty("prefix.distinction.name", "test");
        when(context.getEnvironment()).thenReturn(environment);
        new SpringUtil().initialize(context);
    }

    @BeforeEach
    void setUp() {
        channelDataService = new ChannelDataService();
        ReflectionTestUtils.setField(channelDataService, "baseDataClient", baseDataClient);
        ReflectionTestUtils.setField(channelDataService, "redisCache", redisCache);
        //使用真实线程池执行异步调用，mock的Feign客户端在池线程中被调用
        threadPoolExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        ReflectionTestUtils.setField(channelDataService, "threadPoolExecutor", threadPoolExecutor);
    }

    @AfterEach
    void tearDown() {
        threadPoolExecutor.shutdownNow();
    }

    private GetChannelDataVo buildChannelDataVo() {
        GetChannelDataVo vo = new GetChannelDataVo();
        vo.setId(1L);
        vo.setCode("0001");
        vo.setName("测试渠道");
        vo.setTokenSecret("token-secret");
        return vo;
    }

    @Test
    @DisplayName("校验code参数：code为空时抛出 ArgumentException")
    void testCheckCodeEmpty() {
        ArgumentException nullException = assertThrows(ArgumentException.class,
                () -> channelDataService.checkCode(null));
        assertEquals(BaseCode.ARGUMENT_EMPTY.getCode(), nullException.getCode());

        assertThrows(ArgumentException.class, () -> channelDataService.checkCode(""));
    }

    @Test
    @DisplayName("按code获取渠道数据：redis缓存命中时直接返回，不调用Feign客户端")
    void testGetChannelDataCacheHit() {
        GetChannelDataVo cacheVo = buildChannelDataVo();
        when(redisCache.get(any(RedisKeyBuild.class), eq(GetChannelDataVo.class))).thenReturn(cacheVo);

        GetChannelDataVo result = channelDataService.getChannelDataByCode("0001");

        assertSame(cacheVo, result);
        verify(baseDataClient, never()).getByCode(any(GetChannelDataByCodeDto.class));
    }

    @Test
    @DisplayName("按code获取渠道数据：缓存未命中时走Feign客户端查询，并回写redis缓存")
    void testGetChannelDataCacheMissAndClientSuccess() {
        when(redisCache.get(any(RedisKeyBuild.class), eq(GetChannelDataVo.class))).thenReturn(null);
        GetChannelDataVo clientVo = buildChannelDataVo();
        when(baseDataClient.getByCode(any(GetChannelDataByCodeDto.class)))
                .thenReturn(ApiResponse.ok(clientVo));

        GetChannelDataVo result = channelDataService.getChannelDataByCode("0001");

        assertNotNull(result);
        assertEquals("0001", result.getCode());
        assertEquals("测试渠道", result.getName());
        //验证回写缓存
        verify(redisCache).set(any(RedisKeyBuild.class), eq(clientVo));
    }

    @Test
    @DisplayName("按code获取渠道数据：Feign返回非成功状态码时抛出 DaMaiFrameException(没有找到ChannelData)")
    void testGetChannelDataClientErrorCode() {
        when(redisCache.get(any(RedisKeyBuild.class), eq(GetChannelDataVo.class))).thenReturn(null);
        when(baseDataClient.getByCode(any(GetChannelDataByCodeDto.class)))
                .thenReturn(ApiResponse.error(BaseCode.SYSTEM_ERROR));

        DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                () -> channelDataService.getChannelDataByCode("0001"));
        assertEquals(BaseCode.CHANNEL_DATA_NOT_EXIST.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("按code获取渠道数据：Feign调用抛出异常时抛出 DaMaiFrameException(系统异常)")
    void testGetChannelDataClientExecutionException() {
        when(redisCache.get(any(RedisKeyBuild.class), eq(GetChannelDataVo.class))).thenReturn(null);
        when(baseDataClient.getByCode(any(GetChannelDataByCodeDto.class)))
                .thenThrow(new RuntimeException("feign调用失败"));

        DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                () -> channelDataService.getChannelDataByCode("0001"));
        assertEquals(BaseCode.SYSTEM_ERROR.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("按code获取渠道数据：code为空时直接抛出参数异常，不查询缓存与客户端")
    void testGetChannelDataWithEmptyCode() {
        assertThrows(ArgumentException.class, () -> channelDataService.getChannelDataByCode(""));
        verify(redisCache, never()).get(any(RedisKeyBuild.class), eq(GetChannelDataVo.class));
        verify(baseDataClient, never()).getByCode(any(GetChannelDataByCodeDto.class));
    }

    @Test
    @DisplayName("校验code参数：异常信息中包含参数名code")
    void testCheckCodeArgumentDetail() {
        ArgumentException exception = assertThrows(ArgumentException.class,
                () -> channelDataService.checkCode(null));
        assertTrue(exception.getArgumentErrorList().stream()
                .anyMatch(error -> "code".equals(error.getArgumentName())));
    }
}
