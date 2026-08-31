package com.damai.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IsolationRegionSelector 延迟队列分片选择器单元测试
 */
class IsolationRegionSelectorTest {

    @Test
    @DisplayName("分片索引按轮询方式递增，达到阈值后重置为0")
    void getIndexRoundRobin() {
        IsolationRegionSelector selector = new IsolationRegionSelector(4);

        // 0,1,2,3 之后触发重置；注意 reset 只归零不自增，下一轮首次调用仍返回 0
        assertEquals(0, selector.getIndex());
        assertEquals(1, selector.getIndex());
        assertEquals(2, selector.getIndex());
        assertEquals(3, selector.getIndex());
        assertEquals(0, selector.getIndex());
        assertEquals(0, selector.getIndex());
        assertEquals(1, selector.getIndex());
    }

    @Test
    @DisplayName("阈值为1时索引恒为0")
    void getIndexWithSingleRegion() {
        IsolationRegionSelector selector = new IsolationRegionSelector(1);
        for (int i = 0; i < 5; i++) {
            assertEquals(0, selector.getIndex());
        }
    }

    @Test
    @DisplayName("多线程并发获取索引时不会出现越界或重复乱序")
    void getIndexConcurrent() throws InterruptedException {
        IsolationRegionSelector selector = new IsolationRegionSelector(4);
        int threads = 8;
        int perThread = 100;
        Set<Integer> indices = ConcurrentHashMap.newKeySet();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                for (int i = 0; i < perThread; i++) {
                    indices.add(selector.getIndex());
                }
                latch.countDown();
            });
        }
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        pool.shutdownNow();

        // 所有索引都在 [0,3] 范围内且不超过阈值
        assertTrue(indices.stream().allMatch(i -> i >= 0 && i < 4));
        assertEquals(4, indices.size(), "并发下四个分片都应被轮询到");
    }
}
