package com.epam.task5.service;

import com.epam.task5.ExchangeAppException;
import com.epam.task5.OperationInterruptedException;
import com.epam.task5.model.Account;
import com.epam.task5.model.Amount;
import com.epam.task5.model.IllegalAmountException;
import com.epam.task5.model.InsufficientFundsException;
import com.epam.task5.utils.TicTac;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;

@Slf4j
@AllArgsConstructor
public class BankingService {

    private final AccountService accountService;
    private final CurrencyExchangeService currencyExchangeService;

    public BankingService(CurrencyExchangeService currencyExchangeService) {
        this(new AccountService(), currencyExchangeService);
    }

    private transient final HashSet<UUID> locked = new HashSet<>();
    private transient final List<Long> waitTimes = new ArrayList<>();

    private TicTac lock(UUID id) {
        TicTac t = TicTac._tic();
        long waitTime = 0L;
        synchronized (locked) {
            while (locked.contains(id)) {
                try {
                    t.tic();
                    locked.wait();
                    waitTime += t.tac();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new OperationInterruptedException(e);
                }
            }
            locked.add(id);
        }
        waitTimes.add(waitTime);
        log.trace("lock for {} acquired in {} ns, wait time {} ns", id, t.ticTac(), waitTime);
        return t;
    }

    private void unlock(UUID id, TicTac t) {
        synchronized (locked) {
            if (locked.remove(id)) {
                log.trace("lock for {} released after {} ns", id, t.tac());
                locked.notifyAll();
            }
        }

    }

    public double getAvgWaitTime() {
        return waitTimes.stream().mapToLong(Long::longValue).average().orElse(0);
    }

    public UUID openAccount(Amount balance) throws IllegalAmountException {
        Objects.requireNonNull(balance, "balance is null");
        if (balance.signum() < 0) {
            throw new IllegalAmountException("balance is negative: " + balance);
        }
        Currency currency = balance.getCurrency();
        UUID id = UUID.randomUUID();

        TicTac t = lock(id);
        try {
            log.debug("Opening {} ({}) account with {}{}", id, currency.getCurrencyCode(), balance, currency.getSymbol());
            Account account = accountService.createAccount(id, currency);
            if (balance.signum() > 0) {
                deposit(account, balance);
            }
            return id;
        } finally {
            unlock(id, t);
        }
    }

    public Statement getAccountStatement(UUID id) throws AccountNotFoundException {
        Objects.requireNonNull(id, "id is null");
        TicTac t = lock(id);
        try {
            log.debug("Building {} account statement", id);
            Account account = accountService.getAccount(id);
            Statement.StatementBuilder builder = Statement.builder()
                    .id(account.getId().toString())
                    .currency(account.getMainCurrency())
                    .timestamp(LocalDateTime.now());
            account.getCurrencies().stream().sorted(
                            comparing(ccy -> !ccy.equals(account.getMainCurrency()))
                                    .thenComparing(ccy -> ((Currency) ccy).getCurrencyCode()))
                    .forEach(ccy -> builder.balance(ccy, account.getBalance(ccy).doubleValue()));
            return builder.build();
        } finally {
            unlock(id, t);
        }
    }

    public Amount closeAccount(UUID id) throws ExchangeAppException {
        Objects.requireNonNull(id, "id is null");
        TicTac t = lock(id);
        try {
            log.debug("Closing account {}", id);
            Account account = accountService.getAccount(id);
            Amount amount = withdrawAll(account);
            deleteAccount(account);
            return amount;
        } finally {
            unlock(id, t);
        }
    }

    public boolean deleteAccount(UUID id) throws AccountNotFoundException, AccountCantBeClosedException {
        Objects.requireNonNull(id, "id is null");
        TicTac t = lock(id);
        try {
            Account account = accountService.getAccount(id);
            return deleteAccount(account);
        } finally {
            unlock(id, t);
        }
    }

    private boolean deleteAccount(Account account) throws AccountCantBeClosedException {
        log.debug("Deleting account {}", account.getId());
        boolean hasFunds = account.getCurrencies().stream()
                .map(account::getBalance).anyMatch(Objects::nonNull);
        if (hasFunds) {
            throw new AccountCantBeClosedException("account has funds");
        }
        return accountService.deleteAccount(account);
    }

    public void deposit(UUID id, Amount amount) throws AccountNotFoundException, IllegalAmountException {
        Objects.requireNonNull(id, "id is null");
        Objects.requireNonNull(amount, "amount is null");
        TicTac t = lock(id);
        try {
            Account account = accountService.getAccount(id);
            deposit(account, amount);
        } finally {
            unlock(id, t);
        }
    }

    private void deposit(Account account, Amount amount) throws IllegalAmountException {
        log.debug("Depositing {} to {}", amount, account.getId());
        account.deposit(amount);
        accountService.updateAccount(account);
    }

    public Amount withdraw(UUID id, Amount amount) throws ExchangeAppException {
        Objects.requireNonNull(id, "id is null");
        Objects.requireNonNull(amount, "amount is null");
        TicTac t = lock(id);
        try {
            Account account = accountService.getAccount(id);
            return withdraw(account, amount);
        } finally {
            unlock(id, t);
        }
    }

    public Amount withdrawAll(UUID id) throws ExchangeAppException {
        Objects.requireNonNull(id, "id is null");
        TicTac t = lock(id);
        try {
            Account account = accountService.getAccount(id);
            return withdrawAll(account);
        } finally {
            unlock(id, t);
        }
    }

