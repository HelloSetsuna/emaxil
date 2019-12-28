package com.veda.emaxil.core.consumer;

import cn.hutool.core.util.StrUtil;
import com.veda.emaxil.core.EmaxilCore;
import com.veda.emaxil.core.entity.AbstractAccount;
import com.veda.emaxil.core.entity.EmailSendAccount;
import com.veda.emaxil.core.entity.EmailSendTask;
import com.veda.emaxil.core.entity.AbstractEmail;
import com.veda.emaxil.core.sender.AbstractEmailSender;
import com.veda.emaxil.core.service.EmailSendTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import static com.veda.emaxil.core.entity.EmailSendTask.StatusEnum.*;

/**
 * 邮件发送任务 抽象消费者
 * @param <A> 账户
 * @param <E> 邮件
 */
@Slf4j
public abstract class AbstractEmailSendTaskConsumer<A extends AbstractAccount, E extends AbstractEmail> {

    @Autowired
    private EmailSendTaskService<E> emailSendTaskService;

    @Autowired
    private EmaxilCore<A, E> emaxilCore;

    @Autowired
    private AbstractEmailSender<A, E> emailSender;

    /**
     * 消费邮件发送任务 需要自行选择账户 判断能否发送 调用 Sender 发送，并更新邮件发送状态 和 相关统计数据
     * @param emailSendTask 邮件发送任务
     */
    void consume(EmailSendTask<E> emailSendTask) {
        // 检查任务状态
        emailSendTask = emailSendTaskService.select(emailSendTask.getId());
        // 如果邮件已经发送成功 或发送失败 且 超过重试次数 不进行处理
        if (SEND_SUCCESS.equals(emailSendTask.getStatus()) ||
                (SEND_FAILURE.equals(emailSendTask.getStatus()) && emailSendTask.getRetryCount() >= emailSendTask.getRetryLimit())) {
            return;
        }
        // 获取可用账户
        EmailSendAccount<A> sendableEmailSendAccount = getSendableEmailSendAccount(emailSendTask);
        // 开始发送邮件
        for (int i = 0; i < emailSendTask.getRetryLimit() + 1; i++) {
            emailSendTask.setRetryCount(i);
            try {
                // 标记任务状态为 发送中
                emailSendTask.setStatus(SENDING);
                emailSendTaskService.update(emailSendTask);
                // 调用发送实现 进行发送
                emailSender.send(sendableEmailSendAccount.getAccount(), emailSendTask.getEmail());
                // 标记任务状态为 发送成功
                emailSendTask.setStatus(SEND_SUCCESS);
                emailSendTaskService.update(emailSendTask);
                log.info("email:{} send success", emailSendTask.getId());
                break;
            } catch (Exception e) {
                if (i == 0) {
                    log.error(StrUtil.format("email:{} send failure", emailSendTask.getId()), e);
                } else {
                    log.error(StrUtil.format("email:{} retry:{} send failure", emailSendTask.getId(), i), e);
                }
                // 标记任务状态为 发送失败
                emailSendTask.setStatus(SEND_FAILURE);
                emailSendTask.setErrorMessage(e.getMessage());
                emailSendTaskService.update(emailSendTask);
            }
        }

    }

    /**
     * 获取可发送邮件的邮件发送账户
     * @param emailSendTask 邮件发送任务
     * @return EmailSendAccount<A>
     */
    private EmailSendAccount<A> getSendableEmailSendAccount(EmailSendTask<E> emailSendTask){
        boolean canSend = false;
        EmailSendAccount<A> emailSendAccount = null;
        while (!canSend) {
            emailSendAccount = emaxilCore.getEmailSendAccount();
            // 每个节点 同一时刻 一个发送账户只能有一个消费者 判断能否发送
            synchronized (emailSendAccount) {
                // 账户可用
                if (!emailSendAccount.isEnable()) {
                    log.warn("emailSendAccount: {} is disabled", emailSendAccount.getId());
                }
                // 验证限制
                else if(!emaxilCore.getRestrictService().canSend(emailSendAccount, emailSendTask)) {
                    // 标记当前账户为不可用
                    emailSendAccount.setEnable(false);
                    log.warn("emailSendAccount: {} is disabled", emailSendAccount.getId());
                }
                else {
                    // 验证通过
                    canSend = true;
                }
            }
        }
        return emailSendAccount;
    }
}
