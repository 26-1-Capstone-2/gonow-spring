# GPS 폴링 버그 목록

마지막 업데이트: 2026-06-06

---

## 커밋 이력

| 커밋 | 브랜치 | 내용 |
|------|--------|------|
| `9779d4a` | fix | 단계별 알람 trigger ID AsyncStorage 저장으로 앱 재실행 후 X 버튼 취소 정상화 |
| `b6c98ed` | fix | GPS 폴링 좀비 버그 전면 수정 및 포그라운드/백그라운드 폴링 안정화 |
| `81e515f` | fix | 앱 재실행 시 DEPARTING/MOVING/NEARDEST 상태 알람도 폴링 재개 |
| `7112d52` | fix | 단계별 알람 중복 등록 방지 및 스위치 ON 시 상태 범위 확장 |
| `085f831` | fix | STAGING_DONE_KEY 관리 개선 및 알람 상태 처리 보완 |
| `795a2fd` | fix | 단계별 알람 X버튼 취소, NEARDEST P>=Q 알람, 수정 차단 조건 개선 |
| `c77ea30` | fix | 개인/귀가 알람 수정 차단 조건 MOVING만으로 완화 |
| `03e0bf1` | feat | 자동 로그인, 401 인터셉터, GPS High 정밀도 적용 |
| `67dbf2c` | feat | 백그라운드 알람 취소 구현 및 자동 로그인 타이밍 수정 |
| `d86d7d9` | fix | 그룹 알람 대시보드 버튼 활성화 조건 수정 |
| `482ab72` | fix | 날짜별 리스트 알람 추가/수정 화면 다이렉트 진입 및 UX 개선 |

---

## 수정 완료 목록

| 버그 | 파일 | 상태 |
|------|------|------|
| AppState active 중복 발화 → 좀비 runner 누적 | `_layout.tsx` | ✅ 3초 디바운스 + isRunning 가드 |
| AlarmManager 중복 start 가드 없음 | `alarmService.ts` | ✅ `isRunning()` 추가 |
| 앱 시작 시 AsyncStorage 초기화 await 누락 | `_layout.tsx` | ✅ `await` + `init()` async 래핑 |
| backgroundAlarmTask FCM data 접근 오류 | `backgroundAlarmTask.ts` | ✅ `(data as any)?.data` |
| FCM 토큰 형식 오류 (ExponentPushToken) | `LoginScreen.tsx` | ✅ `getDevicePushTokenAsync` |
| poll() stop 후 재등록 버그 | `alarmService.ts` | ✅ `if (!this.target) return` |
| FCM Priority NORMAL → HIGH 미설정 | `FcmSender.java` (서버) | ✅ `AndroidConfig.Priority.HIGH` |
| 삭제 시 alarmService.stop() + AsyncStorage 제거 누락 | 알람 Sheet 6개 | ✅ 팀원이 수정 완료 |
| 앱 시작 시 AsyncStorage 초기화 코드 누락 | `_layout.tsx` | ✅ 팀원이 수정 완료 |
| 로그아웃 시 alarmService.stopAll() + AsyncStorage 초기화 | `ProfileSettingsScreen.tsx` | ✅ 수정 완료 |
| 회원가입 시 FCM 토큰 미등록 | `LeaveTimeSetupScreen.tsx` | ✅ 수정 완료 |
| 로그아웃 API (auth.ts) 메서드 누락 | `src/api/auth.ts` | ✅ `logout()` 추가 |
| backgroundAlarmTask 포그라운드 중복 처리 | `backgroundAlarmTask.ts` | ✅ `AppState.currentState === 'active'` skip |
| Android Foreground Service background 시작 불가 | `alarmService.ts` | ✅ `start()` 내 `startBackgroundLocationUpdates()` 선호출 |
| handOffFromBackground() ID 제거 후 재등록 누락 | `alarmService.ts` | ✅ stop 후 AsyncStorage ID 재등록 |
| registerTaskAsync NullPointerException (너무 이른 호출) | `_layout.tsx` | ✅ 2초 delay + 5회 재시도 |
| AppState active 시 stop 후 startBackground 미복구 → 상단바 알림 사라짐 | `_layout.tsx` | ✅ active 핸들러에 hasRunning() 조건부 재시작 추가 |
| alarmService.stop() 시 백그라운드 추적 미종료 → 불필요한 상단바 알림 잔존 | `alarmService.ts` | ✅ runners 비면 stopBackgroundLocationUpdates 호출 |
| 백그라운드 태스크 4xx 에러 시 유령 ID 영구 잔존 | `backgroundLocationTask.ts` | ✅ HTTP 4xx → ID 즉시 제거, 네트워크 오류만 재시도 |
| FCM 포그라운드 수신 시 stop→race→즉시 재stop | `_layout.tsx` | ✅ fcmSub에서 stopBackgroundLocationUpdates 제거, alarmService.start() 내부에 위임 |

