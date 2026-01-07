# Troubleshooting Log (장애 대응)

## RabbitMQ PRECONDITION_FAILED 이슈 해결

### 🔴 문제 상황 (Incident)
배포 후 애플리케이션 기동 시 RabbitMQ 연결이 끊어지며 아래 에러가 반복 발생.
```
Shutdown Signal: channel error; protocol method: #method<channel.close>(reply-code=406, reply-text=PRECONDITION_FAILED - inequivalent arg 'x-message-ttl' ...)
```

### 🔍 원인 분석 (Root Cause)
1.  **인프라(Server):** RabbitMQ 서버에는 `x-message-ttl: 86400000` (24시간)이 설정된 큐가 이미 생성되어 있음.
2.  **코드(Client):** `@RabbitListener` 어노테이션 내부에서 `@QueueBinding`을 사용해 큐를 선언할 때, TTL 설정을 누락함.
3.  **충돌:** 클라이언트는 "TTL 없는 큐"를 요청하고, 서버는 "TTL 있는 큐"를 제시하여 스펙 불일치로 연결 거부.

### ✅ 해결 과정 (Solution)
`StorageEventListener.java`의 코드를 리팩토링하여 설정을 일원화함.

**Before (Problematic Code):**
어노테이션 내부에 큐 설정을 직접 하드코딩하여, 중앙 설정 파일(`RabbitMQConfig`)과 불일치 발생.
```java
@RabbitListener(bindings = @QueueBinding(
    value = @Queue(value = "queue.name", durable = "true"), ... // TTL 설정 누락
))
```

**After (Refactored Code):**
`RabbitMQConfig` 빈(Bean) 설정을 참조하도록 변경하여 설정 단일 진실 공급원(SSOT) 원칙 준수.
```java
@RabbitListener(queues = RabbitMQConfig.STORAGE_DESCRIPTION_QUEUE)
```

### 💡 교훈 (Key Takeaways)
*   메시지 큐 설정은 분산 시스템에서 계약(Contract)과 같으므로, 코드와 인프라 간의 설정 동기화가 필수적이다.
*   설정 정보는 분산시키지 말고 `Configuration` 클래스 한곳에서 관리해야 한다.
