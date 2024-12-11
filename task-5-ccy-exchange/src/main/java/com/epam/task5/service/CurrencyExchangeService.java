package com.epam.task5.service;

import com.epam.task5.model.CurrencyExchange;
import com.epam.task5.utils.TicTac;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class CurrencyExchangeService {

    private final HashMap<CurrencyExchange, BigDecimal> rates;
    private final HashMap<CurrencyExchange, ReentrantReadWriteLock> locks;

    private transient final List<Long> writeWaits = new ArrayList<>();
    private transient final List<Long> readWaits = new ArrayList<>();
    private transient final AtomicLong writeLockedCounter = new AtomicLong();
    private transient final AtomicLong readLockedCounter = new AtomicLong();

    public CurrencyExchangeService(HashMap<CurrencyExchange, Double> initialRates) {
        this.rates = new HashMap<>();
        this.locks = new HashMap<>();
        initRates(initialRates);
    }

    private void initRates(HashMap<CurrencyExchange, Double> initialRates) {
        initialRates.forEach((key, rate) -> this.rates.put(key, BigDecimal.valueOf(rate)));
    }

    public void setExchangeRate(Currency ccy1, Currency ccy2, double rate) throws IllegalExchangeRateException {
        Objects.requireNonNull(ccy1, "ccy1 is null");
        Objects.requireNonNull(ccy2, "ccy2 is null");
        if (ccy1.equals(ccy2)) {
            throw new IllegalExchangeRateException("same currency: " + ccy1);
        }
        if (rate <= 0) {
            throw new IllegalExchangeRateException("rate is not positive: " + rate);
        }
        CurrencyExchange key = CurrencyExchange.of(ccy1, ccy2);
        TicTac t = lockWrite(key);
        try {
            BigDecimal oldRate = rates.put(key, BigDecimal.valueOf(rate));
            log.debug("Setting {}/{} exchange rate from {} to {}", ccy1, ccy2, oldRate, rate);
        } finally {
            unlockWrite(key, t);
        }
    }

    public double getExchangeRate(Currency ccy1, Currency ccy2) throws ExchangeRateNotFoundException {
        Objects.requireNonNull(ccy1, "ccy1 is null");
        Objects.requireNonNull(ccy2, "ccy2 is null");
        if (ccy1.equals(ccy2)) {
            return 1d;
        }

        CurrencyExchange key = CurrencyExchange.of(ccy1, ccy2);
        TicTac t = lockRead(key);
        try {
            log.debug("Getting {}/{} exchange rate: {}", ccy1, ccy2, rates.get(key));
            return Optional.ofNullable(rates.get(key))
                    .map(BigDecimal::doubleValue)
                    .orElseThrow(() -> new ExchangeRateNotFoundException(ccy1, ccy2));
        } finally {
            unlockRead(key, t);
        }
    }

    public BigDecimal convert(Currency sourceCurrency, Currency targetCurrency, BigDecimal amount) throws ExchangeRateNotFoundException {
        Objects.requireNonNull(sourceCurrency, "sourceCurrency is null");
        Objects.requireNonNull(targetCurrency, "targetCurrency is null");
        Objects.requireNonNull(amount, "amount is null");
        if (sourceCurrency.equals(targetCurrency)) {
            return amount;
        }
        CurrencyExchange key = CurrencyExchange.of(sourceCurrency, targetCurrency);
        TicTac t = lockRead(key);
        try {
            log.debug("Converting {} from {} to {} with rate {}", amount, sourceCurrency, targetCurrency, rates.get(key));
            return Optional.ofNullable(rates.get(key))
                    .map(amount::multiply)
                    .orElseThrow(() -> new ExchangeRateNotFoundException(sourceCurrency, targetCurrency));
        } finally {
            unlockRead(key, t);
        }
    }

    private TicTac lockRead(CurrencyExchange key) {
        ReentrantReadWriteLock lock = getReadWriteLock(key);
        TicTac t = TicTac._tic();
        if (!lock.readLock().tryLock()) {
            readLockedCounter.incrementAndGet();
            lock.readLock().lock();
        }
        long ns = t.ticTac();
        readWaits.add(ns);
        log.trace("read lock for {} acquired in {} ns", key, ns);
        return t;
    }

    private void unlockRead(CurrencyExchange key, TicTac t) {
        ReentrantReadWriteLock lock = getReadWriteLock(key);
        lock.readLock().unlock();
        log.trace("read lock for {} released after {} ns", key, t.tac());
    }

    private ReentrantReadWriteLock getReadWriteLock(CurrencyExchange key) {
        synchronized (locks) {
            return locks.computeIfAbsent(key, r -> new ReentrantReadWriteLock());
        }
    }

    private TicTac lockWrite(CurrencyExchange key) {
        ReentrantReadWriteLock lock = getReadWriteLock(key);
        TicTac t = TicTac._tic();
        if (!lock.writeLock().tryLock()) {
            writeLockedCounter.incrementAndGet();
            lock.writeLock().lock();
        }
        long ns = t.ticTac();
        writeWaits.add(ns);
        log.trace("write lock for {} acquired in {} ns", key, ns);
        return t;
    }

    private void unlockWrite(CurrencyExchange key, TicTac t) {
        ReentrantReadWriteLock lock = getReadWriteLock(key);
        lock.writeLock().unlock();
        log.trace("write lock for {} released after {} ns", key, t.tac());
    }

    public double getAvgWriteWaitTime() {
        return writeWaits.stream().mapToLong(Long::longValue).average().orElse(0d);
    }

    public double getAvgReadWaitTime() {
        return readWaits.stream().mapToLong(Long::longValue).average().orElse(0d);
    }

    public long getReadLockedCounter() {
        return readLockedCounter.get();
    }

    public long getWriteLockedCounter() {
        return writeLockedCounter.get();
    }

    public Set<Currency> currencies() {
        return rates.keySet().stream()
                .flatMap(key -> Stream.of(key.getCcy1(), key.getCcy2())).collect(Collectors.toSet());
    }
}