---

## 수정 완료 목록 (추가)

| 버그 | 파일 | 상태 |
|------|------|------|
| 앱 재시작 후 로그인 시 READY 알람 폴링 미시작 (쿨다운 타이밍 문제) | `_layout.tsx` | ✅ `doStartReadyAlarms` 분리 |
| 스와이프 킬 후 재실행 시 로그인 전 `/location` 호출 (이전 세션 데이터) | `backgroundLocationTask.ts`, `_layout.tsx` | ✅ `SESSION_READY_KEY` 세션 플래그 추가 |
| 로그인 전 `startReadyAlarms` 403 실패 후 쿨다운 소모 → 로그인 후 READY 알람 미시작 | `_layout.tsx` | ✅ 토큰 없으면 쿨다운 갱신 없이 skip |
| 로그인 후 화면 전환 시 AppState active 미발화 → 기존 READY 알람 폴링 미시작 | `LoginScreen.tsx` | ✅ 로그인 성공 직후 `getAlarms` + `alarmService.start()` 직접 호출 |
| `alarmService.start()` 동시 호출 시 `startBackgroundLocationUpdates` race condition → 여러 번 완료 | `backgroundLocationTask.ts` | ✅ `_startingLocationUpdates` 플래그로 동시 진입 차단 |
| STAGING_DONE_KEY 로 인한 알람 미발송 (앱 재실행/수정 시) | 여러 파일 | ✅ STAGING_DONE_KEY 전면 제거, `stagingStarted`(메모리)만 유지 |
| MOVING/NEARDEST/ARRIVED 진입 시 남은 단계별 알람 미취소 + 개인/귀가 MOVING 케이스 누락 | `alarmService.ts` | ✅ MOVING/NEARDEST/ARRIVED 진입 시 `cancelRemainingStages()` 호출 추가 |
| 알람 삭제 후에도 포그라운드 폴링 계속 (서버 오류 응답 시 stop 없음) | `alarmService.ts` | ✅ 서버 오류(`success:false`) 응답 시 자동 stop 추가 |
| 포그라운드 X 버튼 눌러도 2~4단계 취소 안 됨 | `notifications.ts`, `alarmService.ts`, `backgroundLocationTask.ts` | ✅ `sendAlarm()`에 journeyId/appointmentId data 추가 |
| NEARDEST 상태에서 P>=Q 시 단계별 알람 미발송 | `alarmService.ts` | ✅ NEARDEST 진입 시 departure_alarm_time 비교 후 scheduleAlarmStages() 호출 |
| 개인/귀가 알람 ARRIVED 상태에서 수정 불가 | `JourneyService.java`, `PersonalAlarmSheet.tsx`, `HomeAlarmSheet.tsx` | ✅ UNMODIFIABLE_STATUSES에서 ARRIVED 제거, 프론트 MOVING/NEARDEST만 차단 |
| 그룹 알람 WAITING 외 상태에서 수정 가능 | `GroupAlarmSheet.tsx` | ✅ `isArrivalActive: appointment_status !== 'WAITING'` |
| `GroupAllAlarmSheet` 대시보드 버튼 항상 비활성화 (`IN_PROGRESS` 잘못된 값) | `GroupAllAlarmSheet.tsx` | ✅ `appointment_status !== 'WAITING'`으로 수정 |
| `DailyAlarmScreen` 그룹 카드 비활성화/대시보드 버튼 미반영 | `DailyAlarmScreen.tsx` | ✅ 서버 `appointment_status` 직접 사용 |
| 날짜별 알람 카드 터치 시 목록 화면 거쳐서 수정 화면 진입 (2단계) | `PersonalAlarmSheet.tsx`, `HomeAlarmSheet.tsx`, `GroupAlarmSheet.tsx` | ✅ `initialMode=edit` 시 바로 수정 화면 진입 |
| 수정/X 버튼 후 목록 화면 거쳐 닫힘 (2단계) | 동일 3개 파일 | ✅ `initialMode=edit` 시 `onClose()` 직접 호출 |
| `ArrivalDashboardSheet` 하드코딩 더미 데이터 사용 | `daily-alarm.tsx` | ✅ `appointmentId` 전달하여 실제 API 호출 |
| 수정 화면 열릴 때 빈 화면 찰나 표시 | `PersonalAlarmSheet.tsx` 등 | ✅ `initialAlarm` prop 전달로 즉시 기본 데이터 표시 |
| 수정 화면 제목 불일치 (개인: "알람 수정", 귀가: 없음) | 4개 파일 | ✅ 개인/귀가/그룹 수정 화면 제목 통일 |
| `GroupAlarmSheet` handleLeave 탈퇴 후 `setView('list')` | `GroupAlarmSheet.tsx` | ✅ `initialMode=edit`이면 `onClose()` 호출 |
| `daily-alarm.tsx` 불필요한 `useAppointmentStatusStore` 코드 | `daily-alarm.tsx` | ✅ 제거 |
| 대시보드 버튼 `appointmentId` 미전달 | `DailyAlarmScreen.tsx`, `daily-alarm.tsx` | ✅ `onArrivalPress(appointmentId)` 전달 |
| 앱 재실행 시 로그인 화면 강제 이동 (토큰 미복원) | `_layout.tsx`, `index.tsx`, `client.ts` | ✅ 자동 로그인 + 401 인터셉터 구현 |
| 백그라운드 X버튼/YES버튼 단계별 알람 미취소 (버그10-B) | `notifications.ts` | ✅ trigger ID AsyncStorage 저장 + onBackgroundEvent 처리 추가 |
| GPS 정밀도 Balanced (100m 오차) | `alarmService.ts`, `backgroundLocationTask.ts`, `backgroundAlarmTask.ts` | ✅ Accuracy.High로 변경 |
| 개인/귀가 NEARDEST 상태 수정 차단 | `JourneyService.java`, `PersonalAlarmSheet.tsx`, `HomeAlarmSheet.tsx` | ✅ MOVING만 차단으로 완화 |
| 날짜별 리스트 카드 터치 시 목록 화면 찰나 표시 후 수정 화면 진입 | `PersonalAlarmSheet.tsx`, `HomeAlarmSheet.tsx`, `GroupAlarmSheet.tsx` | ✅ `view` 초기값을 `initialMode` 기반으로 설정 |
| + 버튼 터치 시 목록 화면 찰나 표시 후 추가 화면 진입 | 동일 3개 파일 | ✅ `initialMode='add'`일 때도 `view` 초기값으로 바로 진입 |
| X/저장/삭제 후 목록 화면 거쳐 닫힘 | 동일 3개 파일 | ✅ `initialMode` 있으면 `onClose()` 직접 호출 |
| `addChoice` 화면 `<` 버튼 → 목록 화면 거쳐 닫힘 | `GroupAlarmSheet.tsx` | ✅ `initialMode` 있으면 `onClose()` 호출 |
| 수정 화면 진입 시 `editAlarm` 초기값이 DEFAULT_ALARM → 데이터 파박 교체 | `PersonalAlarmSheet.tsx`, `HomeAlarmSheet.tsx`, `GroupAlarmSheet.tsx` | ✅ `useState` 초기값에 `initialAlarm` 데이터 반영 |
| `HomeAlarmSheet` 수정 진입 시 막차/데드라인 모드 파박 전환 | `HomeAlarmSheet.tsx` | ✅ `editAlarm` 초기값에 `mode` 포함 |
| 날짜별 리스트 Sheet `animateOnMount` 미설정으로 끝에서 튀는 현상 | `PersonalAlarmSheet.tsx`, `HomeAlarmSheet.tsx`, `GroupAlarmSheet.tsx` | ✅ `animateOnMount={false}` 추가 |

