package com.epam.task5;

import com.epam.task5.model.CurrencyExchange;
import com.epam.task5.service.BankingService;
import com.epam.task5.service.CurrencyExchangeService;
import com.epam.task5.utils.RandomActionGenerator;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.IntStream;

@Slf4j
public class App {

    public static void main(String[] args) throws Exception {

        CurrencyExchangeService currencyExchangeService = initCurrencyExchangeService();
        BankingService bankingService = new BankingService(currencyExchangeService);

        RandomActionGenerator actionGenerator = new RandomActionGenerator(bankingService, currencyExchangeService);
        List<Integer> actionCounters = Collections.synchronizedList(new ArrayList<>());

        int n = 1000;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        ExecutorCompletionService<Integer> completionService = new ExecutorCompletionService<Integer>(pool);
        IntStream.range(0, n)
                .forEach(i -> completionService.submit(new Worker(actionGenerator, actionCounters::add), i));

        try {
            //run for 10 seconds
            Future<Integer> poll = completionService.poll(10, TimeUnit.SECONDS);
            if (poll != null) {
                poll.get(); //check for execution exceptions
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            log.info("Shutting down");
            pool.shutdownNow();
            if (pool.awaitTermination(10, TimeUnit.SECONDS)) {
                log.info("Pool shut down");
            } else {
                log.error("Failed to shutdown the pool");
            }
            log.info("Avg account wait time {} ms", String.format("%.3f", bankingService.getAvgWaitTime() / 1000 / 1000));
            log.info("Avg exchange rate write wait time {} ms", String.format("%.3f", currencyExchangeService.getAvgWriteWaitTime() / 1000 / 1000));
            log.info("Write was locked by another thread {} times", currencyExchangeService.getWriteLockedCounter());
            log.info("Avg exchange rate read wait time {} ms", String.format("%.3f", currencyExchangeService.getAvgReadWaitTime() / 1000 / 1000));
            log.info("Read was locked by another thread {} times", currencyExchangeService.getReadLockedCounter());
            log.info("Avg action count performed by a thread {}", String.format("%.3f", actionCounters.stream().mapToInt(Integer::intValue).average().orElse(0d)));
        }

    }

    @Slf4j
    private static class Worker implements Runnable {

        private RandomActionGenerator actionGenerator;
        private Consumer<Integer> actionCounterConsumer;

        public Worker(RandomActionGenerator actionGenerator, Consumer<Integer> actionCounterConsumer) {
            this.actionGenerator = actionGenerator;
            this.actionCounterConsumer = actionCounterConsumer;
        }

        @Override
        public void run() {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            int count = 0;
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    actionGenerator.getRandomAction(rnd, log).execute();
                } catch (ExchangeAppException e) {
                    log.error("Encountered exception: {}", e.toString());
                }
                count++;
            }
            actionCounterConsumer.accept(count);
        }
    }

    @SneakyThrows
    private static CurrencyExchangeService initCurrencyExchangeService() {
        Currency usd = Currency.getInstance("USD");
        Currency eur = Currency.getInstance("EUR");
        Currency jpy = Currency.getInstance("JPY");
        Currency gbp = Currency.getInstance("GBP");
        Currency pln = Currency.getInstance("PLN");

        HashMap<CurrencyExchange, Double> initialRates = new HashMap<>() {{
            put(CurrencyExchange.of(usd, eur), 0.95);
            put(CurrencyExchange.of(usd, jpy), 149.98);
            put(CurrencyExchange.of(usd, gbp), 0.79);
            put(CurrencyExchange.of(usd, pln), 4.04);

            put(CurrencyExchange.of(eur, usd), 1.05);
            put(CurrencyExchange.of(eur, jpy), 158.16);
            put(CurrencyExchange.of(eur, gbp), 0.83);
            put(CurrencyExchange.of(eur, pln), 4.27);

            put(CurrencyExchange.of(jpy, usd), 0.0067);
            put(CurrencyExchange.of(jpy, eur), 0.0063);
            put(CurrencyExchange.of(jpy, gbp), 0.0052);
            put(CurrencyExchange.of(jpy, pln), 0.027);

            put(CurrencyExchange.of(gbp, usd), 1.27);
            put(CurrencyExchange.of(gbp, eur), 1.21);
            put(CurrencyExchange.of(gbp, jpy), 191.07);
            put(CurrencyExchange.of(gbp, pln), 5.14);
        }};

        return new CurrencyExchangeService(initialRates);
    }

}
