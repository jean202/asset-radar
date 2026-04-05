# asset-radar frontend

`frontend/`는 `asset-radar`의 React + Vite 기반 UI다. 현재 실시간 Dashboard와 Analytics 화면을 제공한다.

## 화면 구성

- `/`: 실시간 대시보드, 소스별 자산 그룹, 최근 알림, SSE 연결 상태
- `/analytics`: 이동평균, 변동성, 상관관계, 요약 통계 차트

## 사용 기술

- React 19
- Vite 8
- React Router
- Recharts
- ESLint

## 실행

```bash
npm install
npm run dev
```

기본 개발 서버는 `http://localhost:5173`이며 `/api` 요청은 `http://localhost:8080`으로 프록시된다.

## 스크립트

- `npm run dev`: 개발 서버 실행
- `npm run build`: 프로덕션 빌드
- `npm run preview`: 빌드 결과 미리보기
- `npm run lint`: ESLint 실행

## 현재 상태

- [x] Dashboard 화면 구현
- [x] Analytics 화면 구현
- [x] Recharts 기반 시각화 컴포넌트 추가
- [x] 페이지/라이브러리 코드 스플리팅
- [x] Docker/Nginx 배포 이미지 구성
- [x] ESLint 오류 정리
- [x] Vitest + Testing Library 테스트 추가

## 메모

- 현재 `npm run lint`, `npm run test`, `npm run build`는 통과한다.
