package com.damai.service.composite.register.impl;

import com.damai.dto.UserRegisterDto;
import com.damai.enums.BaseCode;
import com.damai.enums.CompositeCheckType;
import com.damai.exception.DaMaiFrameException;
import com.damai.service.tool.RequestCounter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * UserRegisterCountCheckHandler 单元测试：注册频率限制校验节点
 */
@ExtendWith(MockitoExtension.class)
class UserRegisterCountCheckHandlerTest {

    @Mock
    private RequestCounter requestCounter;

    @InjectMocks
    private UserRegisterCountCheckHandler userRegisterCountCheckHandler;

    @Test
    @DisplayName("请求未超限时正常通过")
    void executeWithoutFrequencyLimit() {
        when(requestCounter.onRequest()).thenReturn(false);

        assertDoesNotThrow(() -> userRegisterCountCheckHandler.execute(new UserRegisterDto()));
    }

    @Test
    @DisplayName("请求超限时抛出 USER_REGISTER_FREQUENCY 异常")
    void executeWithFrequencyLimit() {
        when(requestCounter.onRequest()).thenReturn(true);

        DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                () -> userRegisterCountCheckHandler.execute(new UserRegisterDto()));
        assertEquals(BaseCode.USER_REGISTER_FREQUENCY.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("责任链节点的类型与执行顺序配置正确")
    void checkCompositeConfig() {
        assertEquals(CompositeCheckType.USER_REGISTER_CHECK.getValue(), userRegisterCountCheckHandler.type());
        assertEquals(1, userRegisterCountCheckHandler.executeParentOrder());
        assertEquals(2, userRegisterCountCheckHandler.executeTier());
        assertEquals(1, userRegisterCountCheckHandler.executeOrder());
    }
}
