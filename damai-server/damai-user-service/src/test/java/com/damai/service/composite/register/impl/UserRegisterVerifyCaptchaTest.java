package com.damai.service.composite.register.impl;

import com.damai.captcha.model.common.RepCodeEnum;
import com.damai.captcha.model.common.ResponseModel;
import com.damai.captcha.model.vo.CaptchaVO;
import com.damai.core.RedisKeyManage;
import com.damai.core.SpringUtil;
import com.damai.dto.UserRegisterDto;
import com.damai.enums.BaseCode;
import com.damai.enums.CompositeCheckType;
import com.damai.enums.VerifyCaptcha;
import com.damai.exception.DaMaiFrameException;
import com.damai.redis.RedisCache;
import com.damai.redis.RedisKeyBuild;
import com.damai.service.CaptchaHandle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UserRegisterVerifyCaptcha 单元测试：注册校验责任链中的验证码校验节点（Redis 与验证码服务均用 Mockito mock）
 */
@ExtendWith(MockitoExtension.class)
class UserRegisterVerifyCaptchaTest {

    @Mock
    private CaptchaHandle captchaHandle;

    @Mock
    private RedisCache redisCache;

    @InjectMocks
    private UserRegisterVerifyCaptcha userRegisterVerifyCaptcha;

    @BeforeAll
    static void initSpringUtil() {
        // RedisKeyBuild 依赖 SpringUtil 静态环境读取 key 前缀
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        MockEnvironment environment = new MockEnvironment().withProperty("prefix.distinction.name", "test");
        when(context.getEnvironment()).thenReturn(environment);
        new SpringUtil().initialize(context);
    }

    private UserRegisterDto buildDto() {
        UserRegisterDto dto = new UserRegisterDto();
        dto.setMobile("13800138000");
        dto.setPassword("password123");
        dto.setConfirmPassword("password123");
        dto.setCaptchaId("captcha-id-1");
        dto.setCaptchaVerification("verification-data");
        return dto;
    }

    @Test
    @DisplayName("两次密码不一致时抛出 TWO_PASSWORDS_DIFFERENT 异常")
    void executeWithDifferentPasswords() {
        UserRegisterDto dto = buildDto();
        dto.setConfirmPassword("different-password");

        DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                () -> userRegisterVerifyCaptcha.execute(dto));
        assertEquals(BaseCode.TWO_PASSWORDS_DIFFERENT.getCode(), exception.getCode());
        // 密码不一致应直接失败，不查询 Redis
        verify(redisCache, never()).get(any(RedisKeyBuild.class), eq(String.class));
    }

    @Test
    @DisplayName("Redis中不存在验证码标识时抛出 VERIFY_CAPTCHA_ID_NOT_EXIST 异常")
    void executeWithCaptchaIdNotExist() {
        UserRegisterDto dto = buildDto();
        when(redisCache.get(RedisKeyBuild.createRedisKey(RedisKeyManage.VERIFY_CAPTCHA_ID, dto.getCaptchaId()),
                String.class)).thenReturn(null);

        DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                () -> userRegisterVerifyCaptcha.execute(dto));
        assertEquals(BaseCode.VERIFY_CAPTCHA_ID_NOT_EXIST.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("不需要校验验证码时直接通过，不调用验证码服务")
    void executeWithNoNeedVerifyCaptcha() {
        UserRegisterDto dto = buildDto();
        dto.setCaptchaVerification(null);
        when(redisCache.get(RedisKeyBuild.createRedisKey(RedisKeyManage.VERIFY_CAPTCHA_ID, dto.getCaptchaId()),
                String.class)).thenReturn(VerifyCaptcha.NO.getValue());

        assertDoesNotThrow(() -> userRegisterVerifyCaptcha.execute(dto));
        verify(captchaHandle, never()).verification(any(CaptchaVO.class));
    }

    @Test
    @DisplayName("需要校验验证码但captchaVerification为空时抛出 VERIFY_CAPTCHA_EMPTY 异常")
    void executeWithEmptyCaptchaVerification() {
        UserRegisterDto dto = buildDto();
        dto.setCaptchaVerification("");
        when(redisCache.get(RedisKeyBuild.createRedisKey(RedisKeyManage.VERIFY_CAPTCHA_ID, dto.getCaptchaId()),
                String.class)).thenReturn(VerifyCaptcha.YES.getValue());

        DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                () -> userRegisterVerifyCaptcha.execute(dto));
        assertEquals(BaseCode.VERIFY_CAPTCHA_EMPTY.getCode(), exception.getCode());
        verify(captchaHandle, never()).verification(any(CaptchaVO.class));
    }

    @Test
    @DisplayName("验证码服务校验失败时抛出携带其错误码的异常")
    void executeWithVerificationFailed() {
        UserRegisterDto dto = buildDto();
        when(redisCache.get(RedisKeyBuild.createRedisKey(RedisKeyManage.VERIFY_CAPTCHA_ID, dto.getCaptchaId()),
                String.class)).thenReturn(VerifyCaptcha.YES.getValue());
        ResponseModel responseModel = ResponseModel.errorMsg(RepCodeEnum.API_CAPTCHA_INVALID);
        when(captchaHandle.verification(any(CaptchaVO.class))).thenReturn(responseModel);

        DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                () -> userRegisterVerifyCaptcha.execute(dto));
        assertEquals(Integer.valueOf(responseModel.getRepCode()), exception.getCode());
    }

    @Test
    @DisplayName("验证码服务校验成功时正常通过")
    void executeWithVerificationSuccess() {
        UserRegisterDto dto = buildDto();
        when(redisCache.get(RedisKeyBuild.createRedisKey(RedisKeyManage.VERIFY_CAPTCHA_ID, dto.getCaptchaId()),
                String.class)).thenReturn(VerifyCaptcha.YES.getValue());
        when(captchaHandle.verification(any(CaptchaVO.class))).thenReturn(ResponseModel.success());

        assertDoesNotThrow(() -> userRegisterVerifyCaptcha.execute(dto));
        verify(captchaHandle).verification(any(CaptchaVO.class));
    }

    @Test
    @DisplayName("责任链节点的类型与执行顺序配置正确")
    void checkCompositeConfig() {
        assertEquals(CompositeCheckType.USER_REGISTER_CHECK.getValue(), userRegisterVerifyCaptcha.type());
        assertEquals(0, userRegisterVerifyCaptcha.executeParentOrder());
        assertEquals(1, userRegisterVerifyCaptcha.executeTier());
        assertEquals(1, userRegisterVerifyCaptcha.executeOrder());
    }
}
