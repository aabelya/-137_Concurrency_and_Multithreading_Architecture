package com.epam.task5.service;

import com.epam.task5.model.Account;
import com.epam.task5.model.Amount;
import com.epam.task5.model.IllegalAmountException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BankingServiceTest {

    @Mock
    AccountService accountService;

    @Mock
    CurrencyExchangeService currencyExchangeService;

    @InjectMocks
    BankingService sut;

    @Test
    void openAccount() throws Exception {
        Currency ccy = Currency.getInstance("USD");
        BigDecimal balance = new BigDecimal("50.00");
        Amount amount = Amount.of(ccy, balance);

        Account account = Mockito.mock(Account.class);
        ArgumentCaptor<UUID> idCaptor = ArgumentCaptor.forClass(UUID.class);
        when(accountService.createAccount(idCaptor.capture(), eq(ccy))).thenReturn(account);

        UUID actual = sut.openAccount(amount);
        verify(account).deposit(amount);
        assertEquals(idCaptor.getValue(), actual);
    }

    @Test
    void openAccountShouldThrowIllegalDepositAmountException() {
        Currency ccy = Currency.getInstance("USD");
        BigDecimal value = new BigDecimal("-50.00");
        Amount balance = Amount.of(ccy, value);

        IllegalAmountException actual = assertThrows(IllegalAmountException.class, () -> sut.openAccount(balance));
        assertTrue(actual.getMessage().contains("balance is negative"));
        assertTrue(actual.getMessage().contains(balance.toString()));
    }

    @Test
    void getAccountStatement() throws Exception {
        Currency usd = Currency.getInstance("USD");
        Currency jpy = Currency.getInstance("JPY");
        Currency gbp = Currency.getInstance("GBP");

        UUID uuid = Mockito.mock(UUID.class);
        String uuidStr = "acc_id";
        when(uuid.toString()).thenReturn(uuidStr);
        Account account = Mockito.mock(Account.class);
        when(account.getId()).thenReturn(uuid);
        when(account.getMainCurrency()).thenReturn(usd);
        when(account.getCurrencies()).thenReturn(Set.of(jpy, gbp, usd));
        when(account.getBalance(any(Currency.class)))
                .thenReturn(new BigDecimal("1"), new BigDecimal("2"), new BigDecimal("3"));

        when(accountService.getAccount(uuid)).thenReturn(account);

        BankingService.Statement actual = sut.getAccountStatement(uuid);
        assertEquals(actual.getId(), uuidStr);
        assertEquals(actual.getCurrency(), usd);

        Map<Currency,Double> expected = new LinkedHashMap<>() {{
            put(usd, 1d);
            put(gbp, 2d);
            put(jpy, 3d);
        }};
        var iterator = actual.getBalances().entrySet().iterator();
        expected.forEach((ccy, val) -> {
            assertTrue(iterator.hasNext());
            Map.Entry<Currency, Double> entry = iterator.next();
            assertEquals(entry.getKey(), ccy);
            assertEquals(entry.getValue(), val);
        });
        assertDoesNotThrow(actual::print);
    }

    @Test
    void getAccountStatementShouldThrowAccountNotFoundException() throws Exception {
        UUID uuid = Mockito.mock(UUID.class);
        AccountNotFoundException expected = new AccountNotFoundException();
        when(accountService.getAccount(uuid)).thenThrow(expected);

        AccountNotFoundException actual = assertThrows(AccountNotFoundException.class, () -> sut.getAccountStatement(uuid));
        assertEquals(expected, actual);
    }

    @Test
    void deleteAccount() {
    }

    @Test
    void deposit() {
    }

    @Test
    void withdraw() {
    }

    @Test
    void exchange() {
    }

    @Test
    void transfer() {
    }
}