### ✅ 수정 완료 — 스와이프 킬 후 재실행 시 로그인 전 `/location` 호출

**파일**: `src/tasks/backgroundLocationTask.ts`, `app/_layout.tsx`

**원인**:
- Android `expo-task-manager` 등록 태스크는 스와이프 킬 후에도 OS 레벨에서 살아있음
- 앱 재실행 시 `init()` AsyncStorage 초기화(ACTIVE_JOURNEYS_KEY 등) 완료 전에 backgroundLocationTask가 발화 가능
- 이전 세션 ACTIVE_JOURNEYS_KEY + AsyncStorage JWT로 `/location` 호출 → 로그인 전인데 서버 요청됨

**수정 내용**:
- `backgroundLocationTask.ts`에 `SESSION_READY_KEY = 'gonow_session_ready'` 추가
- 태스크 맨 앞에 `SESSION_READY_KEY !== '1'`이면 skip 처리
- `_layout.tsx` `init()` 맨 앞에서 `SESSION_READY_KEY = '0'` 세팅 (이전 세션 무효화)
- AsyncStorage 초기화 완료 후 `SESSION_READY_KEY = '1'` 세팅 (태스크 허용)

**롤백 방법**: `backgroundLocationTask.ts`에서 `SESSION_READY_KEY` 체크 블록 제거, `_layout.tsx`에서 `SESSION_READY_KEY` setItem 2군데 제거, import에서 `SESSION_READY_KEY` 제거.

