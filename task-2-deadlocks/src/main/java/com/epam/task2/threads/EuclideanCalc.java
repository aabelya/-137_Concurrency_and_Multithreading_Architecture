package com.epam.task2.threads;

import com.epam.task2.model.Result;

import java.util.Collection;

public class EuclideanCalc extends AbstractTaskRunnable {

    private final Result result;
    private final Object sqrtMutex;
    int lastCollectionSize = 0;

    public EuclideanCalc(Collection<Integer> collection) {
        super(collection);
        this.sqrtMutex = this;
        this.result = new Result();
    }

    @Override
    public void doTheWork() {
        synchronized (sqrtMutex) {
            if (lastCollectionSize == collection.size()) return;
            double sqrt = Math.sqrt(collection.stream()
                    .mapToInt(Integer::intValue)
                    .map(i -> i * i).sum());
            lastCollectionSize = collection.size();
            result.set(lastCollectionSize, null, sqrt);
        }
    }

    public Object getSqrtMutex() {
        return sqrtMutex;
    }

    public Result getResult() {
        return result;
    }
}
