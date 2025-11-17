package com.study.kafka.common.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;

@Builder
public record EmailSendMessage(
        String from,
        String to,
        String subject,
        String body
) {
    public static EmailSendMessage fromJson(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, EmailSendMessage.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("역직렬화 문제 발생", e);
        }
    }

    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("직렬화 문제 발생", e);
        }
    }
}

