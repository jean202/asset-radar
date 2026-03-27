# Decision Log

## ADR-001

- 결정: Spring Boot + WebFlux를 기본 런타임으로 사용한다.
- 이유: 실시간/준실시간 수집과 스트림 처리 흐름을 일관되게 가져가기 쉽다.

## ADR-002

- 결정: Kafka, Redis, PostgreSQL, Grafana를 로컬 인프라 기준점으로 둔다.
- 이유: 메시징, 캐시, 이력 저장, 관측성을 초기에 분리해 두는 편이 이후 구조 설명에 유리하다.
