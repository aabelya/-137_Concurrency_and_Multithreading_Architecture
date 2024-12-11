package com.epam.task5.utils;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.NoSuchElementException;


public class TicTac {

    Deque<Long> t = new ArrayDeque<>();

    public static TicTac _tic() {
        TicTac ticTac = new TicTac();
        ticTac.tic();
        return ticTac;
    }

    public void tic() {
        t.push(System.nanoTime());
    }

    public long tac() {
        try {
            return System.nanoTime() - t.pop();
        } catch (NoSuchElementException e) {
            return Long.MIN_VALUE;
        }

    }

    public long ticTac() {
        long tac = tac();
        tic();
        return tac;
    }

}