---

### ✅ 수정 완료 — 앱 재시작 후 READY 알람 폴링 미시작

**파일**: `app/_layout.tsx` 65~89번 줄

**원인**:
- `init()` 실행 시 89번 줄에서 `startReadyAlarms()` 직접 호출 → `lastStartReadyAlarmsAt = Date.now()` 갱신
- 앱 시작 직후 `background → active` AppState 이벤트가 10초 이내에 발화
- AppState active 핸들러에서 `startReadyAlarms()` 호출 시 쿨다운(10초)에 막혀 `getAlarms` 미호출
- 결과: 로그인 후 화면 전환해도 READY 알람 스캔이 안 되어 폴링 시작 안 됨

**수정 내용**:
- `doStartReadyAlarms()` — 쿨다운 없이 `getAlarms` 호출하는 실제 로직
- `startReadyAlarms()` — 10초 쿨다운 체크 후 `doStartReadyAlarms()` 호출 (AppState active 전용)
- `init()` 최초 호출은 `doStartReadyAlarms()` 직접 호출 → 쿨다운 카운터 소모 안 함

**롤백 방법**: `doStartReadyAlarms` 내용을 다시 `startReadyAlarms` 안으로 합치고, `init()` 호출을 `startReadyAlarms()`로 되돌리면 됨.

---

## 미수정 버그 목록

> 중요도 높은 순으로 정렬

---

### 🔴 버그18 — DEPARTING 진입 시 단계별 알람 중복 발송 [영향 높음]

**파일**: `src/services/alarmService.ts`

**증상**: 임계 구간(4단계) 알람이 두 세트((1/3)(2/3)(3/3) × 2) 울림

**원인**:
- READY에서 `interval !== null` 조건으로 1~4단계 OS 등록 (방금 수정)
- DEPARTING 진입 시 `cancelRemainingStages()` + 재등록 → 4단계만 남으면 또 3번짜리로 재등록
- 결과: READY 등록분 + DEPARTING 재등록분 중복

**수정 방향**: DEPARTING 진입 시 `cancelRemainingStages()` + `scheduleAlarmStages()` 호출 제거. READY에서 이미 정확한 시각으로 등록해놨으므로 DEPARTING에서 건드릴 필요 없음.

**주의**: `handlePersonalStatus`(223번 줄), `handleGroupStatus` 두 곳 모두 수정 필요.

---

### ⚠️ 임시 변경 — 테스트용 interval 30초 고정 (원복 필요) [즉시]

→ 아래 임시 변경 상세 참고

---

### 🟡 버그14 — 추방/방 삭제 시 OS 등록 단계별 알람 미취소 [영향 낮음]

→ 아래 버그14 상세 참고

---

### 🟡 버그13 — 잘못된 초대코드 "네트워크 오류" 문구 [영향 낮음, 쉬움]

→ 아래 버그13 상세 참고

---

### 🟡 버그3 — 백그라운드 폴링 interval 30초 고정 [영향 낮음]

→ 아래 버그3 상세 참고

---

### 🟡 버그9 — 상단바 알림 깜빡임 [영향 매우 낮음]

→ 아래 버그9 상세 참고

---

### 🟡 버그8 — 포그라운드 GPS 중복 찌르기 [영향 매우 낮음]

→ 아래 버그8 상세 참고

---

### 🟡 버그1 — FCM Data 포그라운드 수신 시 backgroundAlarmTask와 타이밍 gap [간헐적, 영향 미미]

→ 아래 버그1 상세 참고

---

### 🟡 버그2 — 앱 재실행 시 로그인 전 공백 [영향 매우 낮음]

→ 아래 버그2 상세 참고

---

## 미수정 버그 상세

---

### 🟡 버그1 — FCM Data 포그라운드 수신 시 backgroundAlarmTask와 타이밍 gap [간헐적, 영향 미미]

**파일**: `src/tasks/backgroundAlarmTask.ts`

