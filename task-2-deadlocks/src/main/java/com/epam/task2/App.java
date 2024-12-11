package com.epam.task2;

import com.epam.task2.threads.EuclideanCalc;
import com.epam.task2.threads.IntegerProducer;
import com.epam.task2.threads.SumCalc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.*;

public class App {

    public static void main(String[] args) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(3);
        CompletionService<Integer> service = runWorkflow(pool);

        try {
            //run for 60 seconds
            Future<Integer> poll = service.poll(60, TimeUnit.SECONDS);
            if (poll != null) {
                poll.get(); //check for execution exceptions
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            System.out.println("\nShutting down");
            pool.shutdownNow();
            if (pool.awaitTermination(10, TimeUnit.SECONDS)) System.out.println("Pool shut down");
            else System.err.println("Failed to shutdown the pool");
        }
    }

    private static CompletionService<Integer> runWorkflow(ExecutorService pool) {
        CompletionService<Integer> service = new ExecutorCompletionService<>(pool);

        Collection<Integer> collection = new ArrayList<>();
        EuclideanCalc sqrtCalc = new EuclideanCalc(collection);
        SumCalc sumCalc = new SumCalc(collection, sqrtCalc.getResult());
        IntegerProducer integerProducer = new IntegerProducer(collection, sumCalc.getSumMutex(), sqrtCalc.getSqrtMutex());

        service.submit(integerProducer, 1);
        service.submit(sumCalc, 1);
        service.submit(sqrtCalc, 1);
        return service;
    }













}
