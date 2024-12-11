package com.epam.task5.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

import static java.util.Optional.ofNullable;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Account implements Serializable {

    private static final long serialVersionUID = 7918823056372748239L;

    private static final Currency DEFAULT_CCY = Currency.getInstance("USD");

    @EqualsAndHashCode.Include
    private final UUID id;

    @ToString.Exclude
    private final Map<Currency, BigDecimal> funds;
    @NonNull
    private Currency mainCurrency;

    public Account(UUID id) {
        this(id, DEFAULT_CCY);
    }

    public Account(UUID id, Currency currency) {
        Objects.requireNonNull(id, "id is null");
        Objects.requireNonNull(currency, "currency is null");
        this.id = id;
        this.mainCurrency = currency;
        this.funds = new HashMap<>();
    }

    public void deposit(Amount amount) throws IllegalAmountException {
        Currency currency = amount.getCurrency();
        BigDecimal value = amount.getValue();
        validateAmountValue(value);
        funds.compute(currency, (ccy, val) -> ofNullable(val).map(v -> v.add(value)).orElse(value));
    }

    public void withdraw(Amount amount) throws IllegalAmountException, InsufficientFundsException {
        Currency currency = amount.getCurrency();
        BigDecimal value = amount.getValue();
        validateAmountValue(value);
        BigDecimal newVal = ofNullable(funds.get(currency))
                .map(val -> val.subtract(value)).filter(val -> val.signum() >= 0)
                .orElseThrow(InsufficientFundsException::new);
        if (newVal.signum() > 0) {
            funds.put(currency, newVal);
        } else {
            funds.remove(currency);
        }
    }

    public BigDecimal getBalance(Currency currency) {
        return this.funds.get(currency);
    }

    public Set<Currency> getCurrencies() {
        return this.funds.keySet();
    }

    private void validateAmountValue(BigDecimal amount) throws IllegalAmountException {
        if (amount.signum() <= 0) {
            throw new IllegalAmountException("amount is not positive: " + amount);
        }
    }
}
