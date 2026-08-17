# 🛰️ Gonow: Universal Journey State Machine Specification (v1.5)

본 문서는 **Gonow** 서비스의 핵심 엔진인 **여정 상태 머신 (Journey State Machine)** 의 구조와 전역 비즈니스 로직을 정의합니다. 본 체계는 `PERSONAL`, `HOME (막차)`, `GROUP` 모든 모드에 공통 적용되는 핵심 아키텍처입니다.

---

## 1. 개요 (Overview)
**Gonow** 엔진은 사용자의 실시간 위치 변화를 '이벤트'로 인지하여 최적의 알람 시각을 도출하고, 사용자의 물리적 이동을 기반으로 여정의 단계를 능동적으로 전환합니다.

GPS는 원칙적으로 지오펜싱 대신 **주기적 폴링(N초마다)** 방식을 사용한다. (Apple Developer 계정 없이 iOS 개발 시 지오펜싱 불가) 단, 안드로이드는 전 상태 지오펜싱 전환이 코드상 완료됐다 — NEARDEST(2026-08-12, 순수 지오펜싱) → DEPARTING(2026-08-17, 순수 지오펜싱) → MOVING(2026-08-17, 폴링과 병행하는 보조 지오펜싱) → READY(2026-08-17, 지오펜싱 + 서버 시간 트리거) 순서로 구현됨. NEARDEST/DEPARTING/MOVING/READY 4개 상태 전부 실기기 검증·커밋까지 완료됐다(2026-08-17). iOS는 모든 상태에서 기존 폴링 방식 그대로다. 상세 설계와 진행 이력은 `docs/history/geofencing-migration-plan.md` 참고.

---

## 2. 상태 전이 다이어그램

```
SCHEDULED ──(새벽 4시 스케줄러)──▶ READY
  ▲  반복여정: ARRIVED도 READY로        │
  │                    ┌───────────────┤
  │                    │ distToDest    │ P>=Q
  │                    │ <100m         ▼
  │                NEARDEST        DEPARTING
  │                    │               │       │
  │    P<Q+100m 벗어남 │               │       │ distFromAnchor
  └───────────────────┘│  distToDest   │       │   >= 300m
          확인버튼 또는 │  < 100m       │       ▼
          targetTime초과▼              ▼     MOVING
                    ARRIVED        ARRIVED     │
                       ▲                       │
                       └───── distToDest<100m ─┘

NEARDEST: targetTime 초과 → ArrivedTransitionScheduler → ARRIVED
NEARDEST: P < Q + 100m 벗어남 → READY 복귀
NEARDEST: P >= Q → 100m 벗어나도 NEARDEST 고정 (알람 울리는 중 방어)
반복 여정: ARRIVED → 다음 반복 요일 새벽 4시 → READY (SCHEDULED 생략)
```

**추가**: `READY`/`DEPARTING`/`MOVING` 상태가 `targetTime` + 1시간이 지나도록 `ARRIVED`에 도달하지 못하면 `ArrivedTransitionScheduler`가 강제로 `ARRIVED` 처리한다 (지각/노쇼 정리, 여정·참가자 공통).

---

## 3. 상태별 상세 정의 (State Definitions)

### ⓪ SCHEDULED (여정 예약 상태)
- **정의:** 여정이 생성되었으나 당일이 아닌 상태
- **전환 트리거:** 매일 새벽 4시 서버 스케줄러(`ReadyTransitionScheduler`) → `READY`

### ① READY (대기 및 최적화 상태)
- **정의:** 여정 당일 새벽 4시부터 출발 알람 전까지의 상태. 주기적 GPS 폴링 가동(iOS·안드로이드 미전환 시). `P >= Q` 시간 트리거는 위치와 무관해 지오펜스로 못 잡으므로 서버 `DepartingTransitionScheduler`(매분)가 대신 감시 — 아래 "GPS 감지 방식" 참고.
- **앵커 관리 (500m 단위):**
  - 최초 좌표 수신 시 → `current_lat/lng`에 앵커 저장
  - 이후 앵커로부터 500m 이탈 시에만 앵커 갱신 + 플라스크 호출 (500m 미만이면 좌표 갱신 안 함)
  - **500m 단위 앵커 보존 이유:** N초마다 좌표를 갱신하면 기준점이 계속 바뀌어 500m 이탈 감지 불가
  - **반복 여정 주의(버그44, 2026-08-17 수정):** 반복 여정이 `ARRIVED → READY`로 넘어갈 때 이 앵커와 `departureAlarmTime`을 서버가 명시적으로 null 리셋한다(`ReadyTransitionService`) — 안 그러면 어제 값이 남아있어 오늘 첫 GPS가 어제 앵커 500m 안일 때 재계산 없이 스테일 값으로 판정될 수 있었음.
