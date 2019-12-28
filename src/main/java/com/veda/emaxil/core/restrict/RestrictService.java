package com.veda.emaxil.core.restrict;

import com.veda.emaxil.core.entity.EmailSendAccount;
import com.veda.emaxil.core.entity.EmailSendTask;
import com.veda.emaxil.core.entity.AbstractAccount;
import com.veda.emaxil.core.entity.AbstractEmail;

import java.util.List;

/**
 * 限制业务接口
 * @param <A> 账户详情
 * @param <E> 邮件详情
 */
public interface RestrictService<A extends AbstractAccount, E extends AbstractEmail> {

    /**
     * 更新邮件发送账户的可用状态
     * @param emailSendAccounts 发送账户列表
     * @param isPassEnabled 是否对当前可用的邮件发送账户进行跳过 只处理不可用的
     */
    void updateEmailSendAccountEnable(List<EmailSendAccount<A>> emailSendAccounts, boolean isPassEnabled);

    /**
     * 删除 beforeTimestamps 之前时间的 邮件发送账户 统计数据
     * @param emailSendAccounts 发送账户列表
     * @param beforeTimestamps 需要删除该时间戳 之前的统计数据
     */
    void deleteEmailSendAccountStatistic(List<EmailSendAccount<A>> emailSendAccounts, long beforeTimestamps);

    /**
     * 当前 发送账户 是否可以发送 该邮件任务
     * @param emailSendAccount 发送账户
     * @param emailSendTask 邮件任务
     * @return 是否可以发送
     */
    boolean canSend(EmailSendAccount<A> emailSendAccount, EmailSendTask<E> emailSendTask);
}
