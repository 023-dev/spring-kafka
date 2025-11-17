package com.study.kafka.producer.service;

import com.study.kafka.common.message.EmailSendMessage;
import com.study.kafka.producer.dto.EmailSendRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendEmail(final EmailSendRequest request) {
        var message = EmailSendMessage.builder()
                .from(request.from())
                .to(request.to())
                .subject(request.subject())
                .body(request.body())
                .build();

        kafkaTemplate.send("email.send", message.toJson());
    }
}

