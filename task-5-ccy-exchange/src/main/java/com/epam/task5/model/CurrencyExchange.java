package com.epam.task5.model;

import lombok.Value;

import java.util.Currency;

@Value(staticConstructor = "of")
public class CurrencyExchange {

    Currency ccy1;
    Currency ccy2;

}
