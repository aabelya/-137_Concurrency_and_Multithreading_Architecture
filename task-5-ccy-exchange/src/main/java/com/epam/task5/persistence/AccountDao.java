package com.epam.task5.persistence;

import com.epam.task5.model.Account;
import com.sun.nio.file.ExtendedOpenOption;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.filefilter.RegexFileFilter;

import java.io.*;
import java.nio.file.*;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class AccountDao implements Dao<Account, UUID> {

    private static final String DIR = "./data";
    private static final String EXT = "acc";
    private static final String UUID_REGEX = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
    private static final RegexFileFilter FILE_FILTER = new RegexFileFilter(String.format("^%s.%s$", UUID_REGEX, EXT));

    private final Path dir;

    public AccountDao() {
        this(Paths.get(DIR));
    }

    AccountDao(Path dir) {
        this.dir = dir;
    }

    @Override
    public boolean exists(UUID id) {
        log.debug("Checking if {} exists", id);
        return getAccountPath(id).isPresent();
    }

    @Override
    public Optional<Account> get(UUID id) {
        log.debug("Getting account {}", id);
        return getAccountPath(id).map(this::readFrom);
    }

    @Override
    public List<Account> getAll() {
        log.debug("Getting all accounts");
        if (!dirExists()) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(FILE_FILTER::matches)
                    .map(this::readFrom).collect(Collectors.toList());
        } catch (IOException e) {
            throw new DataAccessException("getAll", dir, e);
        }
    }


    private Account readFrom(Path path) {
        log.debug("Reading from {}", dir.relativize(path));
        try (InputStream is = Files.newInputStream(path, StandardOpenOption.SYNC,
                // this will blow up if two or more threads will try to access file at the same time
                ExtendedOpenOption.NOSHARE_READ, ExtendedOpenOption.NOSHARE_WRITE, ExtendedOpenOption.NOSHARE_DELETE)) {
            ObjectInputStream ois = new ObjectInputStream(is);
            Object result = ois.readObject();
            return (Account) result;
        } catch (IOException | ClassNotFoundException e) {
            throw new DataAccessException("readFrom", path, e);
        }

    }

    @Override
    public Account save(Account account) {
        log.debug("Saving account {}", account);
        ensureDirExists();
        write(account);
        return account;
    }


    private void write(Account account) {
        Path path = toFilePath(account);
        log.debug("Writing to {}", dir.relativize(path));
        try (OutputStream os = Files.newOutputStream(path, StandardOpenOption.SYNC, StandardOpenOption.CREATE,
                // this will blow up if two or more threads will try to access file at the same time
                ExtendedOpenOption.NOSHARE_READ, ExtendedOpenOption.NOSHARE_WRITE, ExtendedOpenOption.NOSHARE_DELETE)) {
            ObjectOutputStream oos = new ObjectOutputStream(os);
            oos.writeObject(account);
        } catch (IOException e) {
            throw new DataAccessException("write", path, e);
        }
    }

    @Override
    public Collection<Account> saveAll(Collection<Account> accounts) {
        log.debug("Saving {} accounts", accounts.size());
        ensureDirExists();
        accounts.forEach(this::write);
        return accounts;
    }

    @Override
    public int delete(Account account) {
        log.debug("Deleting account {}", account);
        Optional<Path> accountPath = getAccountPath(account);
        try {
            return accountPath.isPresent() && Files.deleteIfExists(accountPath.get()) ? 1 : 0;
        } catch (IOException e) {
            throw new DataAccessException("delete", accountPath.orElse(null), e);
        }
    }

    private void ensureDirExists() {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new DataAccessException("ensureDirExists", dir, e);
        }
    }

    private boolean dirExists() {
        return Files.exists(dir) && Files.isDirectory(dir);
    }

    private Optional<Path> getAccountPath(Account account) {
        return getAccountPath(account.getId());
    }

    private Optional<Path> getAccountPath(UUID id) {
        Path path = toFilePath(id);
        return Optional.of(path)
                .filter(Files::exists)
                .filter(Files::isRegularFile);
    }

    private Path toFilePath(Account account) {
        return toFilePath(account.getId());
    }

    private Path toFilePath(UUID id) {
        return dir.resolve(toFileName(id));
    }

    private String toFileName(UUID id) {
        return String.format("%s.%s", id.toString(), EXT);
    }

}
