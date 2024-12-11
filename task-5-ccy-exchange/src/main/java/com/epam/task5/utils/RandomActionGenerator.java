package com.epam.task5.utils;

import com.epam.task5.ExchangeAppException;
import com.epam.task5.model.Amount;
import com.epam.task5.service.BankingService;
import com.epam.task5.service.CurrencyExchangeService;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;


public class RandomActionGenerator {

    private static final int LIMIT_ACCOUNTS = 5;

    private final BankingService bankingService;
    private final CurrencyExchangeService exchangeService;
    private final List<Currency> currencies;
    private final List<BiFunction<Random, Logger, ExchangeAppAction>> actions;
    private final List<UUID> uuids = Collections.synchronizedList(new ArrayList<>());

    public RandomActionGenerator(BankingService bankingService, CurrencyExchangeService exchangeService) {
        this.bankingService = bankingService;
        this.exchangeService = exchangeService;
        this.currencies = new ArrayList<>(exchangeService.currencies()); ;
        //actions with probabilities
        HashMap<BiFunction<Random, Logger, ExchangeAppAction>, Integer> library = new HashMap<>() {{
            put((rnd, log) -> () -> changeExchangeRate(rnd, log), 30);
            put((rnd, log) -> () -> readExchangeRate(rnd, log), 30);
            put((rnd, log) -> () -> openAccount(rnd, log), 15);
            put((rnd, log) -> () -> deleteAccount(rnd, log), 5);
            put((rnd, log) -> () -> closeAccount(rnd, log), 7);
            put((rnd, log) -> () -> deposit(rnd, log), 30);
            put((rnd, log) -> () -> withdraw(rnd, log), 20);
            put((rnd, log) -> () -> withdrawAll(rnd, log), 10);
            put((rnd, log) -> () -> exchange(rnd, log), 45);
            put((rnd, log) -> () -> transfer(rnd, log), 45);
        }};
        actions = library.entrySet().stream()
                .flatMap(e -> Stream.generate(e::getKey).limit(e.getValue())).collect(Collectors.toList());
    }

    public ExchangeAppAction getRandomAction(Random rnd, Logger log) {
        return actions.get(rnd.nextInt(actions.size())).apply(rnd, log);
    }

    public void changeExchangeRate(Random rnd, Logger log) throws ExchangeAppException {
        Currency ccy1 = randomCurrency(rnd);
        Currency ccy2 = randomCurrency(rnd);
        double exchangeRate = exchangeService.getExchangeRate(ccy1, ccy2);
        double newRandomExchangeRate = exchangeRate + exchangeRate * rnd.nextGaussian() / 100;
        log.info("Setting {}/{} exchange rate to {}", ccy1, ccy2, newRandomExchangeRate);
        exchangeService.setExchangeRate(ccy1, ccy2, newRandomExchangeRate);
    }

    public void readExchangeRate(Random rnd, Logger log) throws ExchangeAppException {
        Currency ccy1 = randomCurrency(rnd);
        Currency ccy2 = randomCurrency(rnd);
        log.info("Reading {}/{} exchange rate", ccy1, ccy2);
        log.info("Read {}/{} exchange rate: {}", ccy1, ccy2, exchangeService.getExchangeRate(ccy1, ccy2));
    }

    public void openAccount(Random rnd, Logger log) throws ExchangeAppException {
        if (uuids.size() >= LIMIT_ACCOUNTS) {
            log.warn("Can't open new account: account limit reached");
            return;
        }
        Amount balance = Amount.of(randomCurrency(rnd), randomValue(rnd));
        log.info("Opening {} account, balance: {}", balance.getCurrencyCode(), balance);
        UUID uuid = bankingService.openAccount(balance);
        uuids.add(uuid);
        log.info("Opened {} account: {}", balance.getCurrencyCode(), uuid);
        printAccountStatement(uuid, log);
    }

    public void printAccountStatement(UUID uuid, Logger log) throws ExchangeAppException {
        BankingService.Statement accountStatement = bankingService.getAccountStatement(uuid);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        accountStatement.printTo(new PrintStream(baos));
        log.info("\n{}", baos);
    }

    public void deleteAccount(Random rnd, Logger log) throws ExchangeAppException {
        try {
            UUID uuid = randomAccountUuid(rnd);
            log.info("Deleting account {}", uuid);
            printAccountStatement(uuid, log);
            log.info("Account {} deleted: {}", uuid, bankingService.deleteAccount(uuid));
            uuids.remove(uuid);
        } catch (NotEnoughAccountsException e) {
            log.warn("Can't delete random account: not enough accounts");
        }
    }

    public void closeAccount(Random rnd, Logger log) throws ExchangeAppException {
        try {
            UUID uuid = randomAccountUuid(rnd);
            log.info("Closing account {}", uuid);
            printAccountStatement(uuid, log);
            log.info("Account {} closed: {}", uuid, bankingService.closeAccount(uuid));
            uuids.remove(uuid);
        } catch (NotEnoughAccountsException e) {
            log.warn("Can't close random account: not enough accounts");
        }
    }

