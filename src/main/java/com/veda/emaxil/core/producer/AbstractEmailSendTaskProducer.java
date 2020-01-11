package com.veda.emaxil.core.producer;

import com.veda.emaxil.core.entity.AbstractEmail;

/**
 * 生产者接口
 * @param <E> 邮件
 */
public interface AbstractEmailSendTaskProducer<E extends AbstractEmail> {

    /**
     * 生产邮件发送任务
     * @param email 邮件
     */
    void produce(E email);
}
