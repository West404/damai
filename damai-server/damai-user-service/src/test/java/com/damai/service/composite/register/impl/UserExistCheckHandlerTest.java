package com.damai.service.composite.register.impl;

import com.damai.dto.UserRegisterDto;
import com.damai.enums.BaseCode;
import com.damai.enums.CompositeCheckType;
import com.damai.exception.DaMaiFrameException;
import com.damai.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * UserExistCheckHandler 单元测试：注册时检查用户是否已存在的责任链节点
 */
@ExtendWith(MockitoExtension.class)
class UserExistCheckHandlerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserExistCheckHandler userExistCheckHandler;

    @Test
    @DisplayName("执行时按入参手机号调用用户存在性检查")
    void executeCallsDoExistWithMobile() {
        UserRegisterDto dto = new UserRegisterDto();
        dto.setMobile("13800138000");

        userExistCheckHandler.execute(dto);

        verify(userService).doExist("13800138000");
    }

    @Test
    @DisplayName("用户已存在时将 USER_EXIST 异常向上传递")
    void executePropagatesUserExistException() {
        UserRegisterDto dto = new UserRegisterDto();
        dto.setMobile("13800138000");
        doThrow(new DaMaiFrameException(BaseCode.USER_EXIST)).when(userService).doExist("13800138000");

        DaMaiFrameException exception = assertThrows(DaMaiFrameException.class,
                () -> userExistCheckHandler.execute(dto));
        assertEquals(BaseCode.USER_EXIST.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("责任链节点的类型与执行顺序配置正确")
    void checkCompositeConfig() {
        assertEquals(CompositeCheckType.USER_REGISTER_CHECK.getValue(), userExistCheckHandler.type());
        assertEquals(1, userExistCheckHandler.executeParentOrder());
        assertEquals(2, userExistCheckHandler.executeTier());
        assertEquals(2, userExistCheckHandler.executeOrder());
    }
}
