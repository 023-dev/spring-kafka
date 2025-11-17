# Spring Kafka 학습 프로젝트

## 개요

이 프로젝트는 Spring Boot와 Apache Kafka를 사용하여 비동기 메시지 처리 시스템을 구현합니다. 이메일 발송과 SMS 발송을 예제로, Producer와 Consumer를 멀티모듈로 분리하여 독립적으로 배포 가능한 구조를 학습합니다.

### 학습 목표

이 프로젝트를 완료하면 다음을 수행할 수 있습니다:

- Kafka의 기본 개념과 동작 원리 이해
- Spring Kafka를 사용한 Producer/Consumer 구현
- 멀티모듈 프로젝트 구조 설계 및 독립 배포
- 메시지 재시도 및 Dead Letter Topic 처리

### 사전 준비

- JDK 17 이상
- Gradle 7.0 이상
- Kafka 서버 (로컬 또는 원격)
- 기본적인 Spring Boot 지식

---

## Apache Kafka 핵심 개념

### Kafka의 정의

Apache Kafka는 대규모 데이터를 처리하는 분산 이벤트 스트리밍 플랫폼입니다. 메시지 큐를 기반으로 고성능 비동기 처리를 제공합니다.

- 공식 문서: [Apache Kafka](https://kafka.apache.org/)

### 메시지 큐가 제공하는 가치

메시지 큐는 큐 구조로 데이터를 임시 저장하여 다음 이점을 제공합니다:

- **비동기 처리**: 작업 완료를 기다리지 않고 즉시 응답
- **높은 처리량**: 대규모 트래픽 효율적 처리
- **시스템 분리**: Producer와 Consumer 독립 운영
- **확장성**: MSA 환경에서 서비스 간 통신 최적화

---

## 동기 vs 비동기 처리 비교

### 동기 처리 (REST API)

```
사용자 요청 → 서버 처리 → 작업 완료 대기 → 응답
```

모든 작업이 완료될 때까지 사용자가 대기합니다.

### 비동기 처리 (메시지 큐)

```
1. 사용자 요청 (REST API)
2. Producer가 메시지 큐에 전송
3. 사용자에게 즉시 성공 응답
4. Consumer가 메시지 처리
```

메시지 전송 즉시 응답하여 대기 시간을 제거합니다.

---

## Kafka 기본 구성 요소

| 구성 요소 | 역할 |
|---------|------|
| **Producer** | 메시지를 Kafka에 전송 |
| **Consumer** | Kafka의 메시지를 처리 |
| **Topic** | 메시지를 종류별로 구분 |
| **Offset** | Consumer가 읽은 위치 추적 |
| **Consumer Group** | 여러 Consumer를 그룹으로 관리 |

### 기본 동작 흐름

```
1. Producer가 메시지를 Kafka에 전송
2. Kafka가 Topic별로 메시지 저장
3. Consumer가 주기적으로 새 메시지 확인
4. Consumer가 메시지 조회 및 처리
5. Offset을 업데이트하여 처리 위치 기록
```

### Kafka vs 전통적 메시지 큐

**전통적 메시지 큐 (RabbitMQ, SQS)**
- 메시지를 읽으면 큐에서 제거

**Kafka**
- 메시지를 읽어도 유지
- Offset으로 읽은 위치만 기록
- 동일 메시지를 여러 번 읽기 가능
- 메시지를 일정 기간 보관 후 자동 삭제

---

## 프로젝트 구조

이 프로젝트는 Spring Boot 멀티모듈로 구성되어 있습니다.

```
spring-kafka/
├── common/          # 공통 메시지 DTO
│   └── src/main/java/com/study/kafka/message/
│       ├── EmailSendMessage.java
│       └── SmsSendMessage.java
├── producer/        # Kafka Producer (포트 8080)
│   └── src/main/java/com/study/kafka/producer/
│       ├── controller/
│       ├── dto/
│       └── service/
└── consumer/        # Kafka Consumer (포트 8081)
    └── src/main/java/com/study/kafka/consumer/
        └── service/
```

### 멀티모듈 구조의 이점

- Common 모듈로 메시지 DTO 공유
- Producer와 Consumer 독립 배포
- 각 JAR에 Common 코드 포함되어 별도 의존성 불필요

---

## 빠른 시작

### 1. Kafka 서버 실행

```bash
# Kafka 디렉터리로 이동
cd kafka_2.13-4.1.1

# Kafka 서버 시작
bin/kafka-server-start.sh -daemon config/server.properties

# 실행 확인
sudo lsof -i:9092
```

### 2. Topic 생성

```bash
# 이메일 토픽 생성
bin/kafka-topics.sh --bootstrap-server localhost:9092 \
  --create --topic email.send

# SMS 토픽 생성
bin/kafka-topics.sh --bootstrap-server localhost:9092 \
  --create --topic sms.send

# 생성 확인
bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
```

### 3. 애플리케이션 실행

```bash
# Producer 실행 (터미널 1)
./gradlew :producer:bootRun

# Consumer 실행 (터미널 2)
./gradlew :consumer:bootRun
```

### 4. API 테스트

```bash
# 이메일 발송 요청
curl -X POST http://localhost:8080/api/emails/send \
  -H "Content-Type: application/json" \
  -d '{
    "from": "sender@example.com",
    "to": "receiver@example.com",
    "subject": "테스트 제목",
    "body": "테스트 내용"
  }'

# Consumer 로그에서 처리 확인
```

---

## Producer 구현

Producer는 비즈니스 로직에서 발생한 이벤트를 Kafka로 전송합니다.

### 주요 의존성

```gradle
dependencies {
    implementation project(':common')
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.kafka:spring-kafka'
}
```

### 설정 (application.yml)

```yaml
server:
  port: 8080

spring:
  application:
    name: kafka-producer
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
```

### 메시지 DTO (Common 모듈)

```java
package com.study.kafka.message;

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
    public String toJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("직렬화 실패", e);
        }
    }
}
```

### 요청 DTO

```java
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
    }
}
```

### Service

```java
package com.study.kafka.producer.service;

import com.study.kafka.message.EmailSendMessage;
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
```

### Controller

```java
package com.study.kafka.producer.controller;

import com.study.kafka.producer.dto.EmailSendRequest;
import com.study.kafka.producer.service.EmailProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
```

---

## Consumer 구현

Consumer는 Kafka의 메시지를 읽어 실제 작업을 수행합니다.

### 주요 의존성

```gradle
dependencies {
    implementation project(':common')
    implementation 'org.springframework.boot:spring-boot-starter'
    implementation 'org.springframework.kafka:spring-kafka'
    implementation 'org.springframework.retry:spring-retry'
}
```

### 설정 (application.yml)

```yaml
server:
  port: 8081

spring:
  application:
    name: kafka-consumer
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      auto-offset-reset: earliest
```

**auto-offset-reset 옵션**
- `earliest`: Consumer Group 최초 생성 시 Topic의 처음부터 읽기
- `latest`: 최신 메시지부터 읽기

### 기본 Consumer

```java
package com.study.kafka.consumer.service;

import com.study.kafka.message.EmailSendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailConsumer {
    
    @KafkaListener(
        topics = "email.send",
        groupId = "email-send-group"
    )
    public void consume(String json) {
        var message = EmailSendMessage.fromJson(json);
        
        log.info("이메일 발송: from={}, to={}, subject={}", 
                message.from(), message.to(), message.subject());
        
        // 실제 이메일 발송 로직 구현
    }
}
```

---

## 재시도 및 에러 처리

메시지 처리 실패 시 자동 재시도와 Dead Letter Topic을 활용합니다.

### 재시도 전략 적용

```java
package com.study.kafka.consumer.service;

import com.study.kafka.message.EmailSendMessage;
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
        
        // 이메일 발송 로직
        // 실패 시 자동으로 재시도
    }
}
```

**재시도 정책**
- 재시도 횟수: 5회
- 재시도 간격: 1초 → 2초 → 4초 → 8초 → 16초 (2배씩 증가)
- 모든 재시도 실패 시: `email.send.dlt` Topic으로 전송

### DLT 모니터링

```java
package com.study.kafka.consumer.service;

import com.study.kafka.message.EmailSendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailDltConsumer {
    
    @KafkaListener(
        topics = "email.send.dlt",
        groupId = "email-send-dlt-group"
    )
    public void consume(String json) {
        var message = EmailSendMessage.fromJson(json);
        
        log.error("이메일 발송 최종 실패: {}", message);
        
        // 알림 전송 (Slack, 이메일 등)
        // 로그 시스템에 기록
        // 수동 처리를 위한 데이터베이스 저장
    }
}
```

### DLT 처리 방법

| 상황 | 처리 방법 |
|------|----------|
| 일시적 장애 (외부 서버 다운 등) | 장애 복구 후 메시지를 원래 Topic으로 재전송 |
| 영구적 오류 (잘못된 데이터) | 메시지 폐기 후 로그 기록 |
| 데이터 검증 실패 | Producer 검증 로직 보완 |

---

## Kafka CLI 명령어

Kafka는 CLI를 통해 Topic 관리와 메시지 확인이 가능합니다.

### Topic 관리

```bash
# Topic 생성
bin/kafka-topics.sh --bootstrap-server localhost:9092 \
  --create --topic email.send

# Topic 목록 조회
bin/kafka-topics.sh --bootstrap-server localhost:9092 --list

# Topic 상세 조회
bin/kafka-topics.sh --bootstrap-server localhost:9092 \
  --describe --topic email.send

# Topic 삭제
bin/kafka-topics.sh --bootstrap-server localhost:9092 \
  --delete --topic email.send
```

### 메시지 확인

```bash
# Producer 콘솔
bin/kafka-console-producer.sh --bootstrap-server localhost:9092 \
  --topic email.send

# Consumer 콘솔 (처음부터 읽기)
bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 \
  --topic email.send --from-beginning

# Consumer Group 지정
bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 \
  --topic email.send --from-beginning --group email-send-group
```

### Consumer Group 관리

```bash
# Consumer Group 목록
bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list

# Consumer Group 상세 정보 (Offset 확인)
bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --group email-send-group --describe
```

### DLT 메시지 확인

```bash
# DLT Topic 메시지 조회
bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 \
  --topic email.send.dlt --from-beginning
```

---

## 빌드 및 배포

### 전체 빌드

```bash
./gradlew clean build
```

### 개별 모듈 실행

```bash
# Producer 실행
./gradlew :producer:bootRun

# Consumer 실행
./gradlew :consumer:bootRun
```

### JAR 생성 및 배포

```bash
# Producer JAR 생성
./gradlew :producer:bootJar
# 생성 위치: producer/build/libs/producer-0.0.1-SNAPSHOT.jar

# Consumer JAR 생성
./gradlew :consumer:bootJar
# 생성 위치: consumer/build/libs/consumer-0.0.1-SNAPSHOT.jar

# JAR 실행
java -jar producer/build/libs/producer-0.0.1-SNAPSHOT.jar
java -jar consumer/build/libs/consumer-0.0.1-SNAPSHOT.jar
```

각 JAR 파일에는 Common 모듈의 코드가 포함되어 있어 별도의 의존성 설치 없이 독립적으로 실행됩니다.

---

## 학습 요약

### Kafka의 핵심 가치

1. **비동기 처리**로 응답 시간 단축 및 처리량 증가
2. **메시지 영속성**으로 안정적인 데이터 전달 보장
3. **확장 가능한 구조**로 MSA 환경에 최적화
4. **재시도 및 DLT**로 신뢰성 있는 시스템 구축

### 멀티모듈 구조의 이점

- Common 모듈로 메시지 DTO 중복 제거
- Producer와 Consumer 독립 배포 및 확장
- 각 모듈의 책임 명확히 분리

### 주요 학습 내용

- Kafka의 기본 개념 (Producer, Consumer, Topic, Offset)
- Spring Kafka를 사용한 메시지 송수신
- KafkaTemplate과 @KafkaListener 활용
- @RetryableTopic을 통한 재시도 전략
- Dead Letter Topic을 통한 실패 메시지 처리
- 멀티모듈 프로젝트 구성 및 빌드

---

## 참고 자료

- [Apache Kafka 공식 문서](https://kafka.apache.org/documentation/)
- [Spring for Apache Kafka](https://docs.spring.io/spring-kafka/reference/)
- [Kafka Quick Start](https://kafka.apache.org/quickstart)

---

## 부록: Kafka 서버 설치

Kafka 서버를 로컬 환경에 설치하는 방법입니다.

### 사전 요구사항

- JDK 17 이상

### 설치 단계

```bash
# 1. JDK 설치 (Ubuntu)
sudo apt update
sudo apt install openjdk-17-jdk

# 2. Kafka 다운로드
wget https://dlcdn.apache.org/kafka/4.0.0/kafka_2.13-4.1.1.tgz
tar -xzf kafka_2.13-4.1.1.tgz
cd kafka_2.13-4.1.1

# 3. 메모리 설정 (선택사항)
export KAFKA_HEAP_OPTS="-Xmx400m -Xms400m"

# 4. 서버 설정
vi config/server.properties
# advertised.listeners=PLAINTEXT://localhost:9092 확인

# 5. 로그 초기화
KAFKA_CLUSTER_ID="$(bin/kafka-storage.sh random-uuid)"
bin/kafka-storage.sh format --standalone -t $KAFKA_CLUSTER_ID \
  -c config/server.properties

# 6. Kafka 시작
bin/kafka-server-start.sh -daemon config/server.properties

# 7. 실행 확인
sudo lsof -i:9092
```

### Kafka 종료

```bash
bin/kafka-server-stop.sh
```

---



