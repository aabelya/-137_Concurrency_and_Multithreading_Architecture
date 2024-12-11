package com.epam.task5.persistence;

import java.nio.file.Path;

public class DataAccessException extends RuntimeException {

    public DataAccessException(String message, Path path, Throwable cause) {
        super(String.format("%s: %s", message, path.getFileName()), cause);
    }

}
