package com.veda.emaxil.core.consumer;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.veda.emaxil.core.entity.*;
import com.veda.emaxil.util.RedisMessageQueue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.PostConstruct;

/**
 * 基于RedisMQ 实现的消费者
 * @author derick.jin 2020-01-11 11:24:00
 * @version 1.0
 **/
@Slf4j
public class RedisMqEmailSendTaskConsumer<A extends AbstractAccount, E extends AbstractEmail> extends AbstractEmailSendTaskConsumer<A, E> {

    @Autowired
    private RedisMessageQueue redisMessageQueue;

    private static final String QUEUE_NAME = "email-send-task-queue";

    @PostConstruct
    public void registerConsumer() {
        ThreadPoolTaskExecutor emailSendThreadPool = getEmaxilCore().getEmailSendThreadPool();
        EmailSendConfig emailSendConfig = getEmaxilCore().getEmailSendConfig();
        final Runnable consumer = () ->{
            while (true) {
                String value = null;
                try {
                    RedisMessageQueue.QueueValue queueValue = redisMessageQueue.getValue(QUEUE_NAME);
                    value = queueValue.getValue();
                    // 将获取的值转换为 EmailSendTaskDTO
                    EmailSendTaskDTO emailSendTaskDTO = JSON.parseObject(queueValue.getValue(), EmailSendTaskDTO.class);
                    // 将DTO 转换为 EmailSendTask<E>
                    EmailSendTask<E> emailSendTask = new EmailSendTask<>(emailSendTaskDTO.getId(), emailSendTaskDTO.getPriority());
                    emailSendTask.setStatus(EmailSendTask.StatusEnum.values()[emailSendTaskDTO.getStatus()]);
                    emailSendTask.setRetryCount(emailSendTaskDTO.getRetryCount());
                    emailSendTask.setRetryLimit(emailSendTaskDTO.getRetryLimit());
                    // 消费者消费消息 EmailSendTask<E>
                    consume(emailSendTask);
                    // 消费者 进行 ACK 确认 消息消费成功
                    redisMessageQueue.ackValue(QUEUE_NAME, queueValue);
                } catch (Exception e) {
                    log.error(StrUtil.format("email send task:{} consume failed", value), e);
                }
            }
        };
        // 按线程池的核心数创建消费者线程 这些消费者 常驻 线程池
        for (Integer i = 0; i < emailSendConfig.getThreadPoolSize(); i++) {
            emailSendThreadPool.execute(consumer);
        }
    }
}
