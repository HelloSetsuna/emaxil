package com.veda.emaxil;

import com.veda.emaxil.util.RedisDistributedLock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author derick.jin 2019-12-30 20:07:00
 * @version 1.0
 **/
@SpringBootTest
public class RedisDistributedLuckTest {

    @Autowired
    private RedisDistributedLock redisDistributedLock;

    @Test
    public void test(){
        ExecutorService executorService = Executors.newFixedThreadPool(20);
        for (int i = 0; i < 10; i++) {
            executorService.execute(() -> {
                String lockValue = redisDistributedLock.doLock("lock", 1000);
                System.out.println("lalal~");
                redisDistributedLock.unlock("lock", lockValue);
            });
        }
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
