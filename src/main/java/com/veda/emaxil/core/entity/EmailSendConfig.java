package com.veda.emaxil.core.entity;

import lombok.Data;

@Data
public class EmailSendConfig {
    private Integer threadPoolCoreSize;
    private Integer threadPoolMaxSize;
    private Integer threadPoolQueueCapacity;
}