**원인**:
- `backgroundAlarmTask`에 `AppState.currentState === 'active'` skip 가드가 있으나, AppState가 아직 `active`로 갱신되기 전 짧은 틈에 태스크가 실행될 수 있음
- 이 경우 `backgroundAlarmTask`가 AsyncStorage에 ID를 쓰고 `startBackgroundLocationUpdates()` 호출
- 직후 `fcmSub`의 `alarmService.start()` → `handOffFromBackground()`가 AsyncStorage ID를 지워서 충돌 없이 정리됨

**현재 증상**: 포그라운드 FCM 수신 직후 `/location`이 간헐적 1회 추가 발생 가능
**영향**: 서버는 동일 상태 반환하므로 기능적 문제 없음. 완전 방어는 AppState 타이밍 특성상 불가.

---

### 🟡 버그2 — 앱 완전 종료 후 재실행 시 GPS 폴링 복구 (로그인 전 공백)

**파일**: `src/screens/auth/LoginScreen.tsx`

**현황**: 로그인 성공 직후 `getAlarms` 호출 + `alarmService.start()` 코드 구현 완료. `isRunning()` 중복 가드도 추가 완료.
**남은 문제**: 앱 완전 종료 → 재실행 → **로그인 화면 진입 ~ 로그인 버튼 누르기 전** 구간에서는 JWT 없어서 폴링 없음. 로그인 후 즉시 복구되므로 실용적 영향 낮음.

---

### 🟡 버그3 — 백그라운드 폴링 interval 30초 고정

**파일**: `src/tasks/backgroundLocationTask.ts:52`

**현재**: `timeInterval: 30000` 하드코딩

**원인**: `expo-location startLocationUpdatesAsync`는 한번 시작하면 `timeInterval` 변경 불가. 변경하려면 stop → start 재시작 필요.

**현재 영향**: 
- 목적지 멀어도(READY 초기) 무조건 30초마다 호출 → 배터리 낭비
- 백그라운드에서는 서버가 내려주는 `interval` 값이 무시됨 (30초 고정)
- 포그라운드 전환 시 `alarmService`의 `setTimeout` 기반 동적 interval로 자동 전환되므로 크리티컬하진 않음

**수정 방향**: 포그라운드에서 서버 `interval` 수신 시 → `stopBackgroundLocationUpdates()` + `startBackgroundLocationUpdates(newInterval)` 재시작. 구현 복잡도 대비 효과 낮음 → 후순위.

---

### ~~버그4 — 추방 시 폴링 미중단~~ → ✅ 해결됨

`backgroundLocationTask.ts` HTTP 4xx → ID 즉시 제거 수정으로 해결.
추방 후 다음 `/location` 호출 시 서버 404 → ID 자동 제거 → 폴링 자동 종료.

---

### ~~버그5 — 플라스크 `boarding_time` 누락~~ → ✅ 이미 수정됨

코드 확인 결과 `alarm.py` 257번 줄에 정상 구현되어 있음.
TRANSIT이면 `departure_time + walk_min`, DRIVING이면 `null` 반환.

---

### ~~버그6 — GroupAllAlarmSheet 스와이프 삭제 로직~~ → ✅ 문제 없음

코드 확인 결과 `myMemberId`로 방장 여부를 판단해 `deleteAppointment` / `removeParticipant` 분기 처리.
두 경우 모두 `alarmService.stop(undefined, alarm.appointmentId)`는 **내 폴링**을 멈추는 것이므로 정상.

---

### ~~버그7 — 그룹 알람 스위치 OFF 시 프론트 로컬 알람 미억제~~ → ✅ 해결됨

- `alarmService.setActive(isActive)` 메서드 추가 — 스위치 토글 시 runner의 `isActive` 즉시 반영
- OFF 시 `cancelRemainingStages()` 호출 → 등록된 2~4단계 취소
- ON 시 DEPARTING 상태면 현재 단계부터 재등록
- `GroupAlarmSheet`, `GroupAllAlarmSheet`, `DailyAlarmScreen` 토글 핸들러에 `setActive()` 호출 추가

---

### ~~개인/귀가 스위치 OFF 시 단계별 알람 미취소~~ → ✅ 해결됨

`alarmService.stop()` 내부에 `cancelRemainingStages()` 호출 추가로 해결.
스위치 OFF → `stop()` → OS 등록된 2~4단계 알람 자동 취소.

---

### ⚠️ 임시 변경 — 테스트용 폴링 주기 20초 (원복 필요)

**파일 1**: `src/services/alarmService.ts` — ✅ DEFAULT_INTERVAL = 30으로 원복 완료

