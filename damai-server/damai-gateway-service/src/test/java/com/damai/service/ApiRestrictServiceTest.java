package com.damai.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baidu.fsg.uid.UidGenerator;
import com.damai.core.SpringUtil;
import com.damai.enums.ApiRuleType;
import com.damai.enums.BaseCode;
import com.damai.enums.RuleTimeUnit;
import com.damai.exception.DaMaiFrameException;
import com.damai.kafka.ApiDataMessageSend;
import com.damai.property.GatewayProperty;
import com.damai.redis.RedisCache;
import com.damai.redis.RedisKeyBuild;
import com.damai.service.lua.ApiRestrictCacheOperate;
import com.damai.vo.DepthRuleVo;
import com.damai.vo.RuleVo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @description: ApiRestrictService 接口限流 单元测试（Redis、Lua执行器、Kafka使用Mockito mock）
 **/
@ExtendWith(MockitoExtension.class)
class ApiRestrictServiceTest {

    @Mock
    private RedisCache redisCache;

    @Mock
    private ApiDataMessageSend apiDataMessageSend;

    @Mock
    private ApiRestrictCacheOperate apiRestrictCacheOperate;

    @Mock
    private UidGenerator uidGenerator;

    @Mock
    private ServerHttpRequest request;

    private ApiRestrictService apiRestrictService;

    private GatewayProperty gatewayProperty;

    private final String requestUri = "/program/detail";

    @BeforeAll
    static void initSpringUtil() {
        // RedisKeyBuild 依赖 SpringUtil 静态环境读取 key 前缀，用 mock 上下文初始化
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        MockEnvironment environment = new MockEnvironment().withProperty("prefix.distinction.name", "test");
        when(context.getEnvironment()).thenReturn(environment);
        new SpringUtil().initialize(context);
    }

    @BeforeEach
    void setUp() {
        apiRestrictService = new ApiRestrictService();
        gatewayProperty = new GatewayProperty();
        gatewayProperty.setApiRestrictPaths(new String[]{"/program/**"});
        ReflectionTestUtils.setField(apiRestrictService, "redisCache", redisCache);
        ReflectionTestUtils.setField(apiRestrictService, "gatewayProperty", gatewayProperty);
        ReflectionTestUtils.setField(apiRestrictService, "apiDataMessageSend", apiDataMessageSend);
        ReflectionTestUtils.setField(apiRestrictService, "apiRestrictCacheOperate", apiRestrictCacheOperate);
        ReflectionTestUtils.setField(apiRestrictService, "uidGenerator", uidGenerator);

        // 部分用例不涉及 request，宽松化避免 UnnecessaryStubbingException
        HttpHeaders headers = new HttpHeaders();
        headers.add("x-forwarded-for", "192.168.1.1");
        lenient().when(request.getHeaders()).thenReturn(headers);
    }

    private RuleVo buildRuleVo() {
        RuleVo ruleVo = new RuleVo();
        ruleVo.setId("1");
        ruleVo.setStatTime(60);
        ruleVo.setStatTimeType(RuleTimeUnit.SECOND.getCode());
        ruleVo.setThreshold(10);
        ruleVo.setEffectiveTime(5);
        ruleVo.setEffectiveTimeType(RuleTimeUnit.MINUTE.getCode());
        ruleVo.setMessage("触发限流，请稍后再试");
        return ruleVo;
    }

    private ApiRestrictData buildApiRestrictData(long triggerResult, long triggerCallStat, long messageIndex) {
        ApiRestrictData data = new ApiRestrictData();
        data.setTriggerResult(triggerResult);
        data.setTriggerCallStat(triggerCallStat);
        data.setApiCount(11L);
        data.setThreshold(10L);
        data.setMessageIndex(messageIndex);
        return data;
    }

    @Test
    @DisplayName("检查限流路径：请求uri匹配限流路径时返回true")
    void testCheckApiRestrictMatch() {
        assertTrue(apiRestrictService.checkApiRestrict("/program/detail"));
        assertTrue(apiRestrictService.checkApiRestrict("/program/order/create"));
    }

    @Test
    @DisplayName("检查限流路径：请求uri不匹配限流路径时返回false")
    void testCheckApiRestrictNotMatch() {
        assertFalse(apiRestrictService.checkApiRestrict("/user/login"));
    }

