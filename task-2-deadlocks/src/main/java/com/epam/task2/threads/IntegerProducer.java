package com.epam.task2.threads;

import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;

public class IntegerProducer extends AbstractTaskRunnable {

    private final Object sumMutex;
    private final Object sqrtMutex;
    private final ThreadLocalRandom random;

    public IntegerProducer(Collection<Integer> collection, Object sumMutex, Object sqrtMutex) {
        super(collection);
        this.sumMutex = sumMutex;
        this.sqrtMutex = sqrtMutex;
        random = ThreadLocalRandom.current();
    }

    @Override
    public void doTheWork() {
        synchronized (sqrtMutex) {
            synchronized (sumMutex) {
                this.collection.add(random.nextInt(10));
            }
        }
    }

}
