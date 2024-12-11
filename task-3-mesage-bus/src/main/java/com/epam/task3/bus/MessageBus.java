package com.epam.task3.bus;

import com.epam.task3.model.Message;

import java.util.UUID;

public  interface MessageBus {
    UUID subscribe(String... topics);

    void send(Message message);

    Message readNextMessage(UUID subscriptionId) throws InterruptedException;

}
