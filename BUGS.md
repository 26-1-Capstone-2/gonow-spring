# GPS 폴링 버그 목록

마지막 업데이트: 2026-06-04

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

## 미수정 버그 목록

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

### 🟡 버그7 — 그룹 알람 스위치 OFF 시 프론트 로컬 알람 미억제

**파일**: `src/services/alarmService.ts`, `app/_layout.tsx`, `src/screens/auth/LoginScreen.tsx`, `GroupAlarmSheet.tsx`, `GroupAllAlarmSheet.tsx`

**현황**:
- `alarmService.ts`에 `isActive` 플래그 및 억제 로직 구현 완료
- 서버 `ParticipantRepository.findFcmTokensByAppointmentIdExcluding` 쿼리에 `AND p.isActive = true` 추가 완료 (다른 참가자에게 FCM 미발송)
- **미완료**: `alarmService.start()` 호출부에서 `isActive: a.is_active` 전달 누락

**수정 필요 위치**:
```ts
// _layout.tsx startReadyAlarms — AlarmItem 기반
alarmService.start({ alarmType: 'group', destination: a.dest_name, appointmentId: a.appointment_id, isActive: a.is_active })

// LoginScreen.tsx 로그인 후 복구 — AlarmItem 기반
alarmService.start({ alarmType: 'group', destination: a.dest_name, appointmentId: a.appointment_id, isActive: a.is_active })

// GroupAlarmSheet.tsx, GroupAllAlarmSheet.tsx — 생성/참여 시 응답의 is_active 전달
// getAppointment() 경로(FCM 수신)는 is_active 없으므로 기본값 true 유지
```

**현재 영향**: 스위치 OFF여도 로컬 출발 알람(1~4단계), MOVING/ARRIVED/NEARDEST 알람이 울림.

---

### ⚠️ 임시 변경 — 테스트용 interval 30초 고정 (원복 필요)

**파일**: `src/services/alarmService.ts`

**변경 내용**: 서버가 내려주는 `interval` 값을 무시하고 포그라운드 폴링 주기를 30초로 고정.
테스트 목적으로만 적용. **테스트 완료 후 반드시 원복.**

**원복 위치**: `alarmService.ts`에서 `TODO` 주석 검색 → 고정값 줄 삭제 + 주석 해제

```
// pollPersonal (약 190번 줄)
if (interval !== null) {
  // TODO: 테스트 완료 후 아래 두 줄 원복
  // console.log(`[포그라운드] interval 갱신 ...`);
  // this.intervalSec = interval;
  this.intervalSec = 30; // ← 이 줄 삭제
}

// pollGroup (약 226번 줄)
if (interval !== null) {
  // TODO: 테스트 완료 후 아래 두 줄 원복
  // console.log(`[포그라운드] interval 갱신 ...`);
  // this.intervalSec = interval;
  this.intervalSec = 30; // ← 이 줄 삭제
}
```

---

## 인과관계 요약

```
현재 남은 실질적 문제:

버그1 (FCM 포그라운드 타이밍 gap)    → 간헐적 /location 1회 추가, 기능 영향 없음, 완전 방어 불가
버그2 (앱 재실행 폴링 복구)           → 로그인 전까지 폴링 없음, 로그인 후 즉시 복구되므로 영향 낮음
버그3 (백그라운드 30초 고정)          → 배터리 비효율, 기능 영향 없음, 후순위
버그5 (플라스크 boarding_time)        → ✅ 이미 수정됨 (alarm.py 257번 줄 확인)
버그7 (그룹 스위치 OFF 알람 미억제)   → 호출부에서 isActive 전달만 추가하면 완성
⚠️ interval 30초 고정               → 테스트 완료 후 원복 필수
```
