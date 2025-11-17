package com.study.kafka.producer.controller;

import com.study.kafka.producer.dto.EmailSendRequest;
import com.study.kafka.producer.service.EmailProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/emails")
public class EmailController {
    private final EmailProducer emailProducer;

    @PostMapping("/send")
    public ResponseEntity<String> sendEmail(@RequestBody final EmailSendRequest request) {
        emailProducer.sendEmail(request);
        return ResponseEntity.ok("이메일 발송 요청 완료");
    }
}

