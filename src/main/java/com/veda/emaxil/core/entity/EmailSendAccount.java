package com.veda.emaxil.core.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 邮件发送账户
 * @param <A>
 */
@Getter
@RequiredArgsConstructor
public class EmailSendAccount<A extends AbstractAccount> {

    /**
     * 账户主键
     */
    private final String id;
    /**
     * 账户配置
     */
    private final A account;
    /**
     * 邮件服务供应商
     */
    private final EmailServiceProvider provider;
    /**
     * 发送限制
     */
    private final List<EmailSendRestrict> restricts;
    /**
     * 当前邮件发送服务在当前时间是否可用
     */
    @Setter
    private volatile boolean isEnable = false;
}
