package com.epam.task1.work;

import java.util.Collection;
import java.util.Map;

public class SumCalculator implements Runnable {

    protected final Map<Integer, Integer> targetMap;
    private final int valuesToAdd;

    public SumCalculator(Map<Integer, Integer> targetMap, int valuesToAdd) {
        this.targetMap = targetMap;
        this.valuesToAdd = valuesToAdd;
    }

    @Override
    public void run() {
        int calculationsCounter = 0;
        int size = 0;
        try {
            while (!Thread.currentThread().isInterrupted() && size < valuesToAdd) {
                Collection<Integer> values = targetMap.values();
                size = values.size();
                long sum = sum(values);
                calculationsCounter++;
                System.out.printf("\rSum of %.2f%% elements calculated: %d\r", 100d * size / valuesToAdd, sum);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } finally {
            System.out.println("\nSumCalculator thread exiting. Sums calculated: " + calculationsCounter);
        }

    }

    protected long sum(Collection<Integer> values) {
        long sum = 0L;
        for (Integer v : values) {
            sum += v;
        }
        return sum;
    }

}
