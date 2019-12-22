package com.veda.emaxil.context;

import com.veda.emaxil.persistence.AccountInterface;
import com.veda.emaxil.restrict.RestrictRule;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class SendAccount<A extends AccountInterface> {

    /**
     * 账户主键
     */
    private final String id;
    /**
     * 账户配置
     */
    private final A account;
    /**
     * 限制规则
     */
    private final List<RestrictRule> rules;

    /**
     * 当前邮件发送服务在当前时间是否可用
     */
    private volatile boolean isEnable = false;
    /**
     * 上次检查该账户的可用性时候的时间戳
     */
    private volatile long lastCheckedAt;
}
