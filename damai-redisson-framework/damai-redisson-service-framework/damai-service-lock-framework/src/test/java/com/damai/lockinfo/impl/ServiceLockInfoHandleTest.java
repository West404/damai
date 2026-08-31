package com.damai.lockinfo.impl;

import com.damai.core.SpringUtil;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ServiceLockInfoHandle 单元测试（重点覆盖 AbstractLockInfoHandle 的锁key SpEL 解析逻辑）
 */
class ServiceLockInfoHandleTest {

    private final ServiceLockInfoHandle lockInfoHandle = new ServiceLockInfoHandle();

    /**
     * 模拟业务方法所属类
     */
    @SuppressWarnings("unused")
    static class DemoService {
        public String createOrder(String orderId, Long userId) {
            return "ok";
        }
    }

    /**
     * 模拟接口 + 实现类场景（切面签名拿到的是接口方法）
     */
    interface DemoApi {
        String createOrder(String orderId, Long userId);
    }

    static class DemoApiImpl implements DemoApi {
        @Override
        public String createOrder(String orderId, Long userId) {
            return "ok";
        }
    }

    @BeforeAll
    static void initSpringUtil() {
        // SpringUtil 依赖静态 ApplicationContext，这里用 mock 的上下文初始化，避免启动真实容器
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        MockEnvironment environment = new MockEnvironment().withProperty("prefix.distinction.name", "test");
        when(context.getEnvironment()).thenReturn(environment);
        new SpringUtil().initialize(context);
    }

    /**
     * 构造指定方法与参数的 JoinPoint mock
     */
    private JoinPoint buildJoinPoint(Method method, Object target, Object... args) {
        JoinPoint joinPoint = mock(JoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getName()).thenReturn(method.getName());
        when(signature.getParameterTypes()).thenReturn(method.getParameterTypes());
        when(joinPoint.getTarget()).thenReturn(target);
        when(joinPoint.getArgs()).thenReturn(args);
        return joinPoint;
    }

    @Test
    @DisplayName("SpEL解析：keys使用方法参数名，锁名应拼接参数值")
    void getLockNameWithSpElKeys() throws Exception {
        Method method = DemoService.class.getDeclaredMethod("createOrder", String.class, Long.class);
        JoinPoint joinPoint = buildJoinPoint(method, new DemoService(), "order-1001", 2002L);

        String lockName = lockInfoHandle.getLockName(joinPoint, "createOrder", new String[]{"#orderId", "#userId"});

        assertEquals("test-SERVICE_LOCK:createOrder:order-1001:2002", lockName);
    }

    @Test
    @DisplayName("SpEL解析：签名方法来自接口时，应回退到目标实现类的方法解析参数名")
    void getLockNameWithInterfaceMethod() throws Exception {
        Method interfaceMethod = DemoApi.class.getDeclaredMethod("createOrder", String.class, Long.class);
        JoinPoint joinPoint = buildJoinPoint(interfaceMethod, new DemoApiImpl(), "order-1001", 2002L);

        String lockName = lockInfoHandle.getLockName(joinPoint, "createOrder", new String[]{"#orderId"});

        assertEquals("test-SERVICE_LOCK:createOrder:order-1001", lockName);
    }

    @Test
    @DisplayName("SpEL解析：支持常量字符串与表达式混合的keys")
    void getLockNameWithMixedKeys() throws Exception {
        Method method = DemoService.class.getDeclaredMethod("createOrder", String.class, Long.class);
        JoinPoint joinPoint = buildJoinPoint(method, new DemoService(), "order-1001", 2002L);

        String lockName = lockInfoHandle.getLockName(joinPoint, "createOrder",
                new String[]{"'fixed'", "#orderId"});

        assertEquals("test-SERVICE_LOCK:createOrder:fixed:order-1001", lockName);
    }

    @Test
    @DisplayName("SpEL解析：空key会被忽略，不参与锁名拼接")
    void getLockNameIgnoresEmptyKeys() throws Exception {
        Method method = DemoService.class.getDeclaredMethod("createOrder", String.class, Long.class);
        JoinPoint joinPoint = buildJoinPoint(method, new DemoService(), "order-1001", 2002L);

        String lockName = lockInfoHandle.getLockName(joinPoint, "createOrder",
                new String[]{"#orderId", ""});

        assertEquals("test-SERVICE_LOCK:createOrder:order-1001", lockName);
    }

    @Test
    @DisplayName("simpleGetLockName：使用固定前缀并过滤空key")
    void simpleGetLockNameFiltersEmptyKeys() {
        String lockName = lockInfoHandle.simpleGetLockName("createOrder",
                new String[]{"a", "", "  ", "null", "b"});

        assertEquals("test-LOCK_DISTRIBUTE_ID:createOrder:a:b", lockName);
    }
}
