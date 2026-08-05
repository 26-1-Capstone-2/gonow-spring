# GoNow 문서 색인

루트 `CLAUDE.md`는 항상 자동으로 로드되지만, 아래 문서들은 필요할 때만 직접 읽는 참고 자료입니다 (자동 로드 안 됨).

## GoNow 시스템 문서 (`/sync-docs` 점검 대상)

실제 자바(스프링) 소스코드와 맞춰서 관리하는 문서들 — `CLAUDE.md`와 함께 `/sync-docs`가 정합성을 점검하는 범위(`spec/`, `status/`, `testing/`, `history/`).

| 문서 | 경로 | 언제 참고할지 |
|---|---|---|
| DB 스키마 | [spec/db-schema.md](spec/db-schema.md) | 엔티티/테이블 관련 작업 시 |
| 여정 상태머신 스펙 | [spec/journey-state-machine.md](spec/journey-state-machine.md) | 상태 전이/알람 로직 작업 시 |
| 프론트엔드 구현 현황 | [status/frontend-impl-status.md](status/frontend-impl-status.md) | 프론트-백엔드 연동 작업 시 |
| 백그라운드/FCM 테스트 가이드 | [testing/TEST_GUIDE.md](testing/TEST_GUIDE.md) | GPS 폴링/FCM 테스트 시 |
| 해결된 버그 아카이브 | [history/resolved-bugs.md](history/resolved-bugs.md) | 과거 수정 이력 확인 시 |
| GPS 폴링 동기화 변경 이력 | [history/gps-polling-sync-changes.md](history/gps-polling-sync-changes.md) | API 응답 필드 변경 이력 확인 시 |
| 좀비 Runner 버그 수정 상세 | [history/zombie-runner-fix.md](history/zombie-runner-fix.md) | 폴링 중복 버그 원인/구조 상세 확인 시 |
| FCM/스케줄러 설계안 v1 (구버전, 대체됨) | [history/fcm-scheduler-design-v1.md](history/fcm-scheduler-design-v1.md) | 초기 설계 배경 파악 시 (실제 구현은 `CLAUDE.md`/`spec/journey-state-machine.md` 우선) |

## 그 외 참고 자료 (`/sync-docs` 비대상)

대회 출품 준비, 아이디어, 외부 API 스펙, 개발 도구 가이드 등 — 자바 코드 정합성 점검 대상이 아닌 문서들(`planning/`, `reference/`). `planning/`은 "무엇을 할지"를 정하는 유동적인 계획/아이디어 자료고, `reference/`는 구현 여부와 무관하게 반복 참조하는 안정적인 외부 스펙/도구 가이드라는 점에서 나눔.

| 문서 | 경로 | 언제 참고할지 |
|---|---|---|
| 2026 오픈소스 개발자대회 참가 규정 요약 | [planning/oss-contest-2026.md](planning/oss-contest-2026.md) | 대회 출품 준비, 라이선스/AI 모델 규정 확인, 기능 확장 아이디어 검토 시 |
| GoNow 기능 확장 아이디어 (지도/캘린더/지오펜싱/UX) | [planning/feature-ideas.md](planning/feature-ideas.md) | 대회 출품용 신규 기능 검토, 서비스 확장 방향 논의 시 |
| 카카오맵 딥링크 공식 스펙 + GoNow 구현 가이드 | [reference/kakao-map-deeplink-spec.md](reference/kakao-map-deeplink-spec.md) | 지도 딥링크(아이디어 A) 구현/디버깅 시 |
| ODsay MCP 서버 활용 가이드 | [reference/odsay-mcp-guide.md](reference/odsay-mcp-guide.md) | 개발 중 Claude로 ODsay 경로/역 정보를 직접 조회하고 싶을 때 |

미해결 버그 목록은 루트 `BUGS.md` 참고.
