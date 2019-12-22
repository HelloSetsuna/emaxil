package com.veda.emaxil.persistence;

import java.util.List;

/**
 * 邮件发送账户及其限制规则持久化接口
 * @param <A>
 */
public interface AccountPersistenceInterface<A extends AccountInterface> {

    /**
     * 获取所有可使用的邮件发送账户 及 其相关限制规则
     * @return 所有可使用的邮件发送账户
     */
    List<A> select();
}