    public void deposit(Random rnd, Logger log) throws ExchangeAppException {
        try {
            UUID uuid = randomAccountUuid(rnd);
            Amount amount = randomAmount(rnd);

            log.info("Depositing {} to {}", amount, uuid);
            printAccountStatement(uuid, log);
            bankingService.deposit(uuid, amount);
            log.info("Deposited {} to {}", amount, uuid);
            printAccountStatement(uuid, log);
        } catch (NotEnoughAccountsException e) {
            log.warn("Can't deposit to random account: not enough accounts");
        }
    }

    public void withdraw(Random rnd, Logger log) throws ExchangeAppException {
        try {
            UUID uuid = randomAccountUuid(rnd);
            Amount amount = randomAmount(rnd);

            log.info("Withdrawing {} from {}", amount, uuid);
            printAccountStatement(uuid, log);
            Amount withdrawn = bankingService.withdraw(uuid, amount);
            log.info("Withdrew {} from {}", withdrawn, uuid);
            printAccountStatement(uuid, log);
        } catch (NotEnoughAccountsException e) {
            log.warn("Can't withdraw from random account: not enough accounts");
        }
    }

    public void withdrawAll(Random rnd, Logger log) throws ExchangeAppException {
        try {
            UUID uuid = randomAccountUuid(rnd);
            log.info("Withdrawing all funds from account {}", uuid);
            printAccountStatement(uuid, log);
            Amount withdrawn = bankingService.withdrawAll(uuid);
            log.info("Withdrew {} from {}", withdrawn, uuid);
            printAccountStatement(uuid, log);
        } catch (NotEnoughAccountsException e) {
            log.warn("Can't withdraw all from random account: not enough accounts");
        }
    }

    public void exchange(Random rnd, Logger log) throws ExchangeAppException {
        try {
            UUID uuid = randomAccountUuid(rnd);
            Amount amount = randomAmount(rnd);
            Currency targetCurrency = randomCurrency(rnd);

            log.info("Exchanging {} to {} for account {}", amount, targetCurrency.getSymbol(), uuid);
            printAccountStatement(uuid, log);
            Amount exchanged = bankingService.exchange(uuid, amount, targetCurrency);
            log.info("Exchanged {} to {} for account {}", amount, exchanged, uuid);
            printAccountStatement(uuid, log);
        } catch (NotEnoughAccountsException e) {
            log.warn("Can't exchange on from random account: not enough accounts");
        }
    }

    public void transfer(Random rnd, Logger log) throws ExchangeAppException {
        try {
            UUID sourceAccount = randomAccountUuid(rnd);
            UUID targetAccount = randomAccountUuid(rnd);
            Amount amount = randomAmount(rnd);

            log.info("Transferring {} from {} to {}", amount, sourceAccount, targetAccount);
            printAccountStatement(sourceAccount, log);
            printAccountStatement(targetAccount, log);
            Amount transferred = bankingService.transfer(sourceAccount, targetAccount, amount);
            log.info("Transferred {} from {} to {}", transferred, sourceAccount, targetAccount);
            printAccountStatement(sourceAccount, log);
            printAccountStatement(targetAccount, log);
        } catch (NotEnoughAccountsException e) {
            log.warn("Can't transfer between random accounts: not enough accounts");
        }
    }

    private Amount randomAmount(Random rnd) {
        return Amount.of(randomCurrency(rnd), randomValue(rnd));
    }

    private Currency randomCurrency(Random rnd) {
        return currencies.get(rnd.nextInt(currencies.size()));
    }

    private BigDecimal randomValue(Random rnd) {
        int significant = rnd.nextInt(6) + 1;
        int decimal = rnd.nextInt(2) + 1;
        BigDecimal value = new BigDecimal(String.format("%s.%s",
                IntStream.generate(() -> rnd.nextInt(10)).limit(significant).mapToObj(String::valueOf).collect(Collectors.joining()),
                IntStream.generate(() -> rnd.nextInt(10)).limit(decimal).mapToObj(String::valueOf).collect(Collectors.joining())));
        return randomlyNotPositive(rnd, value);
    }

    private UUID randomAccountUuid(Random rnd) throws NotEnoughAccountsException {
        if (uuids.isEmpty()) {
            throw new NotEnoughAccountsException();
        }
        return uuids.get(rnd.nextInt(uuids.size()));
    }

    private BigDecimal randomlyNotPositive(Random rnd, BigDecimal value) {
        //80% positive, 10% - 0, 10% negative
        int signum = new BigDecimal(8 - rnd.nextInt(10)).signum();
        return value.multiply(new BigDecimal(signum));
    }

    private class NotEnoughAccountsException extends Exception {
        public NotEnoughAccountsException() {
        }
    }
}
