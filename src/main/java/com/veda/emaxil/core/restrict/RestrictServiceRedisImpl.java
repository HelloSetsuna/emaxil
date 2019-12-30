package com.veda.emaxil.core.restrict;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.io.resource.StringResource;
import cn.hutool.core.util.StrUtil;
import com.veda.emaxil.core.entity.*;
import com.veda.emaxil.util.RedisDistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
public class RestrictServiceRedisImpl<A extends AbstractAccount, E extends AbstractEmail> implements RestrictService<A, E> {

    @Autowired
    private RedisDistributedLock redisDistributedLock;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

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
        String lockValue = redisDistributedLock.doLock(emailSendAccount.getId(), 5000);
        try {
            // 检验该账户的规则限制
            if (checkEmailSendAccountRestricts(emailSendAccount)) {
                // 直接添加一个记录
                Boolean isAdded = stringRedisTemplate.opsForZSet().add(getAccountStatisticZSetKey(emailSendAccount.getId()),
                        emailSendTask.getId(), System.currentTimeMillis());
                log.info("add emailSendAccount statistic record:{} result:{}", emailSendTask.getId(), isAdded);
                return true;
            } else {
                emailSendAccount.setEnable(false);
                log.info("set emailSendAccount:{} disable", emailSendAccount.getId());
                return false;
            }
        } finally {
            redisDistributedLock.unlock(emailSendAccount.getId(), lockValue);
        }
    }

    private boolean checkEmailSendAccountRestricts(EmailSendAccount<A> emailSendAccount) {
        long endAt = System.currentTimeMillis();
        String statisticZSetKey = getAccountStatisticZSetKey(emailSendAccount.getId());
        List<EmailSendRestrict> restricts = emailSendAccount.getRestricts();
        for (EmailSendRestrict restrict : restricts) {
            Long count = 0L;
            switch (restrict.getTimeType()) {
                case RANGE_TIME:
                    // 获取范围开始时间
                    long startAt = endAt - restrict.getTimeUnit().toMillis(restrict.getTimeValue());
                    // 统计该时间段数量
                    count = stringRedisTemplate.opsForZSet().count(statisticZSetKey, startAt, endAt);
                    break;
                case POINT_TIME:
                    // 获取距离上个时间点过去了多长时间
                    long afterPointMillis = endAt % restrict.getTimeUnit().toMillis(restrict.getTimeValue());
                    // 统计上个时间点到现在的时间段数量
                    count = stringRedisTemplate.opsForZSet().count(statisticZSetKey, endAt - afterPointMillis, endAt);
                    break;
                default:
                    log.warn("un support restrict timeType:{}", restrict.getTimeType());
                    break;
            }
            // 如超过限制则停止后续判断 注意 是 >= 因为后续会直接添加一个记录
            if (Objects.isNull(count) || count >= restrict.getLimitValue()) {
                return false;
            }
        }
        return true;
    }

    private String getAccountStatisticZSetKey(String emailSendAccountId){
        return StrUtil.format("{}-statistic", emailSendAccountId);
    }
}
