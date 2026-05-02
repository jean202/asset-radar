# asset-radar — Claude 작업 메모

## 다음 작업 시작 시 가장 먼저 제안할 것

### 알림 채널 실동작 확인

- `webhook-notify` 연동이 이미 들어가 있으므로, 다음 `asset-radar` 작업을 시작하면 **다른 기능 작업보다 먼저** Slack/Discord 알림 실동작 검증을 제안할 것
- 아직 웹훅 env가 비어 있으면 아래 값을 먼저 준비하도록 안내할 것
  - `ASSET_RADAR_ALERT_SLACK_WEBHOOK_URL`
  - `ASSET_RADAR_ALERT_DISCORD_WEBHOOK_URL`
  - `ASSET_RADAR_ALERT_NOTIFIER_SLACK_ENABLED=true`
  - `ASSET_RADAR_ALERT_NOTIFIER_DISCORD_ENABLED=true`
- 기본 운영 기준도 함께 상기할 것
  - Slack: `CRITICAL`만 전송
  - Discord: `WARN` 이상 전송
  - `INFO`: 외부 채널 전송 없음
- 사용자가 아직 웹훅을 넣지 않았다면, 다음 액션으로는 기능 개발보다 먼저
  1. env 설정
  2. `asset-radar` 실행
  3. 실제 알림 발생 시나리오 1회 검증
  를 제안할 것
- 사용자가 “다음 뭐 하지?” 또는 일반적인 `asset-radar` 작업을 요청하면, Claude는 우선순위 1번으로 이 검증 작업부터 추천할 것

## 진행 중인 TODO

### README에 동작 스크린샷 추가 (보류 — 사용자가 직접 캡처 예정)

- **상태**: 사용자가 자기 환경에서 실행한 뒤 캡처해서 넣기로 함 (2026-04-08 결정)
- **목적**: README 최상단의 mermaid 아키텍처 다이어그램 옆/아래에 "정말 동작한다"는 시각적 증거를 박아 포폴 첫 카드의 설득력을 한 단계 올린다.
- **권장 캡처 후보** (최소 1장, 최대 3장):
  1. React Dashboard 실시간 화면 (`http://localhost:5173` 또는 `http://localhost:3001`) — 다중 자산 카드 + SSE 실시간 갱신이 보이는 시점
  2. Analytics 화면 — 차트/통계 컴포넌트가 실제 데이터로 그려진 상태
  3. Grafana 대시보드 (`http://localhost:3000`) — Micrometer/Prometheus 메트릭이 흐르는 모습 (운영 관점 증명)
- **저장 위치 권장**: `docs/screenshots/` 폴더 신규 생성 후 그 안에 PNG로 보관
- **README 삽입 위치 권장**: 현재 `## Architecture` 섹션 바로 아래, `## 상태 스냅샷` 위. 새 섹션 `## Screenshots` 또는 `## Live Preview`로 추가
- **사용자가 캡처 파일을 넣어두면 Claude가 할 일**:
  1. `docs/screenshots/` 안의 파일명 확인
  2. README에 적절한 alt text와 함께 ![](docs/screenshots/xxx.png) 형식으로 삽입
  3. 각 스크린샷 1줄 캡션 (어떤 기능/화면인지)
