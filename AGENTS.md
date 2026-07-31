<!-- AGENTS.md 와 CLAUDE.md 는 동일하게 유지됩니다. 한쪽을 수정하면 다른 쪽도 같이 수정하세요. -->
# asset-radar — 작업 메모

## 다음 작업 시작 시 가장 먼저 제안할 것

### Grafana 운영 증거 보강

- `Prometheus + Grafana + Loki + Tempo + Alertmanager` 스택은 구성되어 있으므로, 다음 포트폴리오 보강 작업은 운영 화면 캡처와 runbook 정리가 적합하다.
- demo profile로도 데이터가 흐르므로 외부 API 키 없이 Grafana 캡처를 만들 수 있다.
- 목적: README의 기능 스크린샷 다음 단계로 운영 관측 가능성까지 보여준다.
- 권장 캡처 후보:
  1. Grafana dashboard (`http://localhost:3000`) — API latency, collector count, alert metrics
  2. Prometheus alert rule 화면 (`http://localhost:9090`) — alert rule 로딩 상태
  3. Loki log query 화면 — `service=asset-radar` 기준 collector/API 로그

## 완료된 TODO

### README에 동작 스크린샷 추가

- **완료일**: 2026-05-17 기준 README에 반영됨
- **파일**:
  - `docs/screenshots/dashboard_1.png`
  - `docs/screenshots/dashboard_2.png`
  - `docs/screenshots/analytics.png`
- **삽입 위치**: `## Architecture` 아래 `## Screenshots`

### 실행 환경 분리와 데모 모드

- **완료일**: 2026-05-17
- **프로파일**:
  - `local`: Docker 인프라 + 호스트 Spring Boot
  - `docker`: 전체 Compose 실행 기본값
  - `demo`: 외부 API 수집기를 끄고 synthetic data 생성
  - `prod`: 운영형 환경변수 기반 설정
- **관련 파일**:
  - `src/main/resources/application-local.yml`
  - `src/main/resources/application-docker.yml`
  - `src/main/resources/application-demo.yml`
  - `src/main/resources/application-prod.yml`
  - `docker-compose.demo.yml`
  - `DemoAssetCollector`

### Swagger 예시 응답 추가 정리

- **완료일**: 2026-06-24
- **내용**:
  - 주요 REST API의 성공/에러 Swagger response example 정리
  - 응답 DTO와 도메인 record의 `@Schema` 설명/예시 보강
- **관련 파일**:
  - `src/main/java/com/jean202/assetradar/api/OpenApiExamples.java`
  - `src/main/java/com/jean202/assetradar/api/*Response.java`
  - `src/main/java/com/jean202/assetradar/domain/AssetPrice.java`
  - `src/main/java/com/jean202/assetradar/domain/AssetAnalysis.java`
  - `src/main/java/com/jean202/assetradar/domain/AssetAlert.java`

### 운영 배포 자동화와 secret 주입 방식 확정

- **완료일**: 2026-06-24
- **결정**:
  - GitHub Actions가 API/프론트엔드 이미지를 GHCR에 빌드/푸시
  - 운영 서버는 SSH로 `deploy/docker-compose.prod.yml`을 갱신하고 Docker Compose로 재기동
  - KIS, Alpha Vantage, Finnhub, 알림 webhook, DB 비밀번호는 운영 서버의 `.env.prod`에서 런타임 주입
- **관련 파일**:
  - `.github/workflows/deploy.yml`
  - `deploy/docker-compose.prod.yml`
  - `deploy/.env.prod.example`
  - `deploy/README.md`

### APISIX 게이트웨이 스터디 오버레이 (1~2단계)

- **완료일**: 2026-07-31
- **성격**: 학습용 구성. 제품 기능이 아니다.
- **불변 조건**: `docker-compose.yml`과 애플리케이션 코드는 수정하지 않는다.
  게이트웨이가 없는 상태와 있는 상태를 비교할 수 있어야 도입 비용을 측정할 수 있기 때문이다.
- **실행**:
  - 기준선: `docker compose -f docker-compose.yml -f docker-compose.demo.yml up -d`
  - 게이트웨이 포함: 위 명령에 `-f docker-compose.apisix.yml` 추가
- **포트**: 9080 프록시 / 9180 Admin API / 9091 메트릭 / 2379 etcd. 기존 스택과 충돌 없음.
- **관련 파일**:
  - `docker-compose.apisix.yml`
  - `apisix/config.yaml`
  - `apisix/setup-routes.sh`
  - `apisix/README.md`
- **주의**:
  - `APISIX_ADMIN_KEY`가 없으면 apisix 컨테이너가 뜨지 않는다(의도된 fail-fast).
  - etcd 이미지는 `gcr.io/etcd-development/etcd`를 쓴다. APISIX 문서가 쓰는
    `bitnami/etcd`의 버전 태그는 2026-07 기준 docker.io에서 더 이상 받아지지 않는다.
  - etcd에 볼륨이 없으므로 `down` 하면 라우트가 사라진다. `setup-routes.sh`는
    그래서 `PUT`으로 멱등하게 작성돼 있다.
- **다음 단계**: 3단계(SSE 버퍼링) → 4단계(플러그인) → 5단계(관측 연동) →
  6단계(traffic-split) → 7단계(커스텀 Lua 플러그인). `apisix/README.md` 참고.
