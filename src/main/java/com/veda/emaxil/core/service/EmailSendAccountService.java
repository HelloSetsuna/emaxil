package com.veda.emaxil.core.service;

import com.veda.emaxil.core.entity.AbstractAccount;
import com.veda.emaxil.core.entity.EmailSendAccount;

import java.util.List;

/**
 * 邮件发送账户及其限制规则持久化接口
 * @param <A>
 */
public interface EmailSendAccountService<A extends AbstractAccount> {

    /**
     * 获取所有可使用的邮件发送账户 及 其相关限制规则
     * @return 所有可使用的邮件发送账户
     */
    List<EmailSendAccount<A>> selectAllEnabled();
}