    @Test
    @DisplayName("检查限流路径：未配置限流路径时返回false")
    void testCheckApiRestrictNullPaths() {
        gatewayProperty.setApiRestrictPaths(null);
        assertFalse(apiRestrictService.checkApiRestrict(requestUri));
    }

    @Test
    @DisplayName("获取真实IP：多级代理时取x-forwarded-for的第一个IP")
    void testGetIpAddressFromXForwardedFor() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("x-forwarded-for", "1.2.3.4, 5.6.7.8");
        when(request.getHeaders()).thenReturn(headers);

        assertEquals("1.2.3.4", ApiRestrictService.getIpAddress(request));
    }

    @Test
    @DisplayName("获取真实IP：x-forwarded-for为unknown时依次降级到后续请求头")
    void testGetIpAddressFallbackHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("x-forwarded-for", "unknown");
        headers.add("Proxy-Client-IP", "unknown");
        headers.add("WL-Proxy-Client-IP", "10.0.0.1");
        when(request.getHeaders()).thenReturn(headers);

        assertEquals("10.0.0.1", ApiRestrictService.getIpAddress(request));
    }

    @Test
    @DisplayName("获取真实IP：所有代理请求头都为空时使用远程地址")
    void testGetIpAddressFromRemoteAddress() {
        when(request.getHeaders()).thenReturn(new HttpHeaders());
        when(request.getRemoteAddress()).thenReturn(new InetSocketAddress("9.9.9.9", 8080));

        assertEquals("9.9.9.9", ApiRestrictService.getIpAddress(request));
    }

    @Test
    @DisplayName("限流校验：uri不在限流路径内时不执行任何规则")
    void testApiRestrictUriNotRestricted() {
        apiRestrictService.apiRestrict("1001", "/user/login", request);

        verify(apiRestrictCacheOperate, never()).apiRuleOperate(anyList(), any());
    }

    @Test
    @DisplayName("限流校验：无普通规则时不执行lua脚本，直接放行")
    void testApiRestrictNoRule() {
        when(redisCache.getForHash(any(RedisKeyBuild.class), anyString(), eq(RuleVo.class))).thenReturn(null);
        when(redisCache.getForHash(any(RedisKeyBuild.class), anyString(), eq(String.class))).thenReturn(null);

        apiRestrictService.apiRestrict("1001", requestUri, request);

        verify(apiRestrictCacheOperate, never()).apiRuleOperate(anyList(), any());
    }

    @Test
    @DisplayName("限流校验：普通规则未触发时不抛异常")
    void testApiRestrictNotTriggered() {
        when(redisCache.getForHash(any(RedisKeyBuild.class), anyString(), eq(RuleVo.class)))
                .thenReturn(buildRuleVo());
        when(redisCache.getForHash(any(RedisKeyBuild.class), anyString(), eq(String.class))).thenReturn(null);
        when(apiRestrictCacheOperate.apiRuleOperate(anyList(), any()))
                .thenReturn(buildApiRestrictData(0L, 0L, -1L));

        apiRestrictService.apiRestrict("1001", requestUri, request);

        verify(apiRestrictCacheOperate).apiRuleOperate(anyList(), any());
        verify(apiDataMessageSend, never()).sendMessage(anyString());
    }

    @Test
    @DisplayName("限流校验：普通规则触发时抛出限流异常，异常信息使用规则自定义message，并上报Kafka")
    void testApiRestrictTriggered() {
        RuleVo ruleVo = buildRuleVo();
        when(redisCache.getForHash(any(RedisKeyBuild.class), anyString(), eq(RuleVo.class)))
                .thenReturn(ruleVo);
        when(redisCache.getForHash(any(RedisKeyBuild.class), anyString(), eq(String.class))).thenReturn(null);
        when(apiRestrictCacheOperate.apiRuleOperate(anyList(), any()))
                .thenReturn(buildApiRestrictData(1L, ApiRuleType.RULE.getCode(), -1L));
        when(uidGenerator.getUid()).thenReturn(123456L);

        DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                () -> apiRestrictService.apiRestrict("1001", requestUri, request));

        assertEquals(BaseCode.API_RULE_TRIGGER.getCode(), exception.getCode());
        assertEquals(ruleVo.getMessage(), exception.getMessage());
        //触发类型为普通规则，验证上报了接口调用记录
        verify(apiDataMessageSend).sendMessage(anyString());
    }

    @Test
    @DisplayName("限流校验：规则触发且触发类型无需统计时不上报Kafka")
    void testApiRestrictTriggeredWithoutCallStat() {
        when(redisCache.getForHash(any(RedisKeyBuild.class), anyString(), eq(RuleVo.class)))
                .thenReturn(buildRuleVo());
        when(redisCache.getForHash(any(RedisKeyBuild.class), anyString(), eq(String.class))).thenReturn(null);
        when(apiRestrictCacheOperate.apiRuleOperate(anyList(), any()))
                .thenReturn(buildApiRestrictData(1L, 0L, -1L));

        assertThrows(DaMaiFrameException.class,
                () -> apiRestrictService.apiRestrict("1001", requestUri, request));

        verify(apiDataMessageSend, never()).sendMessage(anyString());
    }

    @Test
    @DisplayName("限流校验：深度规则触发时使用对应深度规则的message")
    void testApiRestrictDepthRuleTriggered() {
        RuleVo ruleVo = buildRuleVo();
        DepthRuleVo depthRuleVo = new DepthRuleVo();
        depthRuleVo.setStartTimeWindow("00:00:00");
        depthRuleVo.setEndTimeWindow("23:59:59");
        depthRuleVo.setStatTime(60);
        depthRuleVo.setStatTimeType(RuleTimeUnit.SECOND.getCode());
        depthRuleVo.setThreshold(5);
        depthRuleVo.setEffectiveTime(1);
        depthRuleVo.setEffectiveTimeType(RuleTimeUnit.MINUTE.getCode());
        depthRuleVo.setMessage("深度规则限流提示");

        when(redisCache.getForHash(any(RedisKeyBuild.class), anyString(), eq(RuleVo.class)))
                .thenReturn(ruleVo);
        when(redisCache.getForHash(any(RedisKeyBuild.class), anyString(), eq(String.class)))
                .thenReturn(JSON.toJSONString(new ArrayList<>(List.of(depthRuleVo))));
        when(apiRestrictCacheOperate.apiRuleOperate(anyList(), any()))
                .thenReturn(buildApiRestrictData(1L, ApiRuleType.DEPTH_RULE.getCode(), 0L));
        when(uidGenerator.getUid()).thenReturn(123456L);

        DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                () -> apiRestrictService.apiRestrict("1001", requestUri, request));

        assertEquals(BaseCode.API_RULE_TRIGGER.getCode(), exception.getCode());
        assertEquals("深度规则限流提示", exception.getMessage());
    }

    @Test
    @DisplayName("限流校验：lua脚本执行异常时不阻断请求，直接放行")
    void testApiRestrictLuaExceptionNotBlockRequest() {
        when(redisCache.getForHash(any(RedisKeyBuild.class), anyString(), eq(RuleVo.class)))
                .thenReturn(buildRuleVo());
        when(redisCache.getForHash(any(RedisKeyBuild.class), anyString(), eq(String.class))).thenReturn(null);
        when(apiRestrictCacheOperate.apiRuleOperate(anyList(), any()))
                .thenThrow(new RuntimeException("redis连接异常"));

        //不应抛出异常
        apiRestrictService.apiRestrict("1001", requestUri, request);
    }

    @Test
    @DisplayName("构建普通规则参数：时间单位换算正确（秒不变、分钟乘60），key拼接正确")
    void testGetRuleParameter() {
        RuleVo ruleVo = buildRuleVo();
        String commonKey = "192.168.1.1_1001_/program/detail";

        JSONObject parameter = apiRestrictService.getRuleParameter(ApiRuleType.RULE.getCode(), commonKey, ruleVo);

        assertEquals(ApiRuleType.RULE.getCode(), parameter.getInteger("apiRuleType"));
        assertEquals("rule_api_limit_" + commonKey, parameter.getString("ruleKey"));
        //statTimeType为秒，不换算
        assertEquals("60", parameter.getString("statTime"));
        assertEquals(10, parameter.getInteger("threshold"));
        //effectiveTimeType为分钟，换算为300秒
        assertEquals("300", parameter.getString("effectiveTime"));
        assertNotNull(parameter.getString("ruleLimitKey"));
        assertNotNull(parameter.getString("zSetRuleStatKey"));
    }

    @Test
    @DisplayName("构建深度规则参数：按开始时间窗口升序排序，并写入时间戳")
    void testGetDepthRuleParameter() {
        DepthRuleVo early = new DepthRuleVo();
        early.setId("early");
        early.setStartTimeWindow("08:00:00");
        early.setEndTimeWindow("12:00:00");
        early.setStatTime(60);
        early.setStatTimeType(RuleTimeUnit.SECOND.getCode());
        early.setThreshold(5);
        early.setEffectiveTime(1);
        early.setEffectiveTimeType(RuleTimeUnit.SECOND.getCode());

        DepthRuleVo late = new DepthRuleVo();
        late.setId("late");
        late.setStartTimeWindow("13:00:00");
        late.setEndTimeWindow("18:00:00");
        late.setStatTime(2);
        late.setStatTimeType(RuleTimeUnit.MINUTE.getCode());
        late.setThreshold(10);
        late.setEffectiveTime(3);
        late.setEffectiveTimeType(RuleTimeUnit.MINUTE.getCode());

        //传入乱序列表
        JSONObject parameter = new JSONObject();
        apiRestrictService.getDepthRuleParameter(parameter, "commonKey", Arrays.asList(late, early));

        assertEquals("2", parameter.getString("depthRuleSize"));
        assertNotNull(parameter.getLong("currentTime"));
        List<JSONObject> depthRules = (List<JSONObject>) parameter.get("depthRules");
        assertEquals(2, depthRules.size());
        //验证已按开始时间窗口排序：08:00 在 13:00 之前
        JSONObject first = depthRules.get(0);
        JSONObject second = depthRules.get(1);
        assertTrue(first.getLong("startTimeWindowTimestamp") < second.getLong("startTimeWindowTimestamp"));
        //深度规则时间单位换算：分钟转秒
        assertEquals(60, first.getInteger("statTime"));
        assertEquals(120, second.getInteger("statTime"));
        assertEquals("180", second.getString("effectiveTime"));
    }

    @Test
    @DisplayName("时间窗口排序：乱序输入按开始时间窗口时间戳升序输出")
    void testSortStartTimeWindow() {
        DepthRuleVo morning = new DepthRuleVo();
        morning.setId("morning");
        morning.setStartTimeWindow("06:00:00");
        morning.setEndTimeWindow("09:00:00");
        DepthRuleVo evening = new DepthRuleVo();
        evening.setId("evening");
        evening.setStartTimeWindow("20:00:00");
        evening.setEndTimeWindow("22:00:00");
        DepthRuleVo noon = new DepthRuleVo();
        noon.setId("noon");
        noon.setStartTimeWindow("12:00:00");
        noon.setEndTimeWindow("14:00:00");

        List<DepthRuleVo> sorted = apiRestrictService.sortStartTimeWindow(
                new ArrayList<>(Arrays.asList(evening, morning, noon)));

        assertEquals("morning", sorted.get(0).getId());
        assertEquals("noon", sorted.get(1).getId());
        assertEquals("evening", sorted.get(2).getId());
        //验证时间戳被回填
        assertTrue(sorted.get(0).getStartTimeWindowTimestamp() > 0);
        assertTrue(sorted.get(0).getEndTimeWindowTimestamp() > sorted.get(0).getStartTimeWindowTimestamp());
    }

    @Test
    @DisplayName("时间窗口转时间戳：转换为当天对应时间的毫秒值")
    void testGetTimeWindowTimestamp() {
        long timestamp = apiRestrictService.getTimeWindowTimestamp("00:00:00");
        //当天0点的时间戳应为当天0点到当前时间范围内
        assertTrue(timestamp <= System.currentTimeMillis());

        long noonTimestamp = apiRestrictService.getTimeWindowTimestamp("12:00:00");
        assertEquals(12 * 3600 * 1000L, noonTimestamp - timestamp);
    }

    @Test
    @DisplayName("保存接口调用数据：组装ApiDataDto并发送Kafka消息")
    void testSaveApiData() {
        when(uidGenerator.getUid()).thenReturn(999L);

        apiRestrictService.saveApiData(request, requestUri, ApiRuleType.RULE.getCode());

        verify(apiDataMessageSend).sendMessage(anyString());
    }
}
