package com.epam.task5.utils;

import com.epam.task5.ExchangeAppException;

@FunctionalInterface
public interface ExchangeAppAction {
    void execute() throws ExchangeAppException;
}