- **전환 조건 (우선순위 순):**
  1. `distToDest < 100m` → `NEARDEST` (최우선, 플라스크 호출 + 앵커 저장 포함)
  2. `currentPoint == null` → 최초 앵커 저장 + 플라스크 호출
  3. `distFromAnchor >= 500m` → 앵커 갱신 + 플라스크 호출
  4. 위 조건 후 `P >= Q` → `DEPARTING`
- **GPS 감지 방식(안드로이드, 2026-08-17~)**: 지오펜싱으로 전환 완료 — 앵커 500m EXIT(재센터링 시 재등록) + 목적지 100m ENTER(NEARDEST 핸드오프) 두 지오펜스(`readyGeofenceTask.ts`). `P >= Q` 도달은 서버 `DepartingTransitionScheduler`가 매분 감시해 FCM(`sync_event: departing_transition`)으로 클라이언트에 알리고, 클라이언트는 캐시해둔 앵커로 DEPARTING 지오펜스로 핸드오프한다(좌표 재확보 불필요). iOS는 기존 폴링 유지. 실기기 검증 전(2026-08-17 기준).

### ② DEPARTING (출발 준비 상태)
- **정의:** 출발 알람(`departureAlarmTime`)이 도달하여 사용자 외출을 유도하는 단계
- **앵커:** `DEPARTING` 진입 시점에 별도로 갱신되지 않음 — READY 때 마지막으로 저장된 앵커(최대 500m 전 좌표일 수 있음)를 그대로 이어받아 300m 이탈 판정 기준점으로 사용(`JourneyService.updateLocation()`의 READY→DEPARTING 분기는 `updateCurrentPoint()`를 호출하지 않음)
- **알람:** 앱이 `P >= Q AND status == DEPARTING` 조건으로 단계별 알람 처리 (1→4단계)
- **GPS 감지 방식(안드로이드, 2026-08-17~)**: 폴링 대신 순수 지오펜싱 — 앵커 기준 300m EXIT + 목적지 기준 100m ENTER 두 지오펜스를 동시 등록(`departingGeofenceTask.ts`). READY도 지오펜싱으로 전환된 뒤(Phase 3)로는 앵커 좌표를 클라이언트가 READY 진입 시점에 로컬 캐시해둔 정확한 값을 그대로 재사용한다(`readyGeofenceTask.ts`의 `getReadyAnchor()`, `sync_event: departing_transition` 핸드오프 경로 포함) — DEPARTING 진입을 감지한 시점의 좌표를 근사치로 쓰던 것(Phase 1 도입 당시, READY가 아직 폴링 기반이던 시절 임시 방편)은 대체됐다. iOS는 기존 폴링 유지.
- **전환 조건:**
  - `distToDest < 100m` → `ARRIVED` (이동 중 도착, 확인 불필요)
  - `distFromAnchor >= 300m` → `MOVING`
  - 300m 미만이면 `DEPARTING` 유지, 좌표 갱신 안 함 (앵커 보존)

### ③ MOVING (실시간 이동 상태)
- **정의:** 출발지 앵커로부터 300m 이탈이 확인된 이동 중 상태
- **알람:** 단계별 알람 중단
- **그룹 특화:** 대시보드에 ETA(`estimatedArrival`) 표시, 매 GPS 수신마다 플라스크 호출 → ETA 갱신
- **GPS 감지 방식(안드로이드, 2026-08-17~)**: 실시간 속도/방향/ETA 계산이 필요해 폴링을 지오펜싱으로 완전히 대체할 수 없음 — 폴링은 그대로 유지하고, 목적지 100m ENTER 지오펜스를 **보조**로 병행 등록(`movingGeofenceTask.ts`). 둘 중 먼저 도착을 확정하는 쪽이 이기며, 반대쪽은 idempotent하게 스킵/정리되도록 방어돼 있음(폴링·지오펜스 어느 쪽이 먼저 `ARRIVED`를 확정해도 안전).
- **전환 조건:** `distToDest < 100m` → `ARRIVED`

### ④ NEARDEST (목적지 근처 도착 확인 대기 상태)
- **정의:** `READY` 상태에서 목적지 100m 이내 도달 시 진입. 도착 확인 대기 중.
- **진입 경로:** `READY → NEARDEST` (일찍 출발해서 목적지 근처 도달)
- **좌표 수신 시 처리:**
  - `distToDest < 100m` → `NEARDEST` 유지
  - `distToDest >= 100m + P < Q` → `READY` 복귀 + Q 재계산 (스쳐지나간 케이스)
  - `distToDest >= 100m + P >= Q` → `NEARDEST` 고정 (알람 울리는 중, 방향 감각 상실 방어)
