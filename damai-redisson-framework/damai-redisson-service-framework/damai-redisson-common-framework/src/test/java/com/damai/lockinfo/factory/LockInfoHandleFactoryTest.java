package com.damai.lockinfo.factory;

import com.damai.constant.LockInfoType;
import com.damai.lockinfo.LockInfoHandle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

/**
 * 锁信息工厂 LockInfoHandleFactory 单元测试
 */
@ExtendWith(MockitoExtension.class)
class LockInfoHandleFactoryTest {

    @Mock
    private ApplicationContext applicationContext;

    @InjectMocks
    private LockInfoHandleFactory lockInfoHandleFactory;

    @Test
    @DisplayName("根据业务类型从Spring容器中获取对应的LockInfoHandle")
    void getLockInfoHandleReturnsBeanFromContext() {
        LockInfoHandle lockInfoHandle = mock(LockInfoHandle.class);
        when(applicationContext.getBean(LockInfoType.SERVICE_LOCK, LockInfoHandle.class))
                .thenReturn(lockInfoHandle);

        LockInfoHandle result = lockInfoHandleFactory.getLockInfoHandle(LockInfoType.SERVICE_LOCK);

        assertSame(lockInfoHandle, result);
        verify(applicationContext).getBean(LockInfoType.SERVICE_LOCK, LockInfoHandle.class);
    }

    @Test
    @DisplayName("防重复幂等类型应使用repeat_execute_limit作为bean名称获取")
    void getLockInfoHandleForRepeatExecuteLimit() {
        LockInfoHandle lockInfoHandle = mock(LockInfoHandle.class);
        when(applicationContext.getBean(LockInfoType.REPEAT_EXECUTE_LIMIT, LockInfoHandle.class))
                .thenReturn(lockInfoHandle);

        LockInfoHandle result = lockInfoHandleFactory.getLockInfoHandle(LockInfoType.REPEAT_EXECUTE_LIMIT);

        assertSame(lockInfoHandle, result);
        verify(applicationContext).getBean(LockInfoType.REPEAT_EXECUTE_LIMIT, LockInfoHandle.class);
    }
}
