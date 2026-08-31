package com.damai.namefactory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 线程命名工厂单元测试
 */
class NameThreadFactoryTest {

    @Test
    @DisplayName("BusinessNameThreadFactory 的名称前缀应以 task-pool-- 开头")
    void businessFactoryNamePrefixShouldStartWithTaskPool() {
        BusinessNameThreadFactory factory = new BusinessNameThreadFactory();
        assertTrue(factory.getNamePrefix().startsWith("task-pool--"));
    }

    @Test
    @DisplayName("不同工厂实例应分配到不同的线程池编号，名称前缀互不相同")
    void differentFactoriesShouldHaveDifferentPoolNumbers() {
        BusinessNameThreadFactory factory1 = new BusinessNameThreadFactory();
        BusinessNameThreadFactory factory2 = new BusinessNameThreadFactory();
        assertNotEquals(factory1.getNamePrefix(), factory2.getNamePrefix());
    }

    @Test
    @DisplayName("newThread：线程名应为 前缀--thread--N 且 N 从 1 开始递增（同一工厂内前缀一致）")
    void newThreadNameShouldHaveIncrementingNumber() {
        BusinessNameThreadFactory factory = new BusinessNameThreadFactory();

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            threads.add(factory.newThread(() -> {
            }));
        }

        // 注意：BusinessNameThreadFactory.getNamePrefix() 每次调用都会递增 POOL_NUM，
        // 因此不能用它反推前缀，直接从第一个线程名中提取前缀
        String firstName = threads.get(0).getName();
        assertTrue(firstName.matches("task-pool--\\d+--thread--1"),
                "线程名格式应为 task-pool--{池编号}--thread--1，实际：" + firstName);
        String prefix = firstName.substring(0, firstName.lastIndexOf("--thread--"));

        for (int i = 0; i < 3; i++) {
            assertEquals(prefix + "--thread--" + (i + 1), threads.get(i).getName());
        }
    }

    @Test
    @DisplayName("newThread：创建的线程应为非守护线程且优先级为 NORM_PRIORITY")
    void newThreadShouldBeNonDaemonWithNormalPriority() {
        BusinessNameThreadFactory factory = new BusinessNameThreadFactory();
        Thread thread = factory.newThread(() -> {
        });

        assertFalse(thread.isDaemon());
        assertEquals(Thread.NORM_PRIORITY, thread.getPriority());
    }
}
