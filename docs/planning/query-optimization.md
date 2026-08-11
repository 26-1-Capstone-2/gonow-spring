# 쿼리 최적화 백로그

지금 당장 고치지 않지만, 시간이 남을 때 진행할 만한 쿼리/리포지토리 레벨 개선 아이디어를 모아두는 곳. 기능 동작에는 문제없는 코드 품질/이식성 개선 항목이라 `BUGS.md`(활성 버그 트래커)가 아니라 여기에 기록한다.

---

## 1. 비트마스크·GROUP_CONCAT 네이티브 쿼리를 Hibernate 7의 포터블 HQL 함수로 전환 가능

**발견 경위**: 2026-08-11, NEARDEST 자동 ARRIVED FCM 발송 기능 추가 중 기존 리포지토리 메서드의 `// 비트마스크 연산으로 네이티브 쿼리 필수` 주석이 실제로 정확한지 질문받아 확인.

**결론**: 이 프로젝트가 쓰는 `hibernate-core:7.2.12.Final`(`./gradlew dependencies --configuration compileClasspath`로 확인)은 HQL에 `bitand(x, y)` 이식 가능 비트 연산 함수를 지원한다(Hibernate 6.2부터 추가됨). `FUNCTION('BITAND', ...)`(JPA 표준 native-function 이스케이프)와는 다른 것 — 이건 Hibernate 자체 HQL 함수라 DB별로 알아서 올바른 문법(MySQL은 `&` 연산자, Oracle은 `BITAND()` 함수)으로 번역해준다. 즉 `nativeQuery = true` 없이도 순수 JPQL로 반복 여정(`repeat_days & bit`) 조건을 표현할 수 있었다.

**주의 — 흔히 헷갈리는 것**: MySQL/MariaDB에는 `BITAND()`라는 함수가 없다(비트 AND는 `&` 연산자로만 제공됨). 이름이 비슷한 `BIT_AND()`는 완전히 다른 용도의 **집계 함수**(GROUP BY로 묶인 여러 행을 AND, `SUM()`과 비슷한 성격)라 여기 쓸 수 없다. `BITAND(x, y)`는 사실 Oracle의 스칼라 함수 이름이다. `FUNCTION('BITAND', ...)`을 MySQL에 그대로 쓰면 "그런 함수 없음" 런타임 에러가 난다 — 반드시 Hibernate의 `bitand()` HQL 함수를 써야 한다.

**GROUP_CONCAT도 잠재적으로 포터블화 가능(미검증)**: 표준 SQL `LISTAGG`를 Hibernate가 `listagg()` HQL 함수로 지원한다. 토큰별 ID 그룹화 쿼리들이 `GROUP_CONCAT` 때문에도 네이티브 쿼리를 쓰고 있어서, 이것까지 바꾸면 완전히 순수 JPQL로 전환 가능할 수 있다(단, 문법 세부사항 미검증 — 착수 시 직접 확인 필요).

**왜 지금 안 고치는지**: 이번에 새로 추가한 메서드 두 개(`findTokenJourneyIdPairsForNeardestOverdueInternal` 등)만의 문제가 아니라, 기존에 이미 있던(그리고 잘 동작 중인) 여러 메서드에 걸친 패턴이라 손대는 범위가 넓다. 지금 당장 우선순위가 높지 않아 보류.

**영향받는 메서드 (전환 시 참고)**

`JourneyRepository.java` — 비트마스크(`repeat_days & bit`) 관련:
- `findAllByPlanDate`
- `bulkUpdateToReadyInternal` / `bulkUpdateToReady`
- `findIdsNeardestOverdueInternal` / `findIdsNeardestOverdue`
- `findIdsActiveOverdueInternal` / `findIdsActiveOverdue`
- `findTokenJourneyIdPairsForReadyTransitionInternal` / `findTokenToJourneyIdsForReadyTransition` (비트마스크 + GROUP_CONCAT 둘 다)
- `findTokenJourneyIdPairsForNeardestOverdueInternal` / `findTokenToJourneyIdsForNeardestOverdue` (비트마스크 + GROUP_CONCAT 둘 다, 신규)

`ParticipantRepository.java` — Appointment는 반복 여정 개념이 없어 비트마스크는 해당 없음, GROUP_CONCAT만 해당:
- `findTokenAppointmentIdPairsForReadyTransitionInternal` / `findTokenToAppointmentIdsForReadyTransition`
- `findTokenAppointmentIdPairsForNeardestOverdueInternal` / `findTokenToAppointmentIdsForNeardestOverdue` (신규)

**전환 시 얻는 이점**: `nativeQuery = true` 제거 → Enum을 `.name()`으로 조립하는 `~Internal` + 기본 래퍼 이중 구조(가독성 저하 원인)를 없애고 엔티티 필드/Enum을 타입 안전하게 직접 참조하는 순수 JPQL로 단순화 가능.

**전환 시 주의**: 기존에 검증된 코드를 다수 건드리는 리팩터링이라, 착수 시 전환 전후 쿼리 결과가 동일한지(특히 반복 여정 비트마스크 조건, 널 토큰 제외 조건) 회귀 테스트 필수.
