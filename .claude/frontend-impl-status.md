# 프론트엔드 구현 현황

프론트 경로: `D:\gonow-app\GoNow_Fronted`
플라스크 경로: `D:\gonow-flask\CounterClockEngine`
마지막 확인일: 2026-06-03

---

## 알람 라이브러리

- **notifee** 사용 (`expo-notifications` 아님)
- `notifee.displayNotification()` 호출 시 포그라운드/백그라운드 모두 상단바 알림 표시
- `fullScreenAction`, `bypassDnd`, 단계별 진동패턴 등 고급 기능 활용
- EAS 빌드 필수 (Expo Go 미지원)

---

## GPS 폴링 구조

- **포그라운드**: `alarmService` (AlarmRunner 클래스) — `setTimeout` 기반 폴링, `expo-location` 사용
- **백그라운드**: `backgroundLocationTask.ts` — `expo-task-manager` 기반, OS가 주기적으로 깨워서 실행
- 포그라운드 전환 시 백그라운드 태스크 `AsyncStorage` 목록에서 해당 알람 제거 (이중 처리 방지)

---

## 구현 완료 항목

### GPS 폴링 시작/중단 (응답 상태 기반)

| API | 처리 파일 | 내용 |
|-----|---------|------|
| `POST /api/journeys/personal` | `PersonalAlarmSheet.tsx` | `journey_status == READY` → GPS 시작 |
| `PUT /api/journeys/personal/{id}` | `PersonalAlarmSheet.tsx` | READY → 시작, SCHEDULED → 중단 |
| `POST /api/journeys/home` | `HomeAlarmSheet.tsx` | `journey_status == READY` → GPS 시작 |
| `PUT /api/journeys/home/{id}` | `HomeAlarmSheet.tsx` | READY → 시작, SCHEDULED → 중단 |
| `POST /api/appointments` | `GroupAlarmSheet.tsx` | `participant_status == READY` → GPS 시작 |
| `POST /api/appointments/join` | `GroupAlarmSheet.tsx` | READY → GPS 시작 |
| `PATCH /api/appointments/{id}` (방장) | `GroupAlarmSheet.tsx` | READY → 시작, SCHEDULED → 중단 |

### 앱 실행 시 GPS 폴링 복구

- `_layout.tsx`에서 앱 실행 시 1회 `GET /api/alarms?date=오늘` 호출
- `my_status == READY`인 항목만 필터링 → 각각 GPS 폴링 시작
- 새벽 4시 FCM Data 못 받은 경우 복구용
- **⚠️ 버그**: 앱 완전 종료 후 재실행 시 로그인 화면으로 튕기므로 토큰 없음 → `getAlarms` 401 실패 → GPS 폴링 복구 안 됨
- **수정 필요**: `LoginScreen.tsx` 로그인 성공 직후에도 `getAlarms` 호출하여 READY 알람 GPS 폴링 시작하도록 추가 필요

### FCM Data 수신 처리 (`_layout.tsx`)

| FCM 페이로드 | 처리 |
|------------|------|
| `journey_ids`, `appointment_ids` | 새벽 4시 READY 전환 트리거 → GPS 폴링 시작 |
| `appointment_id` + `participant_status: READY` | 방장 약속 수정 동기화 → GPS 시작 |
| `appointment_id` + `participant_status: SCHEDULED` | 방장 약속 수정 동기화 → GPS 중단 |

### 알람 수정 버튼 비활성화

- **개인/귀가**: `my_status`가 `MOVING`, `NEARDEST`, `ARRIVED`이면 카드 터치 차단 + 반투명 처리
- **그룹**: `appointment_status == ACTIVE`이면 카드 터치 차단 + 반투명 처리

### 단계별 출발 알람 (DEPARTING 진입 시)

- 1단계: 즉시 `notifee.displayNotification()` 발송
- 2~4단계: `notifee.createTriggerNotification()` (OS 스케줄 등록 — 앱 꺼져도 울림)
- `which_station` 있으면 알람 텍스트에 탑승역 + 남은 분 표시
- `which_station` 없으면 기본 메시지 표시
- NEARDEST 상태 진입 시 단계별 알람 **미발송** (도착 확인 알람만 표시)

### `which_station`, `boarding_time` 응답 처리

- `/location` 응답에서 수신
- `which_station` 있을 때 알람 텍스트: `[목적지] {역명} 탑승까지 N분 남았어요`
- DRIVING(자가용)이거나 플라스크 미호출 시 null → 기본 메시지 사용

---

## FCM Notification (그룹 도착 알림) 포그라운드 처리

- 포그라운드에서도 notifee로 재발행하는 코드 **구현 완료** (`_layout.tsx`, 2026-06-02)
- `addNotificationReceivedListener`에서 `title` + `body` 있을 때 `notifee.displayNotification()` 호출
- 백그라운드/종료: OS 자동 상단바 표시 / 포그라운드: notifee 재발행으로 상단바 표시 — 모든 상태 정상 작동 확인

## FCM Notification 그룹 알람 테스트 결과 (2026-06-03)

