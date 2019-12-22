package com.veda.emaxil;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 适用于中小型企业的邮件发送服务实现, 在邮件服务商的接口速率限制下最大速率的发送邮件, 支持多活
 * 一般邮件服务商的单个账户邮件发送会有每小时和每天的发送数量限制，而中小型企业在不想为邮件发送付费时
 * 就会采用注册多个账户然后发送时使用不同的账户来解决单个账户的限制问题，但这就需要考虑各种限制的问题
 * 但通常发送效率低下，而此项目就是为了最大限度在邮件发送方的限制规则边缘，最大速率发送邮件。
 *
 * 限制器的选择：
 * 如果只部署单个节点可不使用 redis 相关组件，内部通过Map来计算速率即可，简单高效
 * 如果需要部署多个节点则必须使用 redis 相关组件，使用 有序集合 来实时计算速率相关限制条件
 *
 * 任务队列的选择：
 * 如邮件发送的量很大，或者是多节点部署的时候，则推荐使用 消息队列中间件 如 RabbitMQ 或 Kafka，
 * 默认的生产者消费者的实现是使用 Java 的 Queue 来承载任务的, 这会导致如果节点宕机，相关的任务也就丢失了，
 * 需要通过首次启动时从持久化记录中加载未处理的任务，重新由生产者放入队列重新消费。 在发送前查询出来该任务
 * 对应的数据库持久化的任务信息，检查任务状态，发送成功后修改状态提交事务。
 *
 * 持久化的选择：
 * 持久化邮件信息时可选择多种方式来处理，项目默认使用 MongoDb 来进行邮件信息的持久化，而如果有附件需要持久化，
 * 单节点时可直接将附件存入本地，但多节点时则需要使用 NFS 或其它云盘来实现多个节点 共享文件存储
 *
 *
 * consumer 包下是多线程 邮件发送任务队列 消费者 的多种实现
 * controller 包下是邮件服务对外暴露的调用接口 邮件在 controller 结收后 会调用 producer 生产任务
 * persistence 包下是对于邮件数据的 持久化 和 状态更新 的多种实现
 * producer 包下是调用 persistence 持久化存储后 将任务推送到队列中 供 consumer 消费 的多种实现
 * restrict 包下是对 邮件发送速率限制 和 发送总量限制 的限制器 的多种实现
 * sender 包下是对不同的邮件服务商如 网易、阿里 的企业邮箱等 发送邮件的发送者 的多种实现
 */
@SpringBootApplication
public class EmaxilApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmaxilApplication.class, args);
    }

}
