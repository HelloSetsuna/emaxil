package com.veda.emaxil;

import cn.hutool.core.util.IdUtil;
import com.veda.emaxil.util.RedisMessageQueue;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author derick.jin 2019-12-30 20:20:00
 * @version 1.0
 **/
@Slf4j
@SpringBootTest
public class RedisMessageQueueTest {

    @Autowired
    private RedisMessageQueue redisMessageQueue;

    @Test
    public void test(){
        String queueName = "queue";
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 2; i++) {
            executorService.execute(() -> {
                for (int j = 0; j < 10000; j++) {
                    try {
                        String value = IdUtil.fastSimpleUUID();
                        redisMessageQueue.addValue(queueName, value);
                        log.info("producer value:{}", value);
                    } catch (Exception e) {
                        log.error("producer error", e);
                    }
                }
            });
        }
        AtomicLong atomicLong = new AtomicLong(0);
        for (int i = 0; i < 3; i++) {
            executorService.execute(() -> {
                while (true) {
                    try {
                        RedisMessageQueue.QueueValue queueValue = redisMessageQueue.getValue(queueName);
                        log.info("consumer value:{} for:{}", queueValue.getValue(), atomicLong.incrementAndGet());
                        redisMessageQueue.ackValue(queueName, queueValue);
                    } catch (Exception e) {
                        log.error("consumer error", e);
                    }
                }
            });
        }
        try {
            Thread.sleep(100000);
            System.out.println(atomicLong.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
