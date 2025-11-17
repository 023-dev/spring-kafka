package com.study.kafka.producer.dto;

public record EmailSendRequest(
        String from,
        String to,
        String subject,
        String body
) {
    public EmailSendRequest {
        if (from == null || from.isBlank()) {
            throw new IllegalArgumentException("발신자는 필수입니다");
        }
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("수신자는 필수입니다");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("제목은 필수입니다");
        }
    }
}

