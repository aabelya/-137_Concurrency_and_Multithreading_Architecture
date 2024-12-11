package com.epam.task2.threads;

import java.util.Collection;

public abstract class AbstractTaskRunnable implements Runnable {

    final Collection<Integer> collection;

    public AbstractTaskRunnable(Collection<Integer> collection) {
        this.collection = collection;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            doTheWork();
        }
    }

    public abstract void doTheWork();
}
