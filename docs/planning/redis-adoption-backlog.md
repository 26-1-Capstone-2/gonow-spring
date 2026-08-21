# Redis 도입 백로그

지금 당장 고치지 않지만, 시간이 남을 때 진행할 만한 Redis 도입 후보를 정리해두는 곳.

**2026-08-21 갱신**: 애초엔 "회원가입/비번찾기는 MySQL로, Redis는 Refresh Token 단계에서 최초 도입"으로 정리해뒀으나, 회원가입/비밀번호 찾기/Refresh Token을 같은 날 한 번에 진행하기로 하면서 이 순서 제약이 무의미해져 **이메일 인증코드부터 Redis 호환 저장소로 바로 도입**했다(`compose.yml`의 `my-valkey` 서비스, `application.yml`의 `spring.data.redis.*`, `EmailVerificationService`의 `StringRedisTemplate` 사용 — 상세는 `CLAUDE.md`의 "이메일 인증 규칙" 참고). 즉 아래 "1. Refresh Token 저장" 후보는 이제 인프라(compose/설정)가 이미 갖춰진 상태에서 순수 기능 구현만 남은 상태다.

**같은 날 추가 갱신**: 실제 배포 이미지는 Redis가 아니라 **Valkey**(`valkey/valkey:9.1-alpine`)로 채택했다 — Redis는 2024년 라이선스 변경으로 OSI 인증 오픈소스가 아니게 됐고(상업적 재판매 제약이 있는 소스공개 라이선스), Valkey는 그 이전 버전(7.2)을 포크해 BSD-3를 유지하는 커뮤니티 포크(Linux Foundation·AWS·Google 등이 후원)다. 이 문서의 "Redis"라는 표현은 전부 이 계열 기술(Redis-compatible in-memory store) 전체를 가리키는 일반적 지칭으로 이해하면 되고, 실제로 뭘 쓸지 결정할 땐 Valkey를 기본값으로 삼는다. 코드/설정에서 쓰는 `RedisTemplate`류 이름은 Valkey를 써도 그대로 유지된다(프로토콜 호환).

---

## 도입 후보

### 1. Refresh Token 저장

지금 JWT가 access token 하나뿐(50시간 만료)이고 refresh 메커니즘 자체가 없음(`CLAUDE.md` "미구현" 목록에 이미 있는 항목). Redis는 TTL 기반 토큰 저장에 정확히 맞는 용도라, 완성도 있는 인증 구조를 위해 도입 시 가장 먼저 고려할 후보. 실제로 빠진 기능을 채우는 것이라 세 후보 중 가장 명확한 가치가 있음.

### 2. 현재 위도/경도(`current_lat`/`current_lng`, `Journey`/`Participant`의 `currentPoint`)

- **성격**: 매 GPS 갱신마다 덮어써지고, 유실돼도 다음 폴링에서 `isFirstReceive`(앵커 없음)로 자연 처리되어 자동 복구됨. FK로 다른 테이블과 엮이지 않은 독립 값이라 이관 시 스키마 문제 없음. Redis의 "휘발성 고속 저장소" 용도에 정확히 맞는 데이터.
- **DB 컬럼을 완전히 빼는 것도 기술적으로 가능** — 다른 곳에서 조인/참조하지 않음.
- **우선순위가 예전보다 낮아진 이유**: 지오펜싱 마이그레이션(READY/DEPARTING/MOVING/NEARDEST, 2026-08-17 완료) 이후 대부분 상태가 이벤트 기반으로 바뀌면서, 이 값이 갱신되는 빈도 자체가 마이그레이션 이전보다 크게 줄었음. 지금 당장 옮겨서 얻는 체감 이득이 작음 — 유저 수가 실제로 많아져서 MySQL 쓰기 부하가 체감될 때 재검토.

### 3. MOVING 상태 — 플라스크(외부 API) 무조건 호출 문제 (Redis와 독립적으로 먼저 할 수 있는 개선)

**발견 경위**: 2026-08-21, MOVING 트래픽 확장성 논의 중 `JourneyService.updateLocation()`의 MOVING 분기(`case MOVING -> {...}`)를 확인.

**문제**: READY는 `isNearDest || isFirstReceive || isOutOfAnchor`(500m 이탈) 조건이 있어야만 플라스크를 부르는데, MOVING은 이 가드가 없어서 **폴링마다 무조건** `callFlaskAndUpdate()`를 호출함(실시간 ETA 갱신이 목적이라 의도적으로 이렇게 되어 있음). 유저 수가 늘어나면 여기서 외부 API(ODsay/카카오맵) 쿼터·비용·레이턴시가 먼저 병목이 될 가능성이 높음 — Redis 도입으로는 해결 안 되는 문제(유저마다 좌표가 달라 캐시가 안 맞음).

