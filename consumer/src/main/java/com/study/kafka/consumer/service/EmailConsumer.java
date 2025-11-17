package com.study.kafka.consumer.service;

import com.study.kafka.common.message.EmailSendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailConsumer {

    @KafkaListener(
            topics = "email.send",
            groupId = "email-send-group"
    )
    @RetryableTopic(
            attempts = "5",
            backoff = @Backoff(delay = 1000, multiplier = 2),
            dltTopicSuffix = ".dlt"
    )
    public void consume(String json) {
        var message = EmailSendMessage.fromJson(json);

        // 이메일 발송 로직 (실제 구현 필요)
        log.info("이메일 발송: from={}, to={}, subject={}",
                message.from(), message.to(), message.subject());
        System.out.println("이메일 메시지: " + message);
    }
}

