package com.veda.emaxil.core;

import com.veda.emaxil.core.entity.AbstractAccount;
import com.veda.emaxil.core.entity.AbstractEmail;
import com.veda.emaxil.core.entity.EmailSendAccount;
import com.veda.emaxil.core.entity.EmailSendConfig;
import com.veda.emaxil.core.restrict.RestrictService;
import com.veda.emaxil.core.service.EmailSendAccountService;
import com.veda.emaxil.core.service.EmailSendConfigService;
import com.veda.emaxil.core.service.EmailSendTaskService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
public class EmaxilCore<A extends AbstractAccount, E extends AbstractEmail> {

    @Getter
    @Autowired
    private RestrictService<A, E> restrictService;

    @Getter
    @Autowired
    private EmailSendConfigService emailSendConfigService;

    @Getter
    @Autowired
    private EmailSendAccountService<A> emailSendAccountService;

    @Getter
    @Autowired
    private EmailSendTaskService<E> emailSendTaskService;

    // 邮件发送配置
    private volatile EmailSendConfig emailSendConfig;
    private ReentrantReadWriteLock emailSendConfigLock = new ReentrantReadWriteLock(true);

    // 邮件发送账户
    private final List<EmailSendAccount<A>> emailSendAccounts = new ArrayList<>();
    private volatile int emailSendAccountCurrentPointIndex = 0;

    // 邮件发送线程池
    @Getter
    private ThreadPoolTaskExecutor emailSendThreadPool;

    // 周期执行线程池
    @Getter
    private ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

    // 清理统计数据的时间间隔 毫秒
    private Long deleteEmailSendAccountStatisticMillisInterval;

    @PostConstruct
    public void initialized(){
        loadEmailSendConfig();
        initEmailSendThreadPool();
        loadEmailSendAccounts();
        autoUpdateEmailSendAccountEnable();
        autoDeleteEmailSendAccountStatistic();
    }

    public EmailSendConfig getEmailSendConfig() {
        try {
            emailSendConfigLock.readLock().lock();
            return emailSendConfig;
        } finally {
            emailSendConfigLock.readLock().unlock();
        }
    }

    /**
     * 获取下一个可用的邮件发送账户
     * 所有的邮件发送账户构成一个圆圈, 每次新的线程需要获取邮件发送账户时
     * 获取的都是 emailSendAccountCurrentPointIndex 指向的下个可用账户
     * 确保多线程发送邮件使用的发送账户大体是均匀分布的
     * @return 可用的邮件发送账户
     */
    public EmailSendAccount<A> getEmailSendAccount(){
        // 如果找不到可用账户 其余未获取可用账户的线程将会卡在此处等待可用账户
        synchronized (emailSendAccounts) {
            EmailSendAccount<A> emailSendAccount = getNextEnableEmailSendAccount();
            if (Objects.nonNull(emailSendAccount)) {
                return emailSendAccount;
            }
            while (true) {
                // 未找到可用的发送账户 立即尝试更新账户状态
                restrictService.updateEmailSendAccountEnable(emailSendAccounts, false);
                emailSendAccount = getNextEnableEmailSendAccount();
                if (Objects.nonNull(emailSendAccount)) {
                    // 如果找到可用的 返回账户
                    return emailSendAccount;
                } else {
                    // 否则 休眠一段时间后重试
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        log.error("waiting enabled email send account interrupted");
                    }
                }
            }

        }
    }

    /**
     * 定期自动更新邮件发送账户的状态
     */
    public void autoUpdateEmailSendAccountEnable() {
        scheduledExecutor.scheduleAtFixedRate(() -> {
            synchronized (emailSendAccounts) {
                restrictService.updateEmailSendAccountEnable(emailSendAccounts, true);
            }
        }, 60, 60, TimeUnit.SECONDS);
    }

    /**
     * 定期自动清理邮件发送账户的统计数据
     */
    public void autoDeleteEmailSendAccountStatistic() {
        scheduledExecutor.scheduleAtFixedRate(() -> {
            synchronized (emailSendAccounts) {
                long beforeTimestamps = System.currentTimeMillis() - deleteEmailSendAccountStatisticMillisInterval;
                restrictService.deleteEmailSendAccountStatistic(emailSendAccounts, beforeTimestamps);
            }
        }, 24, 24, TimeUnit.HOURS);
    }

    private EmailSendAccount<A> getNextEnableEmailSendAccount(){
        int size = emailSendAccounts.size();
        int sizeOut = size + emailSendAccountCurrentPointIndex + 1;
        // 确保只遍历数组一圈
        for (int i = emailSendAccountCurrentPointIndex + 1; i < sizeOut; i++) {
            EmailSendAccount<A> emailSendAccount = emailSendAccounts.get(i < size ? i : i - size);
            // 如果找到可用的直接返回
            if (emailSendAccount.isEnable()) {
                emailSendAccountCurrentPointIndex = i;
                return emailSendAccount;
            }
        }
        return null;
    }


    private void loadEmailSendConfig() {
        try {
            emailSendConfigLock.writeLock().lock();
            emailSendConfig = emailSendConfigService.select();
        } finally {
            emailSendConfigLock.writeLock().unlock();
        }
    }

    private void initEmailSendThreadPool(){
        emailSendThreadPool = new ThreadPoolTaskExecutor();
        emailSendThreadPool.setCorePoolSize(getEmailSendConfig().getThreadPoolCoreSize());
        emailSendThreadPool.setMaxPoolSize(getEmailSendConfig().getThreadPoolMaxSize());
        emailSendThreadPool.setQueueCapacity(getEmailSendConfig().getThreadPoolQueueCapacity());
        emailSendThreadPool.setThreadNamePrefix("emaxil-");
        emailSendThreadPool.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        emailSendThreadPool.initialize();
    }

    private void loadEmailSendAccounts() {
        emailSendAccounts.addAll(emailSendAccountService.selectAllEnabled());
        if (emailSendAccounts.isEmpty()) {
            throw new IllegalStateException("no enabled email send account loaded");
        }
        // 立即检验所有账户的启用状态
        restrictService.updateEmailSendAccountEnable(emailSendAccounts, false);
        // 获取最短的统计时间间隔 * 2
        deleteEmailSendAccountStatisticMillisInterval = emailSendAccounts.stream()
                .map(emailSendAccount -> emailSendAccount.getRestricts().stream()
                        .map(restrict -> restrict.getTimeUnit().toMillis(restrict.getTimeValue()))
                        .max(Long::compare).orElse(0L))
                .max(Long::compare).orElse(24 * 3600 * 1000L) * 2;
        log.info("cal deleteEmailSendAccountStatisticMillisInterval: {}m", deleteEmailSendAccountStatisticMillisInterval / 1000 / 60);
    }


}