    private Amount withdrawAll(Account account) throws ExchangeAppException {
        log.debug("Withdrawing all funds from account {}", account.getId());
        Currency mainCurrency = account.getMainCurrency();
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<Currency> currencies = new ArrayList<>(account.getCurrencies());
        for (Currency ccy : currencies) {
            BigDecimal withdrawn = withdraw(account, Amount.of(ccy, account.getBalance(ccy))).getValue();
            totalAmount = totalAmount.add(ccy.equals(mainCurrency) ? withdrawn :
                    currencyExchangeService.convert(ccy, mainCurrency, withdrawn));
        }
        return Amount.of(mainCurrency, totalAmount);
    }

    private Amount withdraw(Account account, Amount amount) throws IllegalAmountException, InsufficientFundsException {
        log.debug("Withdrawing {} from {}", amount, account.getId());
        account.withdraw(amount);
        accountService.updateAccount(account);
        return amount;
    }

    public Amount exchange(UUID id, Amount amount, Currency targetCurrency) throws ExchangeAppException {
        Objects.requireNonNull(id, "id is null");
        Objects.requireNonNull(amount, "amount is null");
        Objects.requireNonNull(targetCurrency, "targetCurrency is null");
        if (amount.getCurrency().equals(targetCurrency)) {
            log.warn("Skipping exchange, source and target currencies are the same: {}", targetCurrency);
            return amount;
        }
        TicTac t = lock(id);
        try {
            log.debug("Exchanging {} to {} for {}", amount, targetCurrency.getSymbol(), id);
            Account account = accountService.getAccount(id);
            Amount withdrawn = withdraw(account, amount);
            BigDecimal convertedValue = currencyExchangeService.convert(withdrawn.getCurrency(), targetCurrency, withdrawn.getValue());
            Amount convertedAmount = Amount.of(targetCurrency, convertedValue);
            deposit(account, convertedAmount);
            return convertedAmount;
        } finally {
            unlock(id, t);
        }
    }

    public Amount transfer(UUID sourceAccountId, UUID targetAccountId, Amount amount) throws ExchangeAppException {
        Objects.requireNonNull(sourceAccountId, "sourceAccountId is null");
        Objects.requireNonNull(targetAccountId, "targetAccountId is null");
        Objects.requireNonNull(amount, "amount is null");
        if (sourceAccountId.equals(targetAccountId)) {
            log.warn("Skipping transfer, source and target accounts are the same: {}", sourceAccountId);
            return amount;
        }

        List<UUID> lockingOrder = Stream.of(sourceAccountId, targetAccountId).sorted().collect(Collectors.toList());
        Map<UUID, TicTac> ts = lockingOrder.stream().collect(Collectors.toMap(Function.identity(), this::lock));
        try {
            log.debug("Transferring {} from {} to {}", amount, sourceAccountId, targetAccountId);
            Account sourceAccount = accountService.getAccount(sourceAccountId);
            Account targetAccount = accountService.getAccount(targetAccountId);
            Currency transferCurrency = amount.getCurrency();

            Amount sourceWithdrawAmount;
            if (sourceAccount.getCurrencies().contains(transferCurrency)) {
                sourceWithdrawAmount = amount;
            } else {
                Currency sourceCurrency = sourceAccount.getMainCurrency();
                BigDecimal sourceWithdrawValue = currencyExchangeService.convert(transferCurrency, sourceCurrency, amount.getValue());
                sourceWithdrawAmount = Amount.of(sourceCurrency, sourceWithdrawValue);
            }
            Amount withdrawn = withdraw(sourceAccount, sourceWithdrawAmount);

            Amount targetDepositAmount;
            if (withdrawn.getCurrency().equals(transferCurrency)) {
                targetDepositAmount = withdrawn;
            } else {
                BigDecimal targetDepositValue = currencyExchangeService.convert(withdrawn.getCurrency(), transferCurrency, withdrawn.getValue());
                targetDepositAmount = Amount.of(transferCurrency, targetDepositValue);
            }
            deposit(targetAccount, targetDepositAmount);
            return amount;
        } finally {
            lockingOrder.forEach(id -> unlock(id, ts.get(id)));
        }

    }

    @Data
    @Builder
    public static class Statement {
        private static final int CCY_COL = 10;
        private static final int BALANCE_COL = 20;

        String id;
        Currency currency;
        LocalDateTime timestamp;
        @Singular
        private final Map<Currency, Double> balances;

        public void print() {
            printTo(System.out);
        }

        public void printTo(PrintStream out) {
            out.println("=".repeat(150));
            out.printf("\tAccount: %s (%s)\n", id, currency.getCurrencyCode());
            out.printf("\tDate: %s\n", timestamp.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)));
            out.print("\tBalance:\n");
            out.printf("\t+%s+%s+\n", "-".repeat(CCY_COL), "-".repeat(BALANCE_COL));
            out.printf("\t|%s|%s|\n", StringUtils.center("Currency", CCY_COL), StringUtils.center("Balance", 20));
            out.printf("\t+%s+%s+\n", "-".repeat(CCY_COL), "-".repeat(20));
            balances.forEach((ccy, val) -> out.printf("\t|%s|%s|\n",
                    StringUtils.rightPad(ccy.getCurrencyCode(), CCY_COL), StringUtils.leftPad(String.format("%.3f", val), 20)));
            out.printf("\t+%s+%s+\n", "-".repeat(CCY_COL), "-".repeat(20));
            out.println("=".repeat(150));
        }

    }
}
