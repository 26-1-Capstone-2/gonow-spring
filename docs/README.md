# GoNow 문서 색인

루트 `CLAUDE.md`는 항상 자동으로 로드되지만, 아래 문서들은 필요할 때만 직접 읽는 참고 자료입니다 (자동 로드 안 됨).

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

미해결 버그 목록은 루트 `BUGS.md` 참고.
