# asset-radar

여러 자산의 가격 흐름을 수집하고 비교 분석하기 위한 WebFlux 기반 백엔드 프로젝트입니다.

## 상태

- 현재 상태: 초기 구조 정리 중
- 목표: WebFlux, Kafka, Redis, PostgreSQL을 조합한 이벤트 기반 파이프라인
- 방향: 수집 파이프라인과 문서 구조를 먼저 정리하고 있습니다

## 현재 포함된 내용

- Spring Boot + WebFlux 기본 빌드 설정
- collector / domain / pipeline / api 기본 패키지 구조
- Docker Compose 초안
- 데이터 소스 및 아키텍처 문서

## 문서

- `docs/architecture.md`
- `docs/data-sources.md`
- `docs/decision-log.md`
- `PROJECT_PLAN.md`

## 메모

이 저장소는 공개용 초기 골격입니다. 실제 수집 로직과 Kafka 연동, 시세 분석은 다음 단계에서 계속 확장할 예정입니다.
