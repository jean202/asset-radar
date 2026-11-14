# 야간 작업 명세 — infra 계층 Testcontainers 통합 테스트

이 문서는 무인 실행되는 에이전트가 그대로 따라야 하는 작업 명세다.
사람이 개입하지 않으므로, 여기 적힌 종료 조건과 금지 사항을 그대로 지킨다.

## 목표

`infra` 패키지의 Redis / Postgres / Kafka 어댑터를 **실제 컨테이너**에 붙여 검증하는
통합 테스트를 작성한다. 현재 이 계층은 테스트가 전혀 없고, 유일한 통합 테스트인
`PipelineIntegrationTest`는 환경변수 게이트에 막혀 CI에서 한 번도 실행된 적이 없다.

## 환경 전제 (검증 완료 — 바꾸지 말 것)

2026-08-19에 이 환경에서 실제로 확인한 사항이다.

1. **작업 위치는 `~/portfolio/projects/asset-radar-night`** (worktree, 브랜치 `feat/testcontainers-infra-tests`).
   `settings.gradle.kts`가 `includeBuild("../webhook-notify")`로 형제 디렉토리를 참조하므로,
   worktree를 `portfolio/projects/` 밖에 두면 **빌드 자체가 실패한다.** 위치를 옮기지 말 것.

2. **`api.version` 시스템 프로퍼티가 반드시 필요하다.**
   Docker Engine 29.x는 `MinAPIVersion=1.40`이라 API 1.32로 붙는 docker-java 기본 협상을
   400으로 거부한다. `build.gradle.kts`의 test task에 이미 설정돼 있다.
   이 설정을 지우면 모든 Testcontainers 테스트가 `Could not find a valid Docker environment`로 죽는다.
   환경변수 `DOCKER_API_VERSION`만으로는 동작하지 않는다 — 시스템 프로퍼티가 결정적이다.

3. 필요한 이미지는 이미 로컬에 받아져 있다: `redis:7-alpine`, `postgres:16-alpine`, `confluentinc/cp-kafka:8.1.2`.
   **새 이미지를 추가로 받지 말 것** (야간에 네트워크가 끊기면 그대로 멈춘다).

4. 기동 확인 완료: Redis 왕복 테스트 4개 통과(7.2초), Kafka produce/consume + Postgres R2DBC 스모크 통과(9초).

## 기준선

작업 시작 시점의 `./gradlew test` 결과 — **tests=140, skipped=4, failures=0, errors=0**.
이 숫자는 줄어들 수 없다. 늘기만 해야 한다.

## 작업 대상 (우선순위 순)

이미 완료된 것은 건드리지 않는다:
- [x] `RedisAssetPriceCache` + `RedisLatestAssetPriceReader` → `RedisAssetPriceRoundTripIntegrationTest`
- [x] 컨테이너 스모크 → `ContainerSmokeTest`

### 1순위 — Redis 어댑터 나머지 (왕복 검증)

쓰기 어댑터와 읽기 어댑터가 키 규약을 공유하므로 **반드시 짝지어** 같은 컨테이너에서 검증한다.
키 포맷 불일치는 단위 테스트로는 절대 잡히지 않는다.

- [x] `RedisAssetAnalysisCache` + `RedisLatestAssetAnalysisReader`
- [x] `RedisAssetAlertCache` + `RedisLatestAssetAlertReader`

각 쌍마다 최소: 왕복 1건, 필터 조건, 같은 키 덮어쓰기, TTL 적용.

### 2순위 — Postgres 리더 (실제 SQL 검증)

`src/main/resources/schema.sql`을 컨테이너에 적용한 뒤, 리포지토리로 데이터를 넣고 리더로 조회한다.
SQL 문법 오류와 컬럼 매핑 오류를 잡는 것이 목적이다.