**파일 2**: `src/tasks/backgroundLocationTask.ts` — ✅ 30000으로 원복 완료

---

### ~~⚠️ 임시 변경 — 테스트용 interval 30초 고정~~ → ✅ 원복 완료

**파일**: `src/services/alarmService.ts`

`pollPersonal`, `pollGroup` 두 곳 모두 `this.intervalSec = interval`로 원복. 서버가 내려주는 `interval` 값으로 포그라운드 폴링 주기 동적 갱신.

---

### ~~버그12 — DEPARTING 상태에서 목표 시각 변경 시 새 출발 알람 미발송~~ → ✅ 해결됨

서버가 알람 수정 시 항상 READY로 리셋 + `currentPos = null` 초기화하므로 프론트가 READY를 거쳐 DEPARTING 재감지 → `stagingStarted` 자동 리셋 → 1단계부터 정상 발송.

---

### ~~버그11 — NEARDEST 상태에서 P >= Q 시 단계별 알람 미발송~~ → ✅ 해결됨

`handlePersonalStatus`/`handleGroupStatus`에서 NEARDEST 진입 시 `departure_alarm_time`과 현재 시각 비교하여 P>=Q이면 `scheduleAlarmStages()` 호출 추가.

---


### ~~버그10-A — 포그라운드 X 버튼 눌러도 2~4단계 취소 안 됨~~ → ✅ 해결됨

`sendAlarm()`에 `journeyId`/`appointmentId` 파라미터 추가 및 `data`에 포함.
이제 X 버튼 누르면 `onForegroundEvent`에서 ID를 정상 읽어 `cancelRemainingStages()` 작동.

---

### 🟡 버그10-A — 포그라운드 X 버튼 눌러도 2~4단계 취소 안 됨 [영향 높음]

**파일**: `src/utils/notifications.ts` — `sendAlarm()` 함수

**원인**:
- `sendAlarm()`(1단계 즉시 발송)에 `journeyId`/`appointmentId`를 data에 담지 않음
- X 버튼 누를 때 `_layout.tsx` `onForegroundEvent`에서 `data.journeyId = undefined` → `cancelRemainingStages(undefined, undefined)` 호출 → 아무것도 취소 안 됨

**수정 방향**: `sendAlarm()` 파라미터에 `journeyId?`, `appointmentId?` 추가 후 `data`에 포함.

---

### 🟡 버그10-B — 백그라운드에서 단계별 알람 dismiss 시 나머지 단계 미취소 [영향 중간]

**파일**: `src/utils/notifications.ts`

**원인**:
- 포그라운드에서 dismiss 버튼 누르면 `_layout.tsx` `onForegroundEvent`에서 `cancelRemainingStages()` 호출 → 정상
- 백그라운드/종료 상태에서 알림 스와이프 dismiss 시 `onForegroundEvent` 실행 안 됨
- `notifications.ts`에 `notifee.onBackgroundEvent`가 있으나 `dismiss` 처리 없음

**현재 영향**: 백그라운드에서 1단계 알람 닫아도 2~4단계가 예약된 시각에 계속 울림. 앱 종료 상태에서는 구조적으로 불가.

**수정 방향**:
- `scheduleFutureAlarm()`으로 등록한 trigger notification ID들을 AsyncStorage에 `journeyId`/`appointmentId` 기준으로 저장
- `onBackgroundEvent`에서 `actionId === 'dismiss'` 시 AsyncStorage에서 해당 ID 목록 조회 → `notifee.cancelTriggerNotification()` 직접 호출
- `alarmService.stageTriggerIds`는 메모리 변수라 백그라운드에서 접근 불가 → AsyncStorage 경유 필수

---

### 🟡 버그9 — 3초 중복 active 시 상단바 알림 깜빡임 [영향 낮음]

**파일**: `app/_layout.tsx` 98~106번 줄

**원인**:
- Android에서 AppState `active`가 짧은 간격으로 연속 발화하는 경우가 있음 (알림 탭, GPS 권한 다이얼로그 닫힘 등)
- 이때 3초 중복 가드가 작동하지만, `stopBackgroundLocationUpdates()`를 **가드 체크 전에 무조건 실행**함
- 중복 active 케이스에서도 stop → start 왕복이 발생 → 상단바 "GoNow 알람 실행 중" 알림 순간 깜빡임

**현재 코드 흐름**:
```ts
await stopBackgroundLocationUpdates();  // 무조건 stop
if (now - lastForegroundAt < 3000) {    // 중복 판별
  if (alarmService.hasRunning()) {
    await startBackgroundLocationUpdates(); // 바로 재start → 불필요한 왕복
  }
  return;
}
```

