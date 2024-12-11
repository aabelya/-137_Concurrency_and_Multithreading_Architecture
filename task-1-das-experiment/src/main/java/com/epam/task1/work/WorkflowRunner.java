package com.epam.task1.work;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class WorkflowRunner {


    public static final String ADDER_WORK = "Adder";
    public static final String SUM_CALC_WORK = "SumCalculator";

    public static List<ThreadWork> runWorkflow(Map<Integer, Integer> targetMap) {
        IntegerAdder adder = new IntegerAdder(targetMap);
        return runWorkflow(adder, new SumCalculator(targetMap, adder.getValuesToAdd()));
    }

    public static List<ThreadWork> runWorkflow(IntegerAdder adder, SumCalculator sumCalculator) {
        ExecutorService exec = Executors.newFixedThreadPool(2);
        try {
            CompletionService<Integer> service = new ExecutorCompletionService<Integer>(exec);
            HashMap<Future<Integer>, ThreadWork> futureToWork = new HashMap<Future<Integer>, ThreadWork>();
            ThreadWork adderWork = new ThreadWork(ADDER_WORK, adder);
            futureToWork.put(service.submit(adderWork, 1), adderWork);
            ThreadWork sumCalcWork = new ThreadWork(SUM_CALC_WORK, sumCalculator);
            futureToWork.put(service.submit(sumCalcWork, 1), sumCalcWork);
            try {
                while (!futureToWork.isEmpty()) {
                    Future<Integer> future = service.take();
                    future.get();
                    futureToWork.remove(future).setSuccessful();
                }
            } catch (ExecutionException e) {
                System.out.println("Caught " + e.getCause());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return Arrays.asList(adderWork, sumCalcWork);
        } finally {
            try {
                exec.shutdownNow();
                if (!exec.awaitTermination(10, TimeUnit.SECONDS)) {
                    System.err.println("failed to shutdown executor");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();

            }
        }
    }


}
