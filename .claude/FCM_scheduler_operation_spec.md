# 🔔 Gonow: FCM & Scheduler Operation Specification (v1.0)

본 문서는 **Gonow** 서비스의 안정적인 알람 발송과 앱 소생을 위한 **서버 스케줄러** 및 **FCM(Firebase Cloud Messaging)** 운용 방안을 정의합니다.

---

## 1. 스케줄러 전략 (Scheduler Strategy)

### ① 데일리 소생 스케줄러 (Daily Wake-up)
* **실행 시각:** 매일 새벽 04:00 (KST)
* **대상:** 여정 상태가 `SCHEDULED` 이며, 수행 날짜가 '오늘'인 모든 여정.
* **로직:**
    1. 해당 여정의 유저 토큰을 조회하여 **사일런트 FCM (Data Message)** 발송.
    2. 여정 상태를 `SCHEDULED` -> `READY` 로 일괄 변경.
* **목적:** 유저가 앱을 강제 종료했더라도 OS 차원에서 앱을 깨워 위치 추적(지오펜싱) 및 연쇄 보정 로직을 재개함.

### ② 실시간 알람 감시 스케줄러 (Alarm Monitor)
* **주기:** 매 1분 (Fixed Rate)
* **대상:** 상태가 `READY` 이며, `departure_alarm_time` 이 현재 시각 $\pm$ 1분 이내인 여정.
* **로직:**
    1. 해당 시각에 도달한 유저에게 **가시적 FCM (Notification Message)** 발송.
    2. 여정 상태를 `READY` -> `DEPARTING` 으로 변경.
* **목적:** 앱 내부 타이머가 실패할 경우를 대비한 **최후의 보루(Redundancy)** 역할.

---

## 2. FCM 메시지 유형 (Message Types)

| 유형 | 전송 시점 | 메시지 구조 (Payload) | 유저 인지 여부 |
| :--- | :--- | :--- | :--- |
| **Silent Push (Data)** | 새벽 4시 / 장기 침묵 시 | `{"action": "WAKE_UP", "journeyId": 123}` | **비가시적** (은밀한 깨움) |
| **Notification (Alert)** | 출발 알람 시각 도달 시 | `{"title": "지금 출발하세요!", "body": "지각 위기!"}` | **가시적** (소리/진동) |
| **Emergency (Alert)** | `MOVING` 중 지각 위험 시 | `{"title": "서두르세요!", "body": "현재 속도로는 지각입니다."}` | **가시적** (긴급 알람) |

---

## 3. 예외 상황 및 안정성 확보 (Reliability)

### ① 장기 침묵 감지 (Heartbeat Check)
* **상태:** `READY` 또는 `MOVING` 인데 위치 보고가 20분 이상 없을 경우.
* **대응:** **사일런트 FCM** 을 재발송하여 앱의 포그라운드 서비스 재시작 및 소생 유도.

### ② 알람 중복 방지 로직
* **서버-앱 동기화:** 서버 푸시보다 앱의 로컬 알람이 먼저 울렸을 경우, 앱이 즉시 `DEPARTING` 상태 변경 API를 호출하여 서버의 중복 푸시를 무시하도록 처리함.

---

## 4. 기술적 보조 사양 (Technical Specs)
* **인덱스 전략:** `departure_alarm_time` 컬럼에 인덱스를 생성하여 매분 반복되는 스케줄러 조회 성능 최적화.
* **배터리 최적화:** `SCHEDULED` 상태에서는 정기적인 통신을 차단하고, 오직 새벽 4시 신호에만 반응하도록 설계.
* **UTC/KST 관리:** 서버와 DB의 시간대를 **Asia/Seoul** 로 통일하여 시간차 오류 방지.