**수정 방향**: `stopBackgroundLocationUpdates()` 호출을 3초 가드 통과 후로 이동. 단, `backgroundAlarmTask` 경로(foregroundService 없음)를 foregroundService 포함 버전으로 교체하려면 stop이 필요한 구조적 이유도 있어 신중히 수정.

**현재 영향**: 기능 문제 없음. 상단바 알림 깜빡임 UX만 저하. **후순위**.

---

### 🟡 버그8 — 포그라운드에서 GPS 하드웨어 중복 찌르기 (배터리 낭비)

**현상**: 포그라운드일 때 GPS 하드웨어가 두 번 찍힘
- `alarmService` → `getCurrentPositionAsync()` (서버 interval 주기)
- `backgroundLocationTask` → OS GPS 업데이트 (30초 고정) → skip하지만 GPS는 이미 찍힘

**결과**: 서버 interval이 300초여도 backgroundLocationTask가 30초마다 GPS를 찌름 → interval 의미 없음 + 배터리 낭비

**해결책**: 포그라운드 진입 시 `startLocationUpdatesAsync`를 `Accuracy.Low` + 긴 `timeInterval`로 재호출하여 OS GPS 발화 최소화. 백그라운드 진입 시 `Accuracy.Balanced` + 30초로 복구.

**수정 파일**:
- `src/tasks/backgroundLocationTask.ts` — `startBackgroundLocationUpdates(accuracy, interval)` 파라미터 추가
- `app/_layout.tsx` — AppState active/background 핸들러에서 각각 다른 옵션으로 호출

**롤백 방법**: `startBackgroundLocationUpdates()` 호출부에서 파라미터 제거하면 원래 상태로 돌아감

---

### ✅ 수정 완료 — ForegroundServiceDidNotStartInTimeException 크래시

**원인**: `alarmService.start()` 내부에서 `startBackgroundLocationUpdates()`를 백그라운드 상태에서 호출 → Android OS가 5초 내 `startForeground()` 미호출로 앱 강제 종료

**수정**: `AppState.currentState === 'active'` 체크 추가 → 포그라운드일 때만 호출
**파일**: `src/services/alarmService.ts`

---

### ✅ 수정 완료 — notifee Promise.all 동시 호출 SIGABRT 크래시

**원인**: `scheduleAlarmStages()`에서 `Promise.all`로 notifee 알람 등록을 동시 호출 → 네이티브 메모리(Scudo) 충돌 → `signal 6 (SIGABRT)` 크래시

**수정**: `Promise.all` → 순차 실행(`await` 직렬화)
**파일**: `src/services/alarmService.ts`, `src/tasks/backgroundLocationTask.ts`

---

---

## 플라스크 버그 목록

### ~~플라스크 버그1 — `estimated_arrival` 계산 오류~~ → ✅ 해결됨

```python
# 수정 후
estimated_arrival = datetime.now() + timedelta(seconds=duration_sec)
```
현재 위치 기준 실제 ETA로 수정. `_compute_appointment_alarm()` 및 `_compute_alarm()` 모두 반영.

---

### ~~플라스크 버그2 — 막차 모드 `target_time` 의미 오류~~ → ✅ 해결됨

```python
# 수정 후
"target_time": last_arrival_dt.strftime("%Y-%m-%dT%H:%M:%S")
```
막차 출발 시각 → 막차 타고 집 도착 시각으로 수정. 스프링의 자동 ARRIVED 타이밍 정상화.

---

### ~~버그15 — 스위치 OFF→ON / 앱 재실행 시 DEPARTING 1단계 재발송~~ → ✅ 해결됨

- READY 상태에서 `departure_alarm_time` + `which_station` 수신 시 1~4단계 전부 절대 시각으로 OS 예약
- DEPARTING 진입 시 기존 취소 + `startIdx` 현재 시각 기준으로 해당 단계부터 재등록
- 앱 재실행/알람 수정 후 DEPARTING 직접 진입 시에도 `scheduleAlarmStages()` 호출로 정상 처리
- `backgroundLocationTask.ts`도 동일 로직 적용 (취소 후 재등록, 건너뛰기)

---

### 🟡 버그14 — 추방/방 삭제 시 OS 등록 단계별 알람 미취소 [영향 낮음]

**원인**:
- 방장이 참가자 추방 또는 방 삭제 시 피해 참가자는 서버 404로 폴링만 중단됨
- OS에 이미 등록된 2~4단계 단계별 알람은 취소되지 않고 예정 시각에 계속 울림

