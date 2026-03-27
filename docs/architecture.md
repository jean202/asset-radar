# Architecture

`asset-radar`는 데이터 소스별 수집 주기와 방식이 다른 환경을 하나의 파이프라인으로 정리하는 것을 목표로 한다.

## 현재 설계 방향

- 수집 계층: 자산별 Collector
- 처리 계층: Kafka 기반 이벤트 스트림
- 제공 계층: REST API, SSE, 캐시

## 현재 단계

- 프로젝트 골격과 패키지 구조 정리
- Collector 인터페이스와 기본 진입점 정의
- 운영 구성요소를 `docker-compose.yml`로 먼저 명시
