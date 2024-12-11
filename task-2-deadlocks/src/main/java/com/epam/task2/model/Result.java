package com.epam.task2.model;

public class Result {

    private int collectionSize;
    private Integer sum;
    private Double sqrt;

    public synchronized void set(int collectionSize, Integer sum, Double sqrt) {
        this.collectionSize = collectionSize;
        this.sum = sum;
        this.sqrt = sqrt;
    }

    public synchronized void print() {
        System.out.printf("Elements: %d, sum: %d, Euclidean norm: %.5f\n", collectionSize, sum, sqrt);
    }

    public int getCollectionSize() {
        return collectionSize;
    }

    public Integer getSum() {
        return sum;
    }

    public Double getSqrt() {
        return sqrt;
    }

}
