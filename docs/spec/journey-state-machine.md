# 🛰️ Gonow: Universal Journey State Machine Specification (v1.5)

본 문서는 **Gonow** 서비스의 핵심 엔진인 **여정 상태 머신 (Journey State Machine)** 의 구조와 전역 비즈니스 로직을 정의합니다. 본 체계는 `PERSONAL`, `HOME (막차)`, `GROUP` 모든 모드에 공통 적용되는 핵심 아키텍처입니다.

---

## 1. 개요 (Overview)
**Gonow** 엔진은 사용자의 실시간 위치 변화를 '이벤트'로 인지하여 최적의 알람 시각을 도출하고, 사용자의 물리적 이동을 기반으로 여정의 단계를 능동적으로 전환합니다.

GPS는 지오펜싱 대신 **주기적 폴링(N초마다)** 방식을 사용한다. (Apple Developer 계정 없이 iOS 개발 시 지오펜싱 불가)

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
- **정의:** 여정 당일 새벽 4시부터 출발 알람 전까지의 상태. 주기적 GPS 폴링 가동.
- **앵커 관리 (500m 단위):**
  - 최초 좌표 수신 시 → `current_lat/lng`에 앵커 저장
  - 이후 앵커로부터 500m 이탈 시에만 앵커 갱신 + 플라스크 호출 (500m 미만이면 좌표 갱신 안 함)
  - **500m 단위 앵커 보존 이유:** N초마다 좌표를 갱신하면 기준점이 계속 바뀌어 500m 이탈 감지 불가
- **전환 조건 (우선순위 순):**
  1. `distToDest < 100m` → `NEARDEST` (최우선, 플라스크 호출 + 앵커 저장 포함)
  2. `currentPoint == null` → 최초 앵커 저장 + 플라스크 호출
  3. `distFromAnchor >= 500m` → 앵커 갱신 + 플라스크 호출
  4. 위 조건 후 `P >= Q` → `DEPARTING`

### ② DEPARTING (출발 준비 상태)
- **정의:** 출발 알람(`departureAlarmTime`)이 도달하여 사용자 외출을 유도하는 단계
- **앵커 확정:** `DEPARTING` 진입 시점의 `current_lat/lng`가 출발지 앵커로 확정됨
- **알람:** 앱이 `P >= Q AND status == DEPARTING` 조건으로 단계별 알람 처리 (1→4단계)
- **전환 조건:**
  - `distToDest < 100m` → `ARRIVED` (이동 중 도착, 확인 불필요)
  - `distFromAnchor >= 300m` → `MOVING`
  - 300m 미만이면 `DEPARTING` 유지, 좌표 갱신 안 함 (앵커 보존)

### ③ MOVING (실시간 이동 상태)
- **정의:** 출발지 앵커로부터 300m 이탈이 확인된 이동 중 상태
- **알람:** 단계별 알람 중단
- **그룹 특화:** 대시보드에 ETA(`estimatedArrival`) 표시, 매 GPS 수신마다 플라스크 호출 → ETA 갱신
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

### 앵커 (Anchor) 개념
- **READY 앵커:** 최초 좌표 또는 500m 이탈 시 갱신. 플라스크 호출 기준점.
- **DEPARTING 앵커:** `DEPARTING` 진입 시점의 좌표로 확정. 300m 이탈 감지 기준점.
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
