package com.damai.context;

import com.damai.config.DelayQueueProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DelayQueueProduceCombine 延迟队列分片生产者单元测试（Redisson 客户端使用 Mockito mock）
 */
@ExtendWith(MockitoExtension.class)
class DelayQueueProduceCombineTest {

    @Mock
    private DelayQueueBasePart delayQueueBasePart;

    @Mock
    private RedissonClient redissonClient;

    /**
     * 按分片主题分别返回独立的延迟队列 mock，便于验证消息路由
     */
    private Map<String, RDelayedQueue<String>> mockQueueByTopic(int regionCount, String topic) {
        Map<String, RDelayedQueue<String>> queueMap = new HashMap<>();
        for (int i = 0; i < regionCount; i++) {
            RBlockingQueue<String> blockingQueue = mock(RBlockingQueue.class);
            RDelayedQueue<String> delayedQueue = mock(RDelayedQueue.class);
            when(redissonClient.<String>getBlockingQueue(topic + "-" + i)).thenReturn(blockingQueue);
            when(redissonClient.<String>getDelayedQueue(blockingQueue)).thenReturn(delayedQueue);
            queueMap.put(topic + "-" + i, delayedQueue);
        }
        return queueMap;
    }

    @Test
    @DisplayName("构造时按隔离区数量创建对应数量的分片队列")
    void constructCreatesRegionQueues() {
        DelayQueueProperties properties = new DelayQueueProperties();
        properties.setIsolationRegionCount(3);
        when(delayQueueBasePart.getDelayQueueProperties()).thenReturn(properties);
        when(delayQueueBasePart.getRedissonClient()).thenReturn(redissonClient);
        mockQueueByTopic(3, "delay-queue");

        new DelayQueueProduceCombine(delayQueueBasePart, "delay-queue");

        for (int i = 0; i < 3; i++) {
            verify(redissonClient).getBlockingQueue("delay-queue-" + i);
        }
    }

    @Test
    @DisplayName("offer 按轮询分片路由消息到对应延迟队列")
    void offerRoutesByRoundRobin() {
        DelayQueueProperties properties = new DelayQueueProperties();
        properties.setIsolationRegionCount(2);
        when(delayQueueBasePart.getDelayQueueProperties()).thenReturn(properties);
        when(delayQueueBasePart.getRedissonClient()).thenReturn(redissonClient);
        Map<String, RDelayedQueue<String>> queueMap = mockQueueByTopic(2, "delay-queue");

        DelayQueueProduceCombine combine = new DelayQueueProduceCombine(delayQueueBasePart, "delay-queue");
        combine.offer("content-1", 10L, TimeUnit.SECONDS);
        combine.offer("content-2", 20L, TimeUnit.SECONDS);

        // 轮询：第一条进分片0，第二条进分片1
        verify(queueMap.get("delay-queue-0")).offer("content-1", 10L, TimeUnit.SECONDS);
        verify(queueMap.get("delay-queue-1")).offer("content-2", 20L, TimeUnit.SECONDS);
    }
}
