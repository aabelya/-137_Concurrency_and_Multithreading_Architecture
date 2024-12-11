package com.epam.task3.bus;

import com.epam.task3.model.Message;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

@Data
@ToString
@Slf4j
public class MessageConsumer implements NamedRunnable {

    private final String name;
    @ToString.Exclude
    private final MessageBus messageBus;
    private final String[] topics;
    private final int[] messageCounts;
    private final UUID subscriptionId;

    public MessageConsumer(String name, MessageBus messageBus, String... topics) {
        this.name = name;
        this.messageBus = messageBus;
        this.topics = topics;
        this.messageCounts = new int[this.topics.length];
        subscriptionId = messageBus.subscribe(topics);

    }

    @Override
    public void run() {
        while (true) {
            try {
                Message message = messageBus.readNextMessage(subscriptionId);
                incrementCounts(message.getTopics());
                log.info("{} {} received from {} sent to {}: {}", name, topics, message.getSender().getName(), message.getTopics(), message.getPayload());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void incrementCounts(String[] messageTopics) {
        List<String> list = Arrays.asList(this.topics);
        Arrays.stream(messageTopics).mapToInt(list::indexOf).filter(i -> i >= 0).forEach(i -> this.messageCounts[i]++);
    }

    public void collectCounts(BiConsumer<String, Integer> consumer) {
        IntStream.range(0, topics.length).forEach(i -> consumer.accept(topics[i], messageCounts[i]));
    }
}
