package com.epam.task5.persistence;

import com.epam.task5.model.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class AccountDaoTest {

    @TempDir
    Path dataDir;

    AccountDao sut;

    @BeforeEach
    void setUp() {
        sut = new AccountDao(dataDir);
    }

    @Test
    void shouldSaveAndGetAccount() {
        UUID id = UUID.randomUUID();
        assertFalse(sut.get(id).isPresent());

        Account acc = new Account(id);
        sut.save(acc);
        Optional<Account> actual = sut.get(id);
        assertTrue(actual.isPresent());

        assertNotSame(acc, actual.get());
        assertEquals(acc, actual.get());
    }

    @Test
    void shouldSaveAllAndGetAllAccount() {
        List<Account> before = sut.getAll();
        assertEquals(before.size(), 0);

        List<Account> accounts = Stream.generate(() -> new Account(UUID.randomUUID()))
                .limit(3).collect(Collectors.toList());
        sut.saveAll(accounts);
        List<Account> actual = sut.getAll();

        assertEquals(accounts.size(), actual.size());
        assertTrue(actual.containsAll(accounts));
    }

    @Test
    void shouldDeleteAccount() {
        Account acc = new Account(UUID.randomUUID());
        sut.save(acc);
        assertEquals(1, sut.delete(acc));
        assertEquals(0, sut.delete(acc));
    }
}