package com.damai.service;

import com.alibaba.fastjson.JSONObject;
import com.damai.core.SpringUtil;
import com.damai.enums.BaseCode;
import com.damai.exception.DaMaiFrameException;
import com.damai.jwt.TokenUtil;
import com.damai.redis.RedisCache;
import com.damai.redis.RedisKeyBuild;
import com.damai.vo.UserVo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @description: TokenService token解析与用户获取 单元测试（RedisCache使用Mockito mock）
 **/
@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private RedisCache redisCache;

    @InjectMocks
    private TokenService tokenService;

    private final String tokenSecret = "gateway-test-secret";

    @BeforeAll
    static void initSpringUtil() {
        // RedisKeyBuild 依赖 SpringUtil 静态环境读取 key 前缀
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        MockEnvironment environment = new MockEnvironment().withProperty("prefix.distinction.name", "test");
        when(context.getEnvironment()).thenReturn(environment);
        new SpringUtil().initialize(context);
    }

    private String createToken(String userId) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("userId", userId);
        return TokenUtil.createToken("1", jsonObject.toJSONString(), 60_000, tokenSecret);
    }

    @Test
    @DisplayName("解析合法token：能够解析出userId")
    void testParseTokenSuccess() {
        String token = createToken("1001");
        String userId = tokenService.parseToken(token, tokenSecret);
        assertEquals("1001", userId);
    }

    @Test
    @DisplayName("解析错误密钥的token：抛出异常")
    void testParseTokenWithWrongSecret() {
        String token = createToken("1001");
        assertThrows(Exception.class, () -> tokenService.parseToken(token, "wrong-secret"));
    }

    @Test
    @DisplayName("解析过期token：抛出 DaMaiFrameException(token过期)")
    void testParseExpiredToken() {
        //来自TokenUtil中示例的已过期token（密钥 CSYZWECHAT，exp=1688854284）
        String expiredToken = "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiIxIiwiaWF0IjoxNjg4NTQyODM3LCJzdWIiOiJ7XCIwMDJrZXlcIjpcIjAwMXZhbHVlXCIsXCIwMDFrZXlcIjpcIjAwMXZhbHVlXCJ9IiwiZXhwIjoxNjg4NTQyODQ3fQ.vIKcAilTn_CR3VYssNE7rBpfuCSCH_RrkmsadLWf664";
        DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                () -> tokenService.parseToken(expiredToken, "CSYZWECHAT"));
        assertEquals(BaseCode.TOKEN_EXPIRE.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("获取用户：token合法且缓存命中时返回用户信息")
    void testGetUserSuccess() {
        String token = createToken("1001");
        UserVo userVo = new UserVo();
        userVo.setId("1001");
        userVo.setName("大麦用户");
        when(redisCache.get(any(RedisKeyBuild.class), eq(UserVo.class))).thenReturn(userVo);

        UserVo result = tokenService.getUser(token, "0001", tokenSecret);
        assertEquals("1001", result.getId());
        assertEquals("大麦用户", result.getName());
        verify(redisCache).get(any(RedisKeyBuild.class), eq(UserVo.class));
    }

    @Test
    @DisplayName("获取用户：token合法但缓存未命中时抛出 DaMaiFrameException(用户没有登录)")
    void testGetUserCacheMiss() {
        String token = createToken("1001");
        when(redisCache.get(any(RedisKeyBuild.class), eq(UserVo.class))).thenReturn(null);

        DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                () -> tokenService.getUser(token, "0001", tokenSecret));
        assertEquals(BaseCode.LOGIN_USER_NOT_EXIST.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("获取用户：token解析不出userId时直接抛出JSON解析异常，不查询缓存")
    void testGetUserWithoutUserId() {
        //subject不是json格式，parseToken中JSONObject.parseObject会直接抛JSONException（生产代码未转译为业务异常）
        String token = TokenUtil.createToken("1", "plain-subject", 60_000, tokenSecret);

        assertThrows(com.alibaba.fastjson.JSONException.class,
                () -> tokenService.getUser(token, "0001", tokenSecret));
        verify(redisCache, never()).get(any(RedisKeyBuild.class), eq(UserVo.class));
    }

    @Test
    @DisplayName("解析token：subject为空json时返回null")
    void testParseTokenEmptySubject() {
        String token = TokenUtil.createToken("1", null, 60_000, tokenSecret);
        assertNull(tokenService.parseToken(token, tokenSecret));
    }
}
