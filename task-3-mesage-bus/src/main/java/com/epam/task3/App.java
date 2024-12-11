package com.epam.task3;

import com.epam.task3.bus.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.epam.task3.Utils.getUpToNRandom;

@Slf4j
public class App {

    public static void main(String[] args) throws Exception {
        MessageBus messageBus = new MessageBusImpl();

        String[] topics = {
                "Apples", "Oranges", "Bananas", "Grapes", "Strawberries",
                "Mangoes", "Watermelons", "Pineapples", "Peaches", "Pears",
//                "Cherries", "Plums", "Kiwis", "Lemons", "Avocados",
//                "Raspberries", "Blueberries", "Papayas", "Figs", "Lychees"
        };

        int producers = 3;
        int consumers = 5;

        ExecutorService pool = Executors.newFixedThreadPool(producers + consumers);
        ExecutorCompletionService<NamedRunnable> completionService = new ExecutorCompletionService<>(pool);

        Random rnd = new Random();
        List<MessageConsumer> consumerList = new ArrayList<>();
        List<Future<NamedRunnable>> consumerFutures = IntStream.range(0, consumers)
                .mapToObj(i -> new MessageConsumer("Consumer_" + i, messageBus, getUpToNRandom(rnd, topics, 5)))
                .peek(App::logConsumer)
                .peek(consumerList::add)
                .map(c -> completionService.submit(c, c))
                .collect(Collectors.toList());

        List<MessageProducer> producerList = new ArrayList<>();
        List<Future<NamedRunnable>> producerFutures = IntStream.range(0, producers)
                .peek(i -> delay(100))
                .mapToObj(i -> new MessageProducer("Producer_" + i, messageBus, topics))
                .peek(producerList::add)
                .map(p -> completionService.submit(p, p))
                .collect(Collectors.toList());

        try {
            while (!producerFutures.isEmpty()) {
                Future<NamedRunnable> take = completionService.take();
                NamedRunnable namedRunnable = take.get();
                log.debug("{} is done", namedRunnable.getName());
                producerFutures.remove(take);
            }
            consumerFutures.forEach(f -> f.cancel(true));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            log.debug("Shutting down");
            pool.shutdown();
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                pool.shutdownNow();
                if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                    log.warn("Failed to shutdown the pool");
                    System.exit(1);
                }
            }
            log.debug("Pool shut down");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        Map<String, TopicStats> topicStats = Arrays.stream(topics)
                .collect(Collectors.toMap(Function.identity(), TopicStats::new));
        producerList.forEach(p -> p.collectCounts((t, cnt) -> topicStats.get(t).count(p.getName(), cnt)));
        consumerList.forEach(c -> c.collectCounts((t, cnt) -> topicStats.get(t).count(c.getName(), cnt)));
        int w = 14;
        ps.printf("+%s+%s+%s+\n", "-".repeat(w), "-".repeat(w * producers + producers - 1), "-".repeat(w * consumers + consumers - 1));
        ps.printf("|%s|%s|%s|\n", " ".repeat(w), StringUtils.center("Produced", w * producers + producers - 1), StringUtils.center("Consumed", w * consumers + consumers - 1));
        int cols = 1 + producers + consumers;
        String separator = Stream.generate(() -> "-".repeat(w)).limit(cols).collect(Collectors.joining("+", "+", "+"));
        ps.println(separator);
        ps.println("|" + StringUtils.center("Topic", w) +
                Stream.of(producerList, consumerList).flatMap(nrl -> nrl.stream().map(nr -> StringUtils.center(nr.getName(), w)))
                        .collect(Collectors.joining("|", "|", "|")));
        ps.println(separator);
        for (String topic : topics) {
            ps.println("|" + StringUtils.center(topic, w) +
                    Stream.of(producerList, consumerList).flatMap(nrl -> nrl.stream()
                                    .map(nr -> topicStats.get(topic).getCount(nr.getName())
                                            .map(String::valueOf).map(cnt -> StringUtils.leftPad(cnt, w))
                                            .orElse(StringUtils.center("-", w))))
                            .collect(Collectors.joining("|", "|", "|")));
            ps.println(separator);
        }
        log.info("\n{}", baos);
    }

    private static void delay(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void logConsumer(MessageConsumer consumer) {
        log.info("{} created for {}", consumer.getName(), consumer.getTopics());
    }

    private static class TopicStats {
        final String topic;
        final Map<String, Integer> counts;

        public TopicStats(String topic) {
            this.topic = topic;
            this.counts = new HashMap<>();
        }

        public void count(String name, int count) {
            counts.put(name, count);
        }

        public Optional<Integer> getCount(String name) {
            return Optional.ofNullable(counts.get(name));
        }
    }

}
