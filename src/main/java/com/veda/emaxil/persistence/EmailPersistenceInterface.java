package com.veda.emaxil.persistence;

import com.veda.emaxil.context.SendEmailTask;

/**
 * 邮件内容持久化接口
 * @param <E> 邮件
 */
public interface EmailPersistenceInterface<E extends EmailInterface> {

    /**
     * 根据 主键 获取邮件的信息
     * @param id 邮件主键
     * @return 持久化的邮件内容
     */
    E select(String id);

    /**
     * 持久化邮件内容 插入
     * @param sendEmailTask 邮件发送任务
     */
    void insert(SendEmailTask<E> sendEmailTask);

    /**
     * 持久化邮件内容 更新
     * @param sendEmailTask 邮件发送任务
     */
    void update(SendEmailTask<E> sendEmailTask);
}
