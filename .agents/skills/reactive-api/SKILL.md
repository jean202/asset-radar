---
name: reactive-api
description: asset-radar에 새 리액티브 API 또는 데이터 수집기를 추가한다. Spring WebFlux 패턴.
argument-hint: "[기능 설명 - 예: 환율 실시간 수집기 추가]"
---

## 리액티브 API/수집기 워크플로우

대상: **$ARGUMENTS**

### 프로젝트 정보
- Spring Boot WebFlux (Kotlin Gradle)
- 리액티브 스트리밍 기반
- 기존 수집기: Upbit WebSocket (코인), Gold API (금)

### 구현 패턴

#### 새 수집기(Collector) 추가
1. `collector/` 패키지에 새 Collector 클래스 생성
2. WebSocket 또는 polling 방식 구현
3. `pipeline/`에 데이터 변환 로직 추가
4. `domain/`에 엔티티 정의

#### 새 API 엔드포인트 추가
1. `api/` 패키지에 Router/Handler 추가
2. 스트리밍 응답은 `Flux<ServerSentEvent>` 사용
3. 대시보드 API: `/api/dashboard/`
4. 비교 API: `/api/compare/`

### 기술 스택
- Reactor (Mono/Flux)
- WebClient (외부 API 호출)
- SSE (Server-Sent Events) 실시간 스트리밍