- [x] `AssetPriceHistoryRepository` + `PostgresAssetPriceHistoryReader`
- [x] `AssetAnalysisHistoryRepository` + `PostgresAssetAnalysisHistoryReader`
- [x] `AssetAlertHistoryRepository` + `PostgresAssetAlertHistoryReader`
- [x] `PostgresAssetStatisticsReader` (이동평균/변동성 집계 SQL)
- [x] `PostgresAssetCompareWindowReader` (127줄, 가장 복잡한 SQL — 경계 조건 포함)
- [x] `PostgresDashboardHistoryMetricsReader`

경계 조건을 반드시 포함한다: 빈 결과, 단일 행, 윈도우 경계 바깥 데이터, NULL 등락률.

### 3순위 — Kafka 퍼블리셔

- [x] `KafkaAssetPricePublisher` — 발행된 레코드의 키/토픽/역직렬화 결과 검증
- [x] `KafkaAssetAnalysisPublisher` — 동일

### 4순위 — PipelineIntegrationTest 전환

- [x] `@EnabledIfEnvironmentVariable(INTEGRATION_TEST)` 게이트를 제거하고 Testcontainers로 전환
- [x] 수동 `docker compose up` 없이 `./gradlew test`만으로 실행되게 한다
- [x] `.github/workflows/ci.yml`에서 별도 설정 없이 돌아가는지 확인 (GitHub Actions 러너에는 Docker가 있다)

## 작업 방식

- **한 번에 하나씩.** 테스트 하나 작성 → 실행 → 통과 확인 → 커밋. 여러 개를 몰아서 쓰지 않는다.
- 커밋 메시지는 기존 규약을 따른다 (`test: ...`, `refactor: ...`).
- 컨테이너는 클래스당 `@Container static`으로 재사용한다. 테스트마다 새로 띄우면 8시간이 부팅으로 다 간다.
- 테스트 간 격리는 컨테이너 재기동이 아니라 데이터 초기화(`flushAll`, `TRUNCATE`)로 한다.
- 기존 테스트 스타일을 따른다: AssertJ, 한국어 주석은 "왜"를 설명할 때만.

## 종료 조건

다음을 **모두** 만족하면 완료다.

1. `./gradlew test`가 통과한다 (failures=0, errors=0).
2. 위 체크리스트의 1~3순위 항목이 모두 체크된다.
3. `skipped` 개수가 0이다 (4순위 전환 완료 시).
4. 작업 내역이 커밋되어 있고, worktree에 커밋되지 않은 변경이 없다.

## 금지 사항

- **`main` 브랜치를 건드리지 않는다.** 머지도 하지 않는다. 사람이 아침에 리뷰한다.
- **프로덕션 코드를 고치지 않는다.** 테스트가 실패하면 그것은 발견이지 고칠 대상이 아니다.
  실패를 발견하면 테스트를 `@Disabled`로 막지 말고, `FINDINGS.md`에 증상과 재현 방법을 기록한 뒤
  다음 항목으로 넘어간다. 프로덕션 버그 수정은 사람의 판단이 필요하다.
  (단, 테스트 코드 자체의 오류는 당연히 고친다.)
- **외부 API를 호출하지 않는다.** Upbit/Binance/KIS/Alpha Vantage/Finnhub 수집기는 대상이 아니다.
- **새 의존성을 추가하지 않는다.** 이미 추가된 Testcontainers 모듈로 충분하다.
- **`~/.testcontainers.properties`를 수정하지 않는다.** 과거에 여기 고정된 전략 설정 때문에 문제가 있었다.
- 커버리지 도구(JaCoCo) 도입 같은 범위 확장을 하지 않는다.

## 막혔을 때

같은 항목에서 **3회 연속 실패하면 그 항목을 건너뛴다.** 붙잡고 늘어지지 않는다.
건너뛴 항목과 이유를 `FINDINGS.md`에 적고 다음으로 넘어간다.
컨테이너가 아예 안 뜨는 상황(Docker 데몬 종료 등)이면 즉시 중단하고 기록만 남긴다.
