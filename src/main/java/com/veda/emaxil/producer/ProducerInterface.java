package com.veda.emaxil.producer;

import com.veda.emaxil.persistence.EmailInterface;

/**
 * 生产者接口
 * @param <E> 邮件
 */
public interface ProducerInterface <E extends EmailInterface> {

    /**
     * 生产邮件发送任务
     * @param email 邮件
     */
    void produce(E email);
}
