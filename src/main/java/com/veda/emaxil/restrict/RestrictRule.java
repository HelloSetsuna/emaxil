package com.veda.emaxil.restrict;

import lombok.Data;

import java.util.concurrent.TimeUnit;

/**
 * 速率限制器规则
 * @author derick.jin
 */
@Data
public class RestrictRule {
    public enum LimitTypeEnum {
        /**
         * 总数限制
         */
        COUNT_LIMIT,
        //
        /**
         * 速率限制
         */
        SPEED_LIMIT;
    }

    public enum TimeTypeEnum {
        /**
         * 从 当前时间前的一段范围时间 到 当前时间 范围内
         * 如：限制每小时最多发送1000封邮件，如发送时间是 07:12:21，计算方式是按当天的 06:12:22 到当天 07:12:21 时间内发送的总数
         */
        RANGE_TIME,
        /**
         * 从 当前时间所在起始时间点 到 结束时间点 范围内
         * 如：限制每小时最多发送1000封邮件，如发送时间是 07:12:21，计算方式是按当天的 07:00:00 到当天 07:59:59 时间内发送的总数
         */
        POINT_TIME;
    }

    /**
     * 时间配置数值
     */
    private Long timeValue;
    /**
     * 时间配置单位
     */
    private TimeUnit timeUnit;
    /**
     * 时间配置类型
     */
    private TimeTypeEnum timeType;
    /**
     * 限制配置数值
     */
    private Long limitValue;
    /**
     * 限制配置类型
     */
    private LimitTypeEnum limitType;
}
