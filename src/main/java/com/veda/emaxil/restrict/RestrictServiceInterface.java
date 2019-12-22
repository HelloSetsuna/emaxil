package com.veda.emaxil.restrict;

import com.veda.emaxil.context.SendAccount;
import com.veda.emaxil.context.SendEmailTask;
import com.veda.emaxil.persistence.AccountInterface;
import com.veda.emaxil.persistence.EmailInterface;

/**
 * 限制业务接口
 * @param <A> 账户详情
 * @param <E> 邮件详情
 */
public interface RestrictServiceInterface<A extends AccountInterface, E extends EmailInterface> {

    /**
     * 当前 发送账户 是否可以发送 该邮件任务
     * @param sendAccount 发送账户
     * @param sendEmailTask 邮件任务
     * @return 是否可以发送
     */
    boolean canSend(SendAccount<A> sendAccount, SendEmailTask<E> sendEmailTask);

    /**
     * 当前 发送账户 已经发送 该邮件任务
     * @param sendAccount 发送账户
     * @param sendEmailTask 邮件任务
     */
    void hasSend(SendAccount<A> sendAccount, SendEmailTask<E> sendEmailTask);
}
