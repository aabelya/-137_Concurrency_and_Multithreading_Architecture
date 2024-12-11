package com.epam.task3.model;

import com.epam.task3.bus.MessageProducer;
import lombok.Data;
import lombok.ToString;

@Data
@ToString()
public class Message {
    private final MessageProducer sender;
    private final String[] topics;
    private final Object payload;

    public Message(MessageProducer sender, Object payload, String... topics) {
        this.sender = sender;
        this.payload = payload;
        this.topics = topics;
    }

}
