package com.epam.task2.threads;

import com.epam.task2.model.Result;

import java.util.Collection;

public class SumCalc extends AbstractTaskRunnable {

    private final Result result;
    private final Object sumMutex;
    int lastCollectionSize = 0;

    public SumCalc(Collection<Integer> collection, Result result) {
        super(collection);
        this.result = result;
        this.sumMutex = this;
    }

    @Override
    public void doTheWork() {
        synchronized (sumMutex) {
            synchronized (result) {
                int collectionSize = result.getCollectionSize();
                if (collectionSize == lastCollectionSize) return;
                double sqrt = result.getSqrt();
                int sum = collection.stream().limit(collectionSize).mapToInt(Integer::intValue).sum();
                result.set(collectionSize, sum, sqrt);
                lastCollectionSize = collectionSize;
                result.print();
            }
        }
    }

    public Object getSumMutex() {
        return sumMutex;
    }

}
