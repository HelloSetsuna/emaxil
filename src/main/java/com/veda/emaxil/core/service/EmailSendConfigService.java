package com.veda.emaxil.core.service;

import com.veda.emaxil.core.entity.EmailSendConfig;

public interface EmailSendConfigService {

    EmailSendConfig select();

    void update(EmailSendConfig emailSendConfig);
}
