package com.veda.emaxil.persistence;

import com.veda.emaxil.context.SendAccount;

public interface AccountInterface<A extends AccountInterface> {

    SendAccount<A> getSendAccount();
}
