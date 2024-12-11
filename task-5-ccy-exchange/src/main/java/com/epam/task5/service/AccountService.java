package com.epam.task5.service;

import com.epam.task5.model.Account;
import com.epam.task5.persistence.AccountDao;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Currency;
import java.util.List;
import java.util.UUID;

@Slf4j
@AllArgsConstructor
class AccountService {

    private final AccountDao accountDao;

    AccountService() {
        this(new AccountDao());
    }

    Account createAccount(UUID id, Currency currency) {
        log.debug("Creating account {} ({})", id, currency);
        Account account = new Account(id, currency);
        return accountDao.save(account);
    }

    Account getAccount(UUID id) throws AccountNotFoundException {
        log.debug("Getting account {}", id);
        return accountDao.get(id).orElseThrow(() -> new AccountNotFoundException(id.toString()));
    }

    List<Account> getAll() {
        log.debug("Getting all accounts");
        return accountDao.getAll();
    }

    void updateAccount(Account account) {
        log.debug("Updating account {}", account);
        accountDao.save(account);
    }

    boolean deleteAccount(Account account) {
        log.debug("Deleting account {}", account);
        return accountDao.delete(account) == 1;
    }
}
