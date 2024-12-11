package com.epam.task3.bus;

import com.epam.task3.model.Message;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

import static com.epam.task3.Utils.getUpToNRandom;

@Data
@ToString(onlyExplicitlyIncluded = true)
@Slf4j
public class MessageProducer implements NamedRunnable {

    private static int MAX_MESSAGE_COUNT = 10;
    private static long MIN_THROTTLE = 100;
    private static long MAX_THROTTLE = 5000;

    @ToString.Include
    private final String name;
    private final MessageBus messageBus;
    private final ThreadLocalRandom random;
    @ToString.Include
    private final String[] topics;
    private final int[] messageCounts;
    private int i;

    public MessageProducer(String name, MessageBus messageBus, String... topics) {
        this.name = name;
        this.messageBus = messageBus;
        this.random = ThreadLocalRandom.current();
        this.topics = topics;
        this.messageCounts = new int[this.topics.length];
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted() && i < MAX_MESSAGE_COUNT) {
            try {
                Thread.sleep(random.nextLong(MIN_THROTTLE, MAX_THROTTLE));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            Message msg = new Message(this,
                    String.format("Hello from '%s' number %d_", name, i++),
                    getUpToNRandom(random, topics, 3));
            incrementCounts(msg.getTopics());
            log.debug("{} sending to {}: {} ", msg.getSender().getName(), msg.getTopics(), msg.getPayload());
            messageBus.send(msg);
        }
    }

    private void incrementCounts(String[] messageTopics) {
        List<String> list = Arrays.asList(this.topics);
        Arrays.stream(messageTopics).mapToInt(list::indexOf).forEach(i -> this.messageCounts[i]++);
    }

    public void collectCounts(BiConsumer<String, Integer> consumer) {
        IntStream.range(0, topics.length).forEach(i -> consumer.accept(topics[i], messageCounts[i]));
    }

}
