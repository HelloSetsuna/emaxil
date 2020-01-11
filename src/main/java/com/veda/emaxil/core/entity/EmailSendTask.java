package com.veda.emaxil.core.entity;

import lombok.Data;

/**
 * 邮件发送任务
 * @param <E>
 */
@Data
public class EmailSendTask<E extends AbstractEmail> {

    /**
     * 邮件发送状态
     */
    public enum StatusEnum {
        /**
         * 未发送
         */
        UN_SEND,
        /**
         * 发送中
         */
        SENDING,
        /**
         * 发送成功
         */
        SEND_SUCCESS,
        /**
         * 发送失败
         */
        SEND_FAILURE,
    }

    /**
     * 标识邮件发送任务的 ID
     */
    private final String id;
    /**
     * 邮件的具体数据封装
     */
    private transient E email;
    /**
     * 邮件发送任务优先级
     */
    private final int priority;
    /**
     * 邮件发送状态
     */
    private volatile StatusEnum status;
    /**
     * 邮件的当前重试次数
     */
    private volatile int retryCount = 0;
    /**
     * 邮件发送失败的最大重试次数
     */
    private volatile int retryLimit = 0;
    /**
     * 发送失败时的异常信息
     */
    private String errorMessage;
}