- **테스트 환경**: prod (https://gonow-api.uk), 폰(a@gmail.com) + 태블릿(b@gmail.com)
- **도착 예정 알람** (참가자 MOVING 진입 시) → 방장 폰에 FCM Notification ✅
- **도착 완료 알람** (참가자/방장 ARRIVED 진입 시) → 상대방에게 FCM Notification ✅
- 포그라운드(폰): notifee 재발행으로 상단바 표시 ✅
- 백그라운드(태블릿): OS 자동 상단바 표시 ✅

---

## FCM Data GPS 폴링 테스트 결과 (2026-06-03 완료)

### 수정 완료 항목

1. **`backgroundAlarmTask.ts` 21번 줄**: `(data as any)` → `(data as any)?.data` 수정 ✅
   - FCM Data 구조: `data.data.journey_ids` / `data.data.appointment_ids`
2. **`LoginScreen.tsx` 40번 줄**: `getExpoPushTokenAsync` → `getDevicePushTokenAsync` 수정 ✅
   - `getExpoPushTokenAsync`는 `ExponentPushToken[...]` 형식 반환 → Firebase 직접 발송 불가
   - `getDevicePushTokenAsync`는 실제 FCM 토큰(`dTMPJ6...`) 반환
3. **`alarmService.ts` `pollPersonal()` / `pollGroup()` stop 후 재등록 버그**: `if (!this.target) return;` 추가 ✅
4. **서버 `FcmSender.java`**: `AndroidConfig.Priority.HIGH` 추가 ✅
   - NORMAL priority FCM Data는 백그라운드에서 Android OS가 차단 → HIGH 필수

### GPS 폴링 백그라운드/포그라운드 테스트 결과 (2026-06-03)

| 시나리오 | 결과 | 비고 |
|---------|------|------|
| 포그라운드 FCM 수신 → `/location` 호출 | ✅ | 즉각 호출 (8초 이내) |
| 백그라운드 FCM 수신 → `/location` 호출 | ✅ | 30초 이내 호출 |
| 포그라운드 → 백그라운드 전환 중 폴링 유지 | ✅ | 30초 간격 유지 |
| 화면 꺼진 상태 폴링 유지 | ✅ | foregroundService 활성 시 30초 보장 |
| 로그인 후 READY 알람 GPS 폴링 복구 | ✅ | LoginScreen.tsx에서 getAlarms 호출 |

### GPS 폴링 주기 특성

- **포그라운드 (`alarmService`)**: `setTimeout` 기반, `/location` 응답의 `interval` 값으로 동적 갱신
- **백그라운드 (`backgroundLocationTask`)**: `startLocationUpdatesAsync` 등록, `timeInterval: 30000` 고정
- **foregroundService**: `backgroundLocationTask.ts`의 `foregroundService` 설정으로 상단바 "GoNow 알람 실행 중" 표시 → Android Doze 모드 면제 → 화면 꺼져도 30초 보장
- **foregroundService 미활성 상태**: 화면 꺼지면 Android Doze로 수분 단위 지연 가능

### 미수정 사항 (팀원 전달 필요)

- **`_layout.tsx` 앱 시작 시 AsyncStorage 초기화**: 앱 종료 후 재실행 시 이전 세션의 `ACTIVE_JOURNEYS_KEY`, `ACTIVE_APPOINTMENTS_KEY`가 남아 불필요한 `/location` 호출 지속
  ```ts
  await stopBackgroundLocationUpdates().catch(() => {});
  await AsyncStorage.setItem(ACTIVE_JOURNEYS_KEY, JSON.stringify([]));
  await AsyncStorage.setItem(ACTIVE_APPOINTMENTS_KEY, JSON.stringify([]));
  ```

---

## 플라스크 연동 테스트 결과 (2026-06-03)

### 테스트 환경
- `location-journey.http` 시나리오 A~E (로컬) + 시나리오 E DRIVING 추가
- `location-journey-edge.http` 시나리오 EDGE-1~6 (로컬) — 전체 정상
- `location-group.http` 시나리오 A~C (로컬) + prod(a@gmail.com, b@gmail.com)

### 플라스크 응답 정상 여부

| 항목 | DRIVING | TRANSIT 일반모드 | TRANSIT 막차모드 |
|------|---------|----------------|----------------|
| `departure_alarm_time` | ✅ | ✅ | ✅ |
| `interval` | ✅ | ✅ | ✅ |
| `which_station` | null (정상) | ✅ | ✅ |
| `boarding_time` | null (정상) | ✅ | ✅ |

### ~~플라스크 버그: `boarding_time` 누락~~ → ✅ 수정 완료

- `alarm.py` 코드 확인 결과 `boarding_dt.strftime(...)` 정상 구현되어 있음 (TRANSIT이면 반환, DRIVING이면 null)

---

## API 타입 정의 현황 (`src/api/`)

- `journeys.ts`: `LocationResponse`에 `which_station`, `boarding_time` 포함 ✅
- `appointments.ts`: `ParticipantLocationResponse`에 `which_station`, `boarding_time` 포함 ✅
- `appointments.ts`: `CreateAppointmentResponse`에 `participant_status` 포함 ✅
- `appointments.ts`: `UpdateAppointmentResponse`에 `participant_status` 포함 ✅
- `alarms.ts`: `AlarmItem`에 `my_status` 포함 ✅
- `alarms.ts`: 날짜별 조회 파라미터 `date` (오타 아님, 문서상 `data=`는 오타) ✅