- **전환 조건:**
  - 사용자 확인(`/arrive` API 호출) → `ARRIVED`
  - `targetTime` 초과 → `ArrivedTransitionScheduler` 자동 `ARRIVED`
- **알람:** 앱이 `P >= Q AND (status == DEPARTING OR status == NEARDEST)` 조건으로 단계별 알람 처리

### ⑤ ARRIVED (여정 완료 상태)
- **정의:** 최종 목적지에 도착하여 모든 트래킹 종료
- **그룹 특화:** 모든 참가자 `ARRIVED` → `Appointment` 상태 `FINISHED`
- **반복 여정:** 다음 반복 요일 새벽 4시 스케줄러가 `ARRIVED` → `READY` 직접 전환

---

## 4. DEPARTING 케이스별 처리

| 상황 | 조건 | 처리 |
|---|---|---|
| 정상 흐름 | DEPARTING 진입 시 dest ≥ 400m | 300m 이탈 → MOVING → 100m → ARRIVED |
| 반경 겹침 | DEPARTING 진입 시 100m ≤ dest < 400m | 이동하다 100m 진입 → ARRIVED |
| 일찍 도착 | READY 상태에서 dest < 100m | READY → NEARDEST → 확인 → ARRIVED |

---

## 5. Appointment 상태 전이 (그룹 전용)

| 전이 | 조건 |
|---|---|
| `WAITING → ACTIVE` | 참가자 중 누군가 `MOVING` 또는 `ARRIVED` 진입 시 |
| `ACTIVE → FINISHED` | 모든 참가자 `ARRIVED` 시 (`countNotArrived == 0`) |

---

## 6. 기술적 보조 사양 (Technical Specs)

### GPS 폴링 방식
- 지오펜싱 대신 N초마다 좌표를 서버에 전송 (`PATCH /location`)
- 폴링 주기(`interval`)는 서버가 계산해서 `/location` 응답으로 지시 (목적지에 가까울수록/시간이 급할수록 짧게) — 앱은 이 값을 받아 다음 호출 주기를 조정
- 서버는 좌표를 받아 Haversine 공식으로 거리 계산 후 상태 전이 판단
- **예외(안드로이드)**: NEARDEST(2026-08-13~)·DEPARTING(2026-08-17~)·READY(2026-08-17~)는 폴링 대신 순수 지오펜싱으로 대체됐고, MOVING(2026-08-17~)은 폴링에 보조 지오펜싱을 병행한다. READY→DEPARTING 전이는 위치와 무관한 순수 시간 조건(`P >= Q`)이라 지오펜스로 못 잡으므로, 서버 `DepartingTransitionScheduler`(매분)가 대신 감시해 FCM(`sync_event: departing_transition`)으로 알린다. 지오펜스 콜백이 오면 그 좌표로 `/location`을 1회 호출해 서버 판정을 받는 방식은 동일 — 즉 좌표가 폴링으로 왔는지 지오펜스 콜백으로 왔는지 서버는 구분하지 않는다. iOS는 지오펜싱을 못 쓰므로 모든 상태에서 기존 폴링을 유지한다 — `alarmService.ts`/`backgroundLocationTask.ts`의 READY/DEPARTING/NEARDEST/MOVING 진입점 전부에 `Platform.OS === 'android'` 인라인 체크가 있어 iOS는 자동으로 폴링 분기로 빠진다(2026-08-17 전체 적용 완료). FGS(포그라운드 서비스)는 알람 존재 여부와만 연동되고 GPS 폴링과는 독립적으로 켜짐/꺼짐이 결정된다(`modules/foreground-service`, GoNow_Fronted). 지오펜스 콜백이 짧은 시간에 중복 전달되거나(같은 지오펜스가 2번), 서로 다른 지오펜스가 거의 동시에 발화하는 경우가 실기기로 확인돼(2026-08-16), key 단위 처리 락(`withKeyLock`)으로 방어한다 — 4개 지오펜스 태스크(NEARDEST/DEPARTING/MOVING/READY) 전부에 적용돼 있다(2026-08-17 NEARDEST 소급 적용 완료).

### 앵커 (Anchor) 개념
- **READY 앵커:** 최초 좌표 또는 500m 이탈 시 갱신. 플라스크 호출 기준점.
- **DEPARTING 앵커:** 진입 시점에 새로 확정되지 않음 — READY 앵커를 그대로 재사용해 300m 이탈 감지 기준점으로 씀(즉 DEPARTING 진입 직후 이미 앵커로부터 최대 500m 가까이 떨어져 있을 수 있어, 300m 이탈 판정이 예상보다 빨리 걸릴 수 있음)
- 두 상태 모두 앵커 보존을 위해 조건 미충족 시 `updateCurrentPoint()` 호출 안 함