**수정 방향**: FCM으로 추방/삭제 사실을 피해 참가자에게 알리고, 프론트에서 `cancelRemainingStages()` 호출. 또는 `notifee.cancelAllTriggerNotifications()`로 전체 취소.

---

### 🟡 버그13 — 잘못된 초대코드 입력 시 "네트워크 오류" 문구 표시 [영향 낮음]

**파일**: `src/screens/alarmManage/GroupAlarmSheet.tsx:167`, `src/screens/allAlarmManage/GroupAllAlarmSheet.tsx:197`

**원인**: `catch` 블록에서 모든 에러를 "네트워크 오류가 발생했습니다."로 표시. 잘못된 초대코드(400)도 동일 문구.

**수정 방향**: `catch (e: any)`로 변경 후 서버 에러 메시지 파싱하거나 "초대코드를 확인해주세요." 등 적절한 문구로 교체.

---

---

### ~~버그16 — 시간대(TimeZone) 미통일~~ → ✅ 확인 완료 (문제 없음)

**확인 결과**:
- **스프링**: `hibernate.jdbc.time_zone: Asia/Seoul` 설정 완료 ✅
- **MySQL**: TZ 미설정(UTC)이나 스프링 JPA가 KST로 변환해서 저장/읽기 → 실질적 문제 없음 ✅
- **플라스크**: KST 기준으로 수정 완료 ✅
- **프론트**: JS `new Date()`는 기기 시간대 자동 사용, 한국 폰 기준 KST ✅

실제 테스트에서 출발 알람 시각 정상 확인됨 — 레이어 간 시간대 불일치 없음.

---

### 🟡 버그17 — 자정~새벽 4시 날짜 경계 처리 [영향 미확인]

**관련 레이어**: 프론트, 스프링 스케줄러

**현황**: 스케줄러가 매일 새벽 4시에 당일 알람을 READY로 전환하므로, 00:00~04:00 구간은 아직 "어제" 기준으로 동작함. 그러나 앱은 자정이 넘으면 날짜를 오늘(`new Date()`)로 계산하여 "오늘 알람"을 조회함.

**증상 시나리오**:
- 자정 넘긴 새벽 1시에 앱 실행 → 캘린더는 6월 6일 표시
- 서버는 아직 6월 6일 알람을 READY 전환 안 함 (새벽 4시 전)
- 알람 목록 조회 시 6월 6일 알람이 SCHEDULED 상태로 표시됨 (정상이긴 하나 혼란)
- FCM도 새벽 4시에 오므로 GPS 폴링 미시작

**정책 결정 필요**:
- 00:00~04:00을 "전날"로 간주하여 날짜 조회 시 하루 빼기
- 또는 현행 유지 (SCHEDULED 상태로 보여주되 사용자 혼란 감수)

**수정 방향 (정책 확정 후)**:
```ts
// 프론트: 현재 시각이 04:00 이전이면 전날 날짜 사용
function getEffectiveDate(): string {
  const now = new Date();
  if (now.getHours() < 4) now.setDate(now.getDate() - 1);
  return now.toISOString().slice(0, 10);
}
```

---

## 인과관계 요약

```
현재 남은 실질적 문제:

버그18 (DEPARTING 단계별 알람 중복)  → 임계 구간 두 세트 울림, 수정 필요

버그1 (FCM 포그라운드 타이밍 gap)    → 간헐적 /location 1회 추가, 기능 영향 없음, 완전 방어 불가
버그2 (앱 재실행 폴링 복구)           → 로그인 전까지 폴링 없음, 로그인 후 즉시 복구되므로 영향 낮음
버그3 (백그라운드 30초 고정)          → 배터리 비효율, 기능 영향 없음, 후순위
버그7 (그룹 스위치 OFF 알람 미억제)   → ✅ 해결됨 (setActive() 메서드 추가, 토글 핸들러 연결)
버그9 (3초 중복 active 상단바 깜빡임) → 기능 영향 없음, 후순위
버그14 (추방/방 삭제 시 단계별 알람 미취소) → 낮음, FCM 연동 필요
버그15 (앱 재실행/스위치 ON 시 1단계 재발송) → ✅ 해결됨 (READY에서 절대 시각 예약, 건너뛰기 로직)
버그16 (시간대 미통일)               → ✅ 확인 완료, 문제 없음
버그17 (자정~새벽 4시 날짜 경계)     → 정책 결정 후 수정, 현행 유지도 가능
⚠️ interval 30초 고정               → ✅ 원복 완료
```
