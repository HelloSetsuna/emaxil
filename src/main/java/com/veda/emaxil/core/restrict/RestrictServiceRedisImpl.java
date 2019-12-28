package com.veda.emaxil.core.restrict;

import com.veda.emaxil.core.entity.EmailSendAccount;
import com.veda.emaxil.core.entity.EmailSendTask;
import com.veda.emaxil.core.entity.AbstractAccount;
import com.veda.emaxil.core.entity.AbstractEmail;

import java.util.List;

public class RestrictServiceRedisImpl<A extends AbstractAccount, E extends AbstractEmail> implements RestrictService<A, E> {

    /**
     * 更新邮件发送账户的可用状态
     * 查询每个账户的 zSet 是否符合规则 并更改对应的 isEnable 状态
     * @param emailSendAccounts 发送账户列表
     * @param isPassEnabled     是否对当前可用的邮件发送账户进行跳过 只处理不可用的
     */
    @Override
    public void updateEmailSendAccountEnable(List<EmailSendAccount<A>> emailSendAccounts, boolean isPassEnabled) {

    }

    /**
     * 删除 beforeTimestamps 之前时间的 邮件发送账户 统计数据
     *
     * @param emailSendAccounts 发送账户列表
     * @param beforeTimestamps  需要删除该时间戳 之前的统计数据
     */
    @Override
    public void deleteEmailSendAccountStatistic(List<EmailSendAccount<A>> emailSendAccounts, long beforeTimestamps) {

    }

    /**
     * 当前 发送账户 是否可以发送 该邮件任务
     * 先使用 Redis 分布式锁 尝试获取该发送账户的锁, 获取锁后 验证限制规则是否符合, 如果不符合 标记该账户为不可用, 返回 false
     * 如果符合 则向 zSet 中添加一条记录, 返回 true, 无论如何最终都会释放 该账户的分布式锁
     * @param emailSendAccount 发送账户
     * @param emailSendTask    邮件任务
     * @return 是否可以发送
     */
    @Override
    public boolean canSend(EmailSendAccount<A> emailSendAccount, EmailSendTask<E> emailSendTask) {
        return false;
    }
}
