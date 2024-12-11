package com.epam.task3.bus;

import com.epam.task3.model.Message;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class MessageBusImpl implements MessageBus {

    private final Queue<MessageWrapper> queue;
    private final Map<String, Set<UUID>> subscriptions;
    private final Object mutex;

    public MessageBusImpl() {
        queue = new LinkedList<>();
        subscriptions = new HashMap<>();
        mutex = this;
    }

    @Override
    public UUID subscribe(String... topics) {
        UUID uuid = UUID.randomUUID();
        Arrays.stream(topics)
                .map(t -> subscriptions.computeIfAbsent(t, s -> new HashSet<>()))
                .forEach(set -> set.add(uuid));
        return uuid;
    }

    @Override
    public void send(Message message) {
        Set<UUID> recipients = Arrays.stream(message.getTopics())
                .map(subscriptions::get)
                .filter(Objects::nonNull)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
        if (recipients.isEmpty()) {
            return;
        }
        synchronized (mutex) {
            queue.add(new MessageWrapper(message, recipients));
            log.trace("Bus queue size: {}", queue.size());
            mutex.notifyAll();
        }
    }

    @Override
    public Message readNextMessage(UUID subscriptionId) throws InterruptedException {
        Message message;
        synchronized (mutex) {
            while ((message = getNextFor(subscriptionId)) == null) {
                mutex.wait();
            }
            return message;
        }
    }

    private Message getNextFor(UUID subscriptionId) {
        for (MessageWrapper messageWrapper : queue) {
            Message message = messageWrapper.getMessage(subscriptionId);
            if (message != null) {
                return message;
            }
        }
        return null;
    }


    private class MessageWrapper {
        Message message;
        Set<UUID> recipients = new HashSet<>();

        private MessageWrapper(Message message, Set<UUID> recipients) {
            this.message = message;
            this.recipients = recipients;
        }

        private Message getMessage(UUID subscriptionId) {
            if (recipients.remove(subscriptionId)) {
                if (recipients.isEmpty()) {
                    queue.remove(this);
                    log.trace("Bus queue size: {}", queue.size());
                }
                return message;
            }
            return null;
        }
    }

}
