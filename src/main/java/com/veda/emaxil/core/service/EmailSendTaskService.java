package com.veda.emaxil.core.service;

import com.veda.emaxil.core.entity.AbstractEmail;
import com.veda.emaxil.core.entity.EmailSendTask;

/**
 * 邮件内容持久化接口
 * @param <E> 邮件
 */
public interface EmailSendTaskService<E extends AbstractEmail> {

    /**
     * 根据 主键 获取邮件的信息
     * @param id 邮件主键
     * @return 邮件发送任务
     */
    EmailSendTask<E> select(String id);

    /**
     * 持久化邮件内容 插入
     * @param emailSendTask 邮件发送任务
     */
    void insert(EmailSendTask<E> emailSendTask);

    /**
     * 持久化邮件内容 更新
     * @param emailSendTask 邮件发送任务
     */
    void update(EmailSendTask<E> emailSendTask);
}
