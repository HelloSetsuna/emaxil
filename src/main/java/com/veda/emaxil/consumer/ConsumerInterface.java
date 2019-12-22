package com.veda.emaxil.consumer;

import com.veda.emaxil.context.SendEmailTask;
import com.veda.emaxil.persistence.EmailInterface;

/**
 * 消费者接口
 * @param <E> 邮件
 */
public interface ConsumerInterface<E extends EmailInterface> {

    /**
     * 消费邮件发送任务 需要自行选择账户 判断能否发送 调用 Sender 发送，并更新邮件发送状态 和 相关统计数据
     * @param sendEmailTask 邮件发送任务
     */
    void consume(SendEmailTask<E> sendEmailTask);
}
