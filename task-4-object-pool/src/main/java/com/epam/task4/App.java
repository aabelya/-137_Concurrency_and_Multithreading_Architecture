package com.epam.task4;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Collection;
import java.util.OptionalDouble;
import java.util.concurrent.*;
import java.util.stream.IntStream;

@Slf4j
public class App {
    public static void main(String[] args) throws Exception {

        BlockingObjectPool objectPool = new BlockingObjectPool(7);

        int N = 15;
        ExecutorService pool = Executors.newFixedThreadPool(N * N);
        ExecutorCompletionService<ObjectWorker> completionService = new ExecutorCompletionService<>(pool);
        ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

        Collection<ObjectWorker> producers = new ConcurrentLinkedQueue<>();
        Collection<ObjectWorker> consumers = new ConcurrentLinkedQueue<>();
        ScheduledFuture<?> statsTask = null;
        try {
            //print stats every second
            statsTask = scheduledExecutor.scheduleAtFixedRate(
                    () -> printStats(producers, consumers), 1L, 1L, TimeUnit.SECONDS);

            for (int i = 0; i <= N; i++) {
                //starting (N - i) producers 40 ms apart, each adding an object every 100-500 ms
                IntStream.range(0, N - i).peek(j -> delay40())
                        .mapToObj(j -> new ObjectWorker(() -> objectPool.take(new Object())))
                        .peek(producers::add)
                        .forEach(p -> completionService.submit(p, p));

                //starting (i) consumers 40 ms apart, each retrieving an object every 100-500 ms
                IntStream.range(0, i).peek(j -> delay40())
                        .mapToObj(j -> new ObjectWorker(objectPool::get))
                        .peek(consumers::add)
                        .forEach(p -> completionService.submit(p, p));
            }

            while (!producers.isEmpty() && !consumers.isEmpty()) {
                Future<ObjectWorker> take = completionService.take();
                producers.remove(take.get());
                consumers.remove(take.get());
            }

        } finally {
            shutdown(pool);
            if (statsTask != null) {
                statsTask.cancel(true);
            }
            shutdown(scheduledExecutor);
            log.debug("Shut down");
        }

    }

    @SneakyThrows
    private static void delay40() {
        Thread.sleep(40);
    }

    private static void printStats(Collection<ObjectWorker> producers, Collection<ObjectWorker> consumers) {
        OptionalDouble producerAvg = producers.stream()
                .flatMapToLong(p -> Arrays.stream(p.getTimings())).average();
        OptionalDouble consumerAvg = consumers.stream()
                .flatMapToLong(p -> Arrays.stream(p.getTimings())).average();
        log.info("{} producers, {} consumers, average 'take' time: {}, average 'get' time: {}",
                String.format("%3s", "" + producers.size()), String.format("%3s", "" + consumers.size()),
                producerAvg.isPresent() ? String.format("%5d ms", TimeUnit.NANOSECONDS.toMillis((long) producerAvg.getAsDouble())) : "   -    ",
                consumerAvg.isPresent() ? String.format("%5d ms", TimeUnit.NANOSECONDS.toMillis((long) consumerAvg.getAsDouble())) : "   -    "
        );
    }

    private static void shutdown(ExecutorService pool) throws InterruptedException {
        pool.shutdown();
        if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
            pool.shutdownNow();
            if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warn("Failed to shutdown pool {}", pool);
            }
        }
    }

    @FunctionalInterface
    interface ObjectWork {
        void work() throws InterruptedException;
    }

    static class ObjectWorker implements Runnable {
        final long[] timings = new long[5];
        final ObjectWork work;
        final ThreadLocalRandom random;
        final long limit;

        int iteration;
        long t1;


        public ObjectWorker(ObjectWork work) {
            this.work = work;
            this.random = ThreadLocalRandom.current();
            this.limit = System.nanoTime() + TimeUnit.SECONDS.toNanos(16); //each worker will run for 16 seconds
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(random.nextLong(200, 300));
                    t1 = System.nanoTime();
                    if (t1 >= limit) {
                        break;
                    }
                    work.work();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    continue;
                }
                timings[iteration++ % timings.length] = System.nanoTime() - t1;
            }
        }

        public long[] getTimings() {
            if (t1 == 0) return new long[0];
            long currentWait = System.nanoTime() - t1;
            long[] res = Arrays.copyOf(timings, Math.min(iteration, timings.length) + 1);
            res[res.length - 1] = currentWait;
            return res;
        }

    }

}
