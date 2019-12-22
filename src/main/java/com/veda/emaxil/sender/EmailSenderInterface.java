package com.veda.emaxil.sender;

import com.veda.emaxil.persistence.AccountInterface;
import com.veda.emaxil.persistence.EmailInterface;

/**
 * 邮件发送接口
 * @param <A> 发送账户
 * @param <E> 发送邮件
 */
public interface EmailSenderInterface<A extends AccountInterface, E extends EmailInterface> {

    /**
     * 只需要实现使用 该账户 发送 该邮件
     * 如发送时发生异常则直接抛出异常即可
     * @param account 发送账户
     * @param email 发送邮件
     */
    void send(A account, E email);
}
