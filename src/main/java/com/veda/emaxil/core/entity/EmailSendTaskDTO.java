package com.veda.emaxil.core.entity;

import lombok.Data;

/**
 * 邮件发送任务 MQ队列中存储的数据
 * @author derick.jin 2020-01-11 11:50:00
 * @version 1.0
 **/
@Data
public class EmailSendTaskDTO {

    /**
     * 标识邮件发送任务的 ID
     */
    private String id;
    /**
     * 邮件发送任务优先级
     */
    private Integer priority;
    /**
     * 邮件发送状态
     */
    private Integer status;
    /**
     * 邮件的当前重试次数
     */
    private Integer retryCount = 0;
    /**
     * 邮件发送失败的最大重试次数
     */
    private Integer retryLimit = 0;
}
