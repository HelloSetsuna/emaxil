package com.veda.emaxil.persistence;

import com.veda.emaxil.context.SendEmailTask;

public interface EmailInterface<E extends EmailInterface> {

    SendEmailTask<E> getSendEmailTask();
}
