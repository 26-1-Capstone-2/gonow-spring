# GPS 폴링 동기화 관련 변경사항

## 개요
프론트가 GPS 폴링 시작/중단 시점을 정확히 판단할 수 있도록 API 응답에 상태값 추가 및 관련 로직 보완

---

## 변경된 파일 목록

| 파일 | 변경 유형 | 내용 |
|------|---------|------|
| `AlarmResponse.java` | 수정 | `myStatus` 필드 추가 |
| `AlarmService.java` | 수정 | `myStatus` 값 세팅 |
| `AppointmentCreateResponse.java` | 수정 | `participantStatus` 필드 추가 |
| `AppointmentJoinResponse.java` | 수정 | `participantStatus` 필드 추가 |
| `AppointmentUpdateResponse.java` | **신규 생성** | 약속 수정 응답 DTO |
| `AppointmentController.java` | 수정 | 수정 API 응답 타입 변경 (`Void` → `AppointmentUpdateResponse`) |
| `AppointmentService.java` | 수정 | 수정 차단 로직, 참가자 상태 재조정, FCM Data 발송, `FcmSender` 주입 |
| `ParticipantRepository.java` | 수정 | `bulkResetStatusByAppointmentId` 쿼리 추가 |
| `JourneyService.java` | 수정 | 수정 차단 로직 추가 (`UNMODIFIABLE_STATUSES`) |
| `FcmSender.java` | 수정 | `sendAllData()` 메서드 추가 |

---

## 상세 변경 내역

### 1. `GET /api/alarms?date=오늘` 응답에 `my_status` 추가

**목적:** 앱 실행 시 READY 상태 알람 감지 → GPS 폴링 복구

```json
{
  "alarm_type": "PERSONAL",
  "journey_id": 1,
  "appointment_id": null,
  "my_status": "READY",
  ...
}
```

---

### 2. `POST /api/appointments` 응답에 `participant_status` 추가

**목적:** 당일 약속 생성 시 즉시 GPS 폴링 시작 여부 판단

```json
{
  "appointment_id": 1,
  "invite_code": "AB3C4DEF",
  "participant_status": "READY"
}
```

---

### 3. `POST /api/appointments/join` 응답에 `participant_status` 추가

**목적:** 당일 약속 참여 시 즉시 GPS 폴링 시작 여부 판단

```json
{
  "appointment_id": 1,
  "participant_status": "SCHEDULED"
}
```

---

### 4. `PATCH /api/appointments/{appointmentId}` 응답 변경

**기존:** `data: null`
**변경:** `participant_status` 반환

**목적:** 방장이 날짜 수정 시 본인 폴링 시작/중단 즉시 판단

```json
{
  "participant_status": "READY"
}
```

**추가 동작:**
- `ACTIVE` 상태 약속 수정 시 `400` 에러 반환 (이미 이동 중인 참가자 있음)
- 날짜 변경 시 모든 참가자 상태 일괄 재조정 (SCHEDULED/READY/DEPARTING 대상)
- 방장 제외 나머지 참가자에게 FCM Data 발송

**FCM Data 페이로드:**
```json
{
  "appointment_id": "123",
  "participant_status": "READY"
}
```

---

### 5. 여정 수정 차단 로직 추가

`PUT /api/journeys/personal/{journeyId}`, `PUT /api/journeys/home/{journeyId}`

- `journey_status` 가 `MOVING`, `NEARDEST`, `ARRIVED` 이면 `400` 에러 반환

---

### 6. `FcmSender.sendAllData()` 추가

동일 페이로드를 여러 기기에 FCM Data 발송하는 메서드 추가
(기존 `sendData()`는 단건, 신규 `sendAllData()`는 다중)

---

## 롤백 방법

아래 커밋으로 `git revert` 또는 `git reset` 사용:

```bash
# 이 커밋 이전으로 되돌리기
git reset --hard <이전 커밋 해시>
```

롤백 시 영향받는 API:
- `GET /api/alarms?date=` — `my_status` 필드 제거됨
- `POST /api/appointments` — `participant_status` 필드 제거됨
- `POST /api/appointments/join` — `participant_status` 필드 제거됨
- `PATCH /api/appointments/{id}` — `data: null` 로 복귀
- 여정/약속 수정 차단 로직 제거됨
- 약속 수정 시 FCM Data 발송 제거됨
