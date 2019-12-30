package com.veda.emaxil.util;

import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 基于 单节点 Redis 实现分布式锁
 * @author derick.jin 2019-12-30 18:58:00
 * @version 1.0
 **/
@Slf4j
@Component
public class RedisDistributedLock {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private DefaultRedisScript<Long> redisScript;

    /**
     * 加锁
     * @param lockName 锁名称
     * @param timeoutMillis 锁超时时间 毫秒
     * @return 锁内容
     */
    public String doLock(String lockName, long timeoutMillis) {
        String lockValue = IdUtil.fastSimpleUUID();
        while (true) {
            Boolean isAbsent = stringRedisTemplate.opsForValue().setIfAbsent(lockName, lockValue, timeoutMillis, TimeUnit.MILLISECONDS);
            if (Objects.nonNull(isAbsent) && isAbsent) {
                log.info("do lock:{} success", lockName);
                return lockValue;
            }
            log.info("do lock:{} retry", lockName);
        }
    }

    /**
     * 解锁
     * @param lockName 锁名称
     * @param lockValue 锁内容
     * @return 是否解锁成功
     */
    public boolean unlock(String lockName, String lockValue) {
        // 使用 Lua 脚本 保证获取 和 删除是一组原子操作
        Long result = stringRedisTemplate.execute(redisScript, Arrays.asList(lockName, lockValue));
        log.info("un lock:{} finish:{}", lockName, result);
        return !Objects.isNull(result) && result == 1L;
    }

    @Bean
    public DefaultRedisScript<Long> defaultRedisScript() {
        DefaultRedisScript<Long> defaultRedisScript = new DefaultRedisScript<>();
        defaultRedisScript.setResultType(Long.class);
        defaultRedisScript.setScriptText("if redis.call('get', KEYS[1]) == KEYS[2] then return redis.call('del', KEYS[1]) else return 0 end");
        return defaultRedisScript;
    }
}