### 거리 기준 상수 (`GeoConstants`)
- `ARRIVAL_THRESHOLD_METERS = 100` — 목적지 도착 판정 반경
- `DEPARTURE_THRESHOLD_METERS = 300` — 출발 감지 반경
- `RECOMPUTE_THRESHOLD_METERS = 500` — 출발 알람 시각 재계산 반경 (READY 상태)

### 스케줄러
| 스케줄러 | 주기 | 역할 |
|---|---|---|
| `ReadyTransitionScheduler` | 매일 새벽 4시 | 당일 `SCHEDULED` → `READY` 벌크 전환 + 반복 여정 `ARRIVED` → `READY` 직접 전환 |
| `ArrivedTransitionScheduler` | 매 1분 | ① `NEARDEST` + 오늘 날짜 + `targetTime` 초과 → 자동 `ARRIVED`  ② `READY`/`DEPARTING`/`MOVING` + `targetTime` + 1시간 초과 → 자동 `ARRIVED` (지각 정리, 여정·참가자 공통) |
| `DepartingTransitionScheduler` | 매 1분 | `READY` + `departureAlarmTime` 도달 → `DEPARTING` (좌표 없이 상태만 벌크 전환 — DEPARTING은 READY 앵커 재사용). 안드로이드 READY 지오펜싱의 시간 트리거 대체(2026-08-17). Participant는 `departureAlarmTime`이 참가자별 독립값이라 약속 단위가 아닌 참가자 ID 단위로 개별 판정 |

①(NEARDEST 초과 자동 전환)만 FCM Data(`sync_event: auto_arrived`, `journey_ids`/`appointment_ids`)를 토큰별로 개별 발송한다(`ArrivedTransitionService.transitionNeardestToArrived()`) — NEARDEST는 지오펜싱 기반이라 클라이언트가 폴링 중이 아니면 서버의 강제 전환을 스스로 알 방법이 없기 때문. 개인/귀가/그룹 여정 공통으로 발송되며, ②(지각 정리, `transitionActiveToArrived()`)는 FCM 발송이 없다. 상세는 `CLAUDE.md`의 "FCM Data 자동 ARRIVED 알림" 절 참고.

`DepartingTransitionScheduler`도 같은 이유(READY가 지오펜싱 기반이라 안드로이드 클라이언트가 서버의 시간 기반 전환을 스스로 알 방법이 없음)로 FCM Data(`sync_event: departing_transition`, `journey_ids`/`appointment_ids`)를 토큰별로 발송한다(`DepartingTransitionService`). 클라이언트는 이 신호를 받으면 READY 지오펜스를 내리고, 로컬에 캐시해둔 READY 진입 시점 앵커 좌표로 DEPARTING 지오펜스를 등록한다(서버가 좌표를 새로 안 보내도 됨).

### 반복 여정 사이클
```
최초 생성 → SCHEDULED
새벽 4시 → READY (plan_date <= 오늘 + 오늘 요일이 repeatDays에 포함)
당일 완료 → ARRIVED
다음 반복 요일 새벽 4시 → ARRIVED → READY (SCHEDULED 단계 생략)
```

### 상태 전이 트리거 요약
| 전이 | 트리거 | 주체 |
|---|---|---|
| `SCHEDULED → READY` | 당일 새벽 4시 | 서버 스케줄러 |
| `ARRIVED → READY` | 다음 반복 요일 새벽 4시 (반복 여정) | 서버 스케줄러 |
| `READY → NEARDEST` | `distToDest < 100m` | 서버 (좌표 수신 시) |
| `READY → DEPARTING` | `P >= Q` | 서버 (좌표 수신 시) |
| `NEARDEST → READY` | `distToDest >= 100m + P < Q` | 서버 (좌표 수신 시) |
| `NEARDEST → NEARDEST 고정` | `P >= Q` (100m 벗어나도 유지) | 서버 (좌표 수신 시) |
| `NEARDEST → ARRIVED` | 사용자 확인 | 앱 → `/arrive` API |
| `NEARDEST → ARRIVED` | `targetTime` 초과 | 서버 스케줄러 |
| `DEPARTING → ARRIVED` | `distToDest < 100m` (이동 중) | 서버 (좌표 수신 시) |
| `DEPARTING → MOVING` | `distFromAnchor >= 300m` | 서버 (좌표 수신 시) |
| `MOVING → ARRIVED` | `distToDest < 100m` | 서버 (좌표 수신 시) |
| `READY`/`DEPARTING`/`MOVING` → `ARRIVED` | `targetTime` + 1시간 초과 (지각 정리) | 서버 스케줄러 |
