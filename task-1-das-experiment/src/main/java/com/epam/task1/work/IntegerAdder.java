package com.epam.task1.work;

import java.util.Map;
import java.util.Random;

public class IntegerAdder implements Runnable {

    public static final double DATA_SIZE_GB = 1;

    private final Map<Integer, Integer> targetMap;
    private final int valuesToAdd;
    private final Random random;

    public IntegerAdder(Map<Integer, Integer> targetMap) {
        this.targetMap = targetMap;
        this.valuesToAdd = (int) (DATA_SIZE_GB * Math.pow(1024, 3) / (2 * Integer.SIZE));
        this.random = new Random(); //not using ThreadLocalRandom due to java 6 compatibility
    }

    @Override
    public void run() {
        int size;
        while (!Thread.currentThread().isInterrupted() && (size = targetMap.size()) < valuesToAdd) {
            targetMap.put(size, random.nextInt(100));
        }
        System.out.printf("Adder thread exiting. Elements added: %.2f%%\n", 100d * targetMap.size() / valuesToAdd);
    }

    public int getValuesToAdd() {
        return valuesToAdd;
    }
}
