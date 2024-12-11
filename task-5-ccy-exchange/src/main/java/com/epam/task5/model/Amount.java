package com.epam.task5.model;

import lombok.Value;

import java.math.BigDecimal;
import java.util.Currency;

@Value(staticConstructor = "of")
public class Amount {

    Currency currency;
    BigDecimal value;

    public String getCurrencyCode() {
        return currency.getCurrencyCode();
    }

    public int signum() {
        return value.signum();
    }

    @Override
    public String toString() {
        return value + currency.getSymbol();
    }
}