**해법 아이디어**: READY와 동일한 패턴을 MOVING에도 적용 — "마지막으로 플라스크를 부른 지점"을 앵커로 삼아 거기서 일정 거리(예: 200m) 이상 이동했을 때만 재계산.

```java
case MOVING -> {
    boolean isOutOfMovingAnchor = (distFromAnchor >= GeoConstants.MOVING_RECOMPUTE_THRESHOLD_METERS); // 예: 200m
    if (isOutOfMovingAnchor) {
        journey.updateCurrentPoint(newPoint);  // 여기서만 갱신 = 새 앵커
        flaskResponse = callFlaskAndUpdate(memberId, journey, newPoint, setting);
        interval = flaskResponse.interval();
    }
    if (isNearDest) journey.updateStatus(JourneyStatus.ARRIVED);
}
```

**주의**: 지금 MOVING은 `updateCurrentPoint(newPoint)`를 폴링마다 무조건 호출해서 `currentPoint`가 "가장 최근 GPS 좌표"일 뿐 "마지막 플라스크 호출 지점"이 아님. 위처럼 갱신 시점을 플라스크 실제 호출 시점으로 옮겨야 거리 게이팅이 의미 있게 동작함(READY의 앵커와 같은 의미로 통일).

**Redis와의 관계**: 이 개선은 저장소(MySQL이냐 Redis냐)와 무관하게 지금 당장(`currentPoint`가 MySQL 컬럼인 채로도) 적용 가능함. 저장소를 Redis로 옮기는 건 별개의, 나중에 순서와 무관하게 진행할 수 있는 결정.

---

## 검토했으나 기각한 것들

- **`/location` Rate Limiting**: 처음엔 "클라이언트 버그로 폭주해도 서버가 스스로를 지킨다"는 방어 논리로 고려했으나, 실제로 2026-08-20 라이브록 버그를 추적해보니 그 경로(막차 모드 target_time 미확정)는 `walk_fallback()`이 로컬 계산만 하고 ODsay를 부르기 전에 return해서 외부 API 비용 폭주는 없었음(스프링→플라스크 호출만 반복됨). 근거가 약해져서 기각 — 버그는 Redis로 감싸기보다 근본 원인을 고치는 게 맞다는 결론(실제로 라이브록 자체는 프론트 지오펜스 재등록 로직을 고쳐서 해결함).
- **그룹 대시보드(`ArrivalDashboardSheet.tsx`) 응답 캐싱**: 폴링 방식이라고 잘못 알고 있었으나, 실제 코드 확인 결과 `setInterval` 없이 화면 진입 시 1회 조회 + 수동 새로고침 버튼뿐이라 캐싱할 만한 hot path가 아님. `CLAUDE.md`의 "대시보드: 프론트가 폴링 방식으로 구현" 서술도 실제와 어긋나 있어 별도로 정정 필요.
- **대시보드를 FCM Data 실시간 갱신으로 전환**: 참가자 상태 전이마다(그것도 MOVING 중엔 ETA가 폴링마다 갱신) 전원에게 FCM을 쏴야 해서 트래픽/코드 복잡도가 크게 늘고, 정작 화면이 상시 떠있는 게 아니라(가끔 여는 BottomSheet) 실시간성의 이득이 거의 없음. 기존에 FCM Data를 쓰는 케이스(참가자 탈퇴/방 삭제 등)는 "드물지만 놓치면 문제되는 구조적 이벤트"라 다른 성격.
- **`departure_alarm_time`/`target_time`을 Redis로 이전**: 겉보기엔 "계산 결과 캐싱"처럼 보이지만, ① 스케줄러가 전체 여정을 범위 조회(`WHERE departure_alarm_time <= now`)하는 관계형 접근이 필요하고 ② `status`와 같은 트랜잭션으로 묶여야 하고 ③ 유실 시 실제 알람 미발송 사고로 이어짐 — Redis 이관 시 이득 없이 두 저장소 간 정합성 문제만 생겨서 기각. `current_lat/lng`와 정반대 성격.
- **플라스크 쪽 Redis 도입**: 요청 하나 받아 계산만 하는 무상태 구조라 사용자마다 좌표가 달라 캐시 히트가 잘 안 남 — 의미 있는 후보를 못 찾음. 다만 조사 중 플라스크의 `recommended_buffer()`(지각 패턴 학습)가 DB가 아니라 로컬 JSON 파일에 저장되고 있어 컨테이너 재배포 시 유실 위험이 있는 걸 발견 — 이건 Redis 문제가 아니라 별도 버그로 `BUGS.md` 버그49에 기록함.
- **프론트 쪽 Redis**: Redis는 순수 서버 기술이라 모바일 앱이 직접 접근하는 경우 자체가 성립하지 않음(항상 스프링을 경유).
