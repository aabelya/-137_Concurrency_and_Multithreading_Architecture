package com.epam.task4;

import java.util.ArrayList;
import java.util.List;

/**
 * Pool that block when it has not any items or it full
 */
public class BlockingObjectPool {

    private final List<Object> backingList;
    private final int capacity;

    /**
     * Creates filled pool of passed size
     *
     * @param size of pool
     */
    public BlockingObjectPool(int size) {
        this.capacity = size;
        this.backingList = new ArrayList<>(this.capacity);
    }

    /**
     * Gets object from pool or blocks if pool is empty
     *
     * @return object from pool
     */
    public Object get() throws InterruptedException {
        synchronized (backingList) {
            while (backingList.isEmpty()) {
                backingList.wait();
            }
            Object get = backingList.remove(0);
            backingList.notifyAll();
            return get;
        }
    }

    /**
     * Puts object to pool or blocks if pool is full
     *
     * @param object to be taken back to pool
     */
    public void take(Object object) throws InterruptedException {
        synchronized (backingList) {
            while (backingList.size() == capacity) {
                backingList.wait();
            }
            if (backingList.size() > capacity) {
                throw new IllegalStateException("pool overflow");
            }
            backingList.add(object);
            backingList.notifyAll();
        }
    }
}

