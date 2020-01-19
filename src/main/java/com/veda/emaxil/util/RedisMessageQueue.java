package com.veda.emaxil.util;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 基于 Redis 实现的 较可靠消息队列
 * 队列消息出队消费后 需要 进行 ACK, 否则超时将重新回原队列中
 * @author Derick S.Jin 2019/12/30
 */
@Slf4j
@Component
public class RedisMessageQueue {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 这里加锁主要是 应对 处于超时临界点的即将超时数据的ACK清理 可能存在的并发问题
     * 可能会发生 一个消费者在数据即将超时时刚好获取该数据 在进行处理, 还未进行ACK,
     * 与此同时, 另一个异步清理超时未ACK的线程发现它超时了, 将其重新放回原队列, 这就可能导致重复消费的问题
     * 这里使用读写锁只适用于单节点的项目, 如果是分布式的情况则应该改为 分布式读写锁
     * 分布式读写锁我还没有基于Redis实现过, 有机会我再优化此处吧, 一般来说可以将超时时间
     * 设置的比较长来规避这个问题, 或者对消息的处理进行防止重复处理的验证, 我这里因为是邮件发送
     * 在发送前会更新邮件发送任务的状态, 因此此锁对业务的影响不大, 我也是出于多想才加上的
     */
    private ReentrantReadWriteLock ackQueueReadWriteLock = new ReentrantReadWriteLock(true);

    @Data
    @AllArgsConstructor
    public static class QueueValue {
        private String value;
        private Long timestamp;
    }

    /**
     * 非阻塞的 将 值 推送到消息队列中
     * @param queueName 队列名称
     * @param value 字符串值
     * @return QueueValue
     */
    public QueueValue addValue(String queueName, String value) {
        if (StrUtil.isBlank(queueName) || StrUtil.isBlank(value)) {
            throw new IllegalArgumentException("queueName or value can not be blank");
        }
        // 将值和时间戳一起构建成为消息 时间戳是为了清理 ACK 队列超时未确认的消息的
        QueueValue queueValue = new QueueValue(value, System.currentTimeMillis());
        // 序列号为 JSON 后将其推送到队列中
        Long result = stringRedisTemplate.opsForList().leftPush(queueName, JSON.toJSONString(queueValue));
        if (Objects.isNull(result) || result == 0) {
            throw new IllegalStateException(StrUtil.format("queue:{} leftPush:{} failed", queueName, value));
        }
        return queueValue;
    }

    /**
     * 阻塞的获取队列弹出的值
     * @param queueName 队列名称
     * @return 出队的值 该对象用于 ACK 时使用
     */
    public QueueValue getValue(String queueName) {
        if (StrUtil.isBlank(queueName)) {
            throw new IllegalArgumentException("queueName can not be blank");
        }
        // 无限制的 等待获取消息
        while (true) {
            // 每次等待 60s 如果没有则继续等待
            String value = stringRedisTemplate.opsForList().rightPopAndLeftPush(queueName, getAckQueueName(queueName), 60, TimeUnit.SECONDS);
            if (StrUtil.isNotBlank(value)) {
                // 获取消息后将其 反序列化
                return JSON.parseObject(value, QueueValue.class);
            }
        }
    }

    /**
     * 用于在 getValue 后成功消费消息的 消费确认
     * 确认后消息才会真正离队, 否则超时后会被后台线程重新放置回原队列再次等待消费
     * @param queueName 队列名称
     * @param queueValue 队列值
     * @return 是否确认成功
     */
    public boolean ackValue(String queueName, QueueValue queueValue) {
        if (StrUtil.isBlank(queueName)) {
            throw new IllegalArgumentException("queueName can not be blank");
        }
        if (Objects.isNull(queueValue) || StrUtil.isBlank(queueValue.getValue()) || Objects.isNull(queueValue.getTimestamp())) {
            throw new IllegalArgumentException("queueValue or fields can not be null");
        }
        String value = JSON.toJSONString(queueValue);
        // 多线程执行该方法并不会互斥
        ackQueueReadWriteLock.readLock().lock();
        try {
            // 将消息从 ACK 队列中移除
            Long result = stringRedisTemplate.opsForList().remove(getAckQueueName(queueName), 0, value);
            // ACK 队列中已经没有该元素 可能被重新放置回原队列中
            if (Objects.nonNull(result) && result == 0) {
                // 将消息从原队列中移除
                result = stringRedisTemplate.opsForList().remove(queueName, 0, value);
                if (Objects.nonNull(result) && result == 0) {
                    // 如果原队列中也没有则返回 false 表示未 ACK 成功
                    return false;
                }
            }
            // 其它情况下认为 ACK 成功
            return true;
        } finally {
            ackQueueReadWriteLock.readLock().unlock();
        }
    }

    /**
     * 对超时未进行 ACK 的消息进行清理, 将他们放回原消息队列中
     * 该方法应被一个定时任务线程定期执行 确保 timeoutMills 足够的长 不会影响正常的 ACK 确认
     * @param queueName 原队列名称
     * @param timeoutMills 超时时间 毫秒
     */
    public void ackClean(String queueName, long timeoutMills){
        if (StrUtil.isBlank(queueName)) {
            throw new IllegalArgumentException("queueName can not be blank");
        }
        // 获取 ACK 队列的所有数据 通常不会很多 约等于消费者数量
        String ackQueueName = getAckQueueName(queueName);
        // 在执行 ACK 清理时, 其它线程无法再进行 ACK 确认操作
        // 同理 其它线程在进行确认时 无法进行清理 公平的 减少碰撞概率
        ackQueueReadWriteLock.writeLock().lock();
        try {
            List<String> values = stringRedisTemplate.opsForList().range(ackQueueName, 0, -1);
            // 如果 ACK 队列 无数据则跳过 表明没有 ACK 异常 也没有正在消费的消息
            if (CollectionUtil.isEmpty(values)) {
                return;
            }
            long currentTimeMillis = System.currentTimeMillis();
            // 开始事务
            stringRedisTemplate.multi();
            for (String value : values) {
                QueueValue queueValue = JSON.parseObject(value, QueueValue.class);
                // 属于超时未 ACK 的消息 需要重回原队列
                if (currentTimeMillis - queueValue.getTimestamp() > timeoutMills) {
                    // 从 ACK 队列中移除
                    stringRedisTemplate.opsForList().remove(ackQueueName, 0, value);
                    // 重新入原队列
                    stringRedisTemplate.opsForList().leftPush(queueName, value);
                }
            }
            // 提交事务
            log.info("queue:{} ack clean exec result:{}", queueName, stringRedisTemplate.exec());
        } finally {
            ackQueueReadWriteLock.writeLock().unlock();
        }
    }

    /**
     * 根据原队列名称 获取 ACK 队列名称
     * @param queueName 原队列名称
     * @return 超时队列名称
     */
    private String getAckQueueName(String queueName){
        return StrUtil.format("ack-{}", queueName);
    }
}
