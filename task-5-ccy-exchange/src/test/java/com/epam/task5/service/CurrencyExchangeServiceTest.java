package com.epam.task5.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.*;

class CurrencyExchangeServiceTest {

    private final CurrencyExchangeService sut = new CurrencyExchangeService();

    @Test
    void shouldConvert() throws Exception {
        Currency ccy1 = Currency.getInstance("USD");
        Currency ccy2 = Currency.getInstance("GBP");
        double rate = 0.79;
        BigDecimal amount = new BigDecimal("2");

        sut.setExchangeRate(ccy1, ccy2, rate);
        assertEquals(amount.multiply(BigDecimal.valueOf(rate)), sut.convert(ccy1, ccy2, amount));
    }

    @Test
    void shouldThrowIllegalExchangeRateException() {
        Currency ccy1 = Currency.getInstance("GBP");
        Currency ccy2 = Currency.getInstance("USD");

        {
            IllegalExchangeRateException actual = assertThrows(IllegalExchangeRateException.class,
                    () -> sut.setExchangeRate(ccy1, ccy1, 0.79));
            assertTrue(actual.getMessage().contains("same currency"));
            assertTrue(actual.getMessage().contains(ccy1.toString()));
        }

        {
            double rate = 0d;
            IllegalExchangeRateException actual = assertThrows(IllegalExchangeRateException.class,
                    () -> sut.setExchangeRate(ccy1, ccy2, rate));
            assertTrue(actual.getMessage().contains("rate is not positive"));
            assertTrue(actual.getMessage().contains("" + rate));
        }

        {
            double rate = -1d;
            IllegalExchangeRateException actual = assertThrows(IllegalExchangeRateException.class,
                    () -> sut.setExchangeRate(ccy1, ccy2, rate));
            assertTrue(actual.getMessage().contains("rate is not positive"));
            assertTrue(actual.getMessage().contains("" + rate));
        }
    }

    @Test
    void shouldThrowExchangeRateNotFoundException() {
        Currency ccy1 = Currency.getInstance("GBP");
        Currency ccy2 = Currency.getInstance("USD");
        BigDecimal amount = new BigDecimal("2");

        ExchangeRateNotFoundException actual = assertThrows(ExchangeRateNotFoundException.class,
                () -> sut.convert(ccy1, ccy2, amount));
        assertTrue(actual.getMessage().contains(ccy1.toString()));
        assertTrue(actual.getMessage().contains(ccy2.toString()));
    }

    @Test
    void shouldThrowNpe() {
        Currency ccy = Currency.getInstance("GBP");
        double rate = 1d;
        BigDecimal amount = new BigDecimal("1");

        assertThrows(NullPointerException.class, () -> sut.setExchangeRate(null, ccy, rate));
        assertThrows(NullPointerException.class, () -> sut.setExchangeRate(ccy, null, rate));

        assertThrows(NullPointerException.class, () -> sut.convert(null, ccy, amount));
        assertThrows(NullPointerException.class, () -> sut.convert(ccy, null, amount));
        assertThrows(NullPointerException.class, () -> sut.convert(ccy, ccy, null));
    }
}