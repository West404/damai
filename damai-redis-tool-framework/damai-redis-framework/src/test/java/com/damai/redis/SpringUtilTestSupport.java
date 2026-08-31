package com.damai.redis;

import com.damai.constant.Constant;
import com.damai.core.SpringUtil;
import org.mockito.Mockito;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * @description: 测试辅助类：为 SpringUtil 注入 mock 的 ApplicationContext，
 * 使 RedisKeyBuild 在无 Spring 容器的环境下也能解析出 key 前缀
 * @author: 阿星不是程序员
 **/
public final class SpringUtilTestSupport {

    /**
     * 测试使用的 key 前缀
     * */
    public static final String TEST_PREFIX = "test";

    private SpringUtilTestSupport() {
    }

    /**
     * 初始化 SpringUtil 的静态 ApplicationContext，前缀固定为 test
     * */
    public static void initPrefixDistinctionName() {
        ConfigurableApplicationContext context = Mockito.mock(ConfigurableApplicationContext.class);
        ConfigurableEnvironment environment = Mockito.mock(ConfigurableEnvironment.class);
        Mockito.when(context.getEnvironment()).thenReturn(environment);
        Mockito.when(environment.getProperty(Constant.PREFIX_DISTINCTION_NAME,
                Constant.DEFAULT_PREFIX_DISTINCTION_NAME)).thenReturn(TEST_PREFIX);
        new SpringUtil().initialize(context);
    }
}
