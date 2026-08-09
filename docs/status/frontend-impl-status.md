# 프론트엔드 구현 현황

프론트 경로: `D:\gonow-app\GoNow_Fronted`
플라스크 경로: `D:\gonow-flask\CounterClockEngine`
마지막 확인일: 2026-08-08(TRANSIT 모드 카카오맵 딥링크 확장 + `PriorityType.MIN_WAIT` 추가 반영)

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
- ✅ 앱 완전 종료 후 재실행 시 로그인 화면 진입 → 로그인 성공 직후 `LoginScreen.tsx`에서 `getAlarms` 호출 + `alarmService.start()` → READY 알람 폴링 즉시 복구

### FCM Data 수신 처리 (`_layout.tsx`)

| FCM 페이로드 | 처리 |
|------------|------|
| `journey_ids`, `appointment_ids` | 새벽 4시 READY 전환 트리거 → GPS 폴링 시작 |
| `appointment_id` + `participant_status: READY` | 방장 약속 수정 동기화 → GPS 시작 |
| `appointment_id` + `participant_status: SCHEDULED` | 방장 약속 수정 동기화 → GPS 중단 |

### 알람 수정 버튼 비활성화

- **개인/귀가**: `my_status`가 `MOVING`이면 카드 터치 차단 + 반투명 처리 (NEARDEST, ARRIVED는 수정 허용)
- **그룹**: `appointment_status == ACTIVE`이면 카드 터치 차단 + 반투명 처리

### 단계별 출발 알람 등록 로직

#### READY 상태에서 1~4단계 전부 OS 예약
- `/location` 응답으로 `departure_alarm_time`이 변경되면(`departureAlarmTime !== lastDepartureAlarmTime`) 즉시 1~4단계 전부 `scheduleFutureAlarm()` (OS 스케줄 등록)
- **1단계 포함 전부 OS 등록** → 앱 완전 종료 상태에서도 예정 시각에 울림
- `lastDepartureAlarmTime` 메모리 변수로 중복 재등록 방지 (같은 값이면 skip)
- 앱 재실행 시 `lastDepartureAlarmTime = null` 초기화 → `/location` 응답 받으면 `cancelRemainingStages()` 후 재등록 (중복 없음)

#### 단계별 시각 계산
- `stepTimes[0] = departure_alarm_time` (1단계)
- `stepTimes[1~3]` = `departure_alarm_time + preparationTime * 25% * (1~3)` (2~4단계)
- 등록 시점에 이미 지난 단계는 `startIdx`로 건너뜀 → 남은 단계부터 등록

#### 알람 텍스트
- `which_station` 있으면: `[목적지] {역명} 탑승까지 N분 남았어요`
- `which_station` 없으면: 기본 메시지 (`귀가 준비를 시작하세요` 등)
- 귀가(home) 알람은 `isLastMode`(막차 모드) 여부로 3·4단계 문구가 갈림 — 막차 모드만 "막차를 놓칠 수 있어요" 류 문구, 데드라인 모드(자가용 포함)는 중립 문구(`HOME_LAST_TRAIN_MESSAGES` 분기, 버그30 수정)
- 1~3단계를 건너뛰고 4단계가 바로 발송되는("이미 늦음") 경우: 제목 `🔴 지각 구간` + 문구 `이미 출발 시각이 지났어요! 지금 바로 출발하세요.`(`LATE_STAGE4_MESSAGES`) — 정상적으로 4단계까지 도달한 경우(`🔴 임계 구간`, `즉시 출발!...`)와 구분됨
- 1~3단계의 "닫기" 액션 버튼 라벨은 `✕ 이후 알림 끄기`(실제 동작이 "남은 단계 전체 취소"라는 걸 명확히 전달하도록 개선)

#### NEARDEST 상태
- NEARDEST 진입 시 단계별 알람 **미발송** (도착 확인 알람만 표시)
- 단, `departure_alarm_time` 변경 시 재등록 조건은 동일하게 적용

### `which_station`, `boarding_time` 응답 처리

- `/location` 응답에서 `which_station`은 실제로 수신·파싱해서 알람 텍스트에 반영됨(`[목적지] {역명} 탑승까지 N분 남았어요`).
- **`boarding_time`은 타입 정의(`LocationResponse`/`ParticipantLocationResponse`)에는 있지만 실제로는 어디서도 구조분해되지 않고 버려짐** — `alarmService.ts`의 `pollPersonal`/`pollGroup`, `backgroundLocationTask.ts` 전부 미파싱 확인됨(코드 직접 확인, 이전 버전 문서의 "응답에서 수신" 서술은 부정확했음).
- DRIVING(자가용)이거나 플라스크 미호출 시 `which_station`은 null → 기본 메시지 사용

### 카카오맵 딥링크 "길찾기" 버튼 (자가용 전용, 신규)

- 인앱 알람 카드(개인/귀가/그룹 리스트 4곳) + 출발 단계별(1~4단계) 푸시 알림 액션 버튼 양쪽에 제공. `DailyAlarmScreen.tsx`/`PersonalAllAlarmSheet.tsx`/`HomeAllAlarmSheet.tsx`/`GroupAllAlarmSheet.tsx`, `notifications.ts`(`scheduleFutureAlarm`), `app/_layout.tsx`/`notifications.ts`의 액션 핸들러, `src/utils/kakaoMapDeeplink.ts`(신규)
- 이동수단이 DRIVING(자가용)이고 `myStatus`가 `DEPARTING`/`MOVING`일 때만 노출(`NEARDEST` 제외)
- 앱이 백그라운드로 전환돼 헤드리스 GPS 추적 경로(`backgroundLocationTask.ts`)를 타도 버튼이 유지되도록, 목적지 좌표를 `alarmService.ts`의 `AlarmRunner.start()`가 AsyncStorage(`ALARM_NAV_INFO_KEY`)에 캐싱해서 헤드리스 경로가 읽어감
- 상세 설계/실기기 검증 기록은 `docs/reference/kakao-map-deeplink-spec.md` 참고. DRIVING 버전은 `GoNow_Fronted`의 `fix` 브랜치에 커밋 완료(`7ffedcb`, `61a9872`)
- **TRANSIT(대중교통) 모드 확장 완료** — DRIVING과 동일한 단일 딥링크(`by='publictransit'`)로 `isDriving: boolean` → `transportMode: 'car' | 'publictransit'` 배선 전체 교체(`alarmService.ts`/`backgroundLocationTask.ts`/`notifications.ts`/`app/_layout.tsx`/카드 6종 전부). 인앱 카드 버튼(날짜별 + 전체보기 화면) + 푸시 알림 액션 버튼(상단바 알림) 전부 실기기 검증 완료 — 전체보기 화면 3곳(`PersonalAllAlarmSheet.tsx`/`HomeAllAlarmSheet.tsx`/`GroupAllAlarmSheet.tsx`)이 자체 `canNavigate`를 갖고 있어 배선 교체에서 누락됐던 버그를 실기기 테스트로 발견·수정함(`docs/reference/kakao-map-deeplink-spec.md` 3부 10번 참고)

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
- `alarms.ts`: `AlarmItem`에 `dest_lat`/`dest_lng` 포함(카카오맵 딥링크 목적지 좌표용, 스프링 `AlarmResponse`와 동기화) ✅
- `alarms.ts`: 날짜별 조회 파라미터 `date` (오타 아님, 문서상 `data=`는 오타) ✅
