# GPS 폴링 버그 — 해결된 항목 아카이브

마지막 업데이트: 2026-07-31

미해결 버그는 루트 `BUGS.md` 참고.

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

## 수정 완료 목록 (2026-07-28)

| 버그 | 파일 | 상태 |
|------|------|------|
| 초대코드로 그룹 참여 실패 시 항상 "네트워크 오류" 문구만 표시 (버그13) | `GroupAlarmSheet.tsx`, `GroupAllAlarmSheet.tsx` | ✅ 서버가 보낸 실제 실패 사유(message)를 파싱해서 표시, 파싱 실패(진짜 네트워크 단절) 시에만 기존 문구 유지 |
| `repeatDays`(반복요일) 범위(0~127) 검증 누락 | `HomeJourneyCreateRequest.java`, `HomeJourneyUpdateRequest.java`, `PersonalJourneyCreateRequest.java`, `PersonalJourneyUpdateRequest.java` | ✅ `@Range(min=0, max=127)` 추가 |
| `preparationTime`(여유시간) 음수 허용 | `SignupRequest.java`, `SettingUpdateRequest.java` | ✅ `@Min(value=0)` 추가 |
| 다크모드 기기에서 오전/오후·시·분 Picker 글씨가 안 보임(배경은 라이트로 고정인데 텍스트만 시스템 다크 테마 기본값을 따라감) | 알람 Sheet 6개(`Group`/`Personal`/`Home` × 일반/전체) | ✅ 각 `Picker.Item`에 `color="#1A1A1A"` 직접 지정 + `app.json`의 `userInterfaceStyle`을 `automatic` → `light`로 고정 |
| `GroupAlarmSheet`/`GroupAllAlarmSheet` 내부 목록이 `alarmVersion` 미구독 → 참가자 변경/삭제 후에도 목록에 카드가 그대로 남아있음 | 두 파일 | ✅ 목록 로딩 `useEffect`의 의존성 배열에 `alarmVersion` 추가 |
| 비밀번호 최소 길이/복잡도 검증 없음 (버그20) | `SignupRequest.java`, `PasswordUpdateRequest.java`, `SignUpScreen.tsx`, `ChangePasswordScreen.tsx` | ✅ `@Pattern(regexp = "^[\\x21-\\x7E]{8,64}$")` — 공백 없는 아스키 출력 문자(영문/숫자/특수문자, 조합 강제 없음) 8~64자, 프론트에도 동일 정규식으로 실시간 검증 + 에러 문구 추가 |
| `GET /api/members/check?email=`에 이메일 형식 검증 없음 → 형식이 틀린 값도 "사용 가능"으로 응답해 프론트에 초록 체크가 잘못 표시됨 | `MemberController.java`, `GlobalExceptionHandler.java`, `SignUpScreen.tsx` | ✅ 컨트롤러에 `@Validated` + 파라미터에 `@Email` 추가, `ConstraintViolationException` 핸들러 신규 추가(400 응답), 프론트도 API 호출 전 `EMAIL_REGEX`로 선검증(디바운스 타이머 클리어 순서 버그도 같이 수정) |

### ✅ 신규 구현 — 그룹 알람 참가자/약속 정보 실시간 동기화 (FCM Data)

**배경**: 그룹 알람 상세화면을 열어둔 상태에서 다른 참가자가 참여/탈퇴/추방되거나 이동수단·약속 정보가 바뀌어도, 화면을 벗어났다가 다시 들어와야만(재마운트) 반영됐음.

**적용 범위**:
- 스프링: `AppointmentService.joinAppointment()`, `AppointmentService.deleteAppointment()`, `ParticipantService.deleteParticipant()`, `ParticipantService.updateTransportType()`에서 관련 참가자들에게 FCM Data(`sync_event: participants_changed` / `appointment_deleted`) 발송. 추방인 경우 쫓겨난 당사자 본인에게만 별도로 `removed_from_appointment`를 단건 발송(`FcmSender.sendData`)
- 프론트: `app/_layout.tsx`의 FCM 리스너에 3개 분기 추가, `appointmentStatusStore`에 `participantsVersion`/`deletedAppointmentId`/`removedAppointmentId` 추가, `GroupAlarmSheet`/`GroupAllAlarmSheet`가 이를 구독해 상세정보 재조회·강제 종료(Alert)·목록 새로고침(`bumpAlarmVersion`)을 수행

**참고**: 약속 삭제/추방 시 OS에 등록된 로컬 단계별 알람 취소는 이번 범위에서 제외(버그14와 함께 별도 처리 예정)

### ✅ 신규 구현 — 필수 권한(알림/위치/알람/배터리) 온보딩 화면 (gonow-app)

**배경**: 위치 항상 허용/정확한 알람/배터리 최적화 제외/알림 권한을 켜지 않으면 GPS 폴링·출발 알람이 조용히 실패하는데, 기존엔 아무 안내 없이 앱 시작 시 알림 팝업만 툭 뜨고 끝이었음.

**적용 범위**:
- `src/utils/permissions.ts`(신규) — 위치 상태 확인/요청, 알람·배터리 설정화면 이동(순수 RN `Linking` 코어 API만 사용, 추가 네이티브 모듈 없이 재빌드 불필요)
- `src/utils/notifications.ts` — `getNotificationPermissionGranted()`, `getExactAlarmGranted()` 추가(notifee 기존 API로 팝업 없이 상태만 확인)
- `src/screens/auth/PermissionSetupScreen.tsx`(신규) — 4개 권한 카드 온보딩 화면, 회원가입 직후 자동 진입 + 설정 화면에서 재방문 가능
- `app/_layout.tsx` — 앱 시작 시 자동으로 뜨던 알림 권한 팝업 제거(이 화면에서 맥락과 함께 요청하도록 이동)

**개발 중 발견/해결한 버그**:
- 위치 권한을 반복 거부하면 안드로이드가 이후 요청부터 팝업 자체를 안 띄우는데(`canAskAgain: false`), 이 상태를 대비 안 해서 "버튼을 눌러도 반응 없음"처럼 보이던 문제 → 알림 권한과 동일하게 Alert+"설정으로 이동" 안내 추가
- `Linking.sendIntent()`가 `Promise`를 반환하는데 `await` 없이 동기 `try/catch`로만 감싸서 일부 기기에서 Unhandled Promise Rejection 위험이 있던 것 → `.catch()`로 수정
- 회원가입 완료 후 "완료" 버튼이 `canGoBack()`으로 진입 경로(회원가입 중 vs 설정 화면)를 구분하려 했으나, 회원가입 스택에도 뒤로 갈 화면이 남아있어 실제로는 항상 `true`가 되어 **메인 화면 대신 회원가입 중간 화면(귀가지 설정)으로 되돌아가던 심각한 버그** → `goToPermissionSetup()`에 `fromOnboarding` 라우팅 파라미터를 명시적으로 붙여서 구분하도록 수정
- 권한이 다 꺼져있어 카드 4개 + 버튼이 전부 보이는 최악의 경우, 화면 하단 "완료" 버튼이 화면 밖으로 밀려나 안 보이던 문제 → 카드 목록을 `ScrollView`로 감싸고 "완료" 버튼은 하단 고정

**시도했다가 되돌린 것(참고용)**: 알람/배터리 설정화면으로 gonow 앱을 바로 찾아가게(`expo-intent-launcher` + `data` URI) 만들려 했으나, 실제 기기에서 이 네이티브 모듈이 재빌드 전 dev client에 없어 `Cannot find native module 'ExpoIntentLauncher'` 크래시 발생 → 전체 목록 화면으로 이동만 시키는 원래 방식(순수 `Linking.sendIntent`)으로 롤백. 배터리는 상태 확인 API 자체가 없어 완료 표시 불가(자동 확인 불가능한 채로 안내만 제공).

---

## 미수정 버그 상세 중 사후에 해결된 항목

> 최초 작성 시 미해결 상태로 기록되었다가 이후 커밋으로 해결된 항목들입니다.

### ~~버그4 — 추방 시 폴링 미중단~~ → ✅ 해결됨

`backgroundLocationTask.ts` HTTP 4xx → ID 즉시 제거 수정으로 해결.
추방 후 다음 `/location` 호출 시 서버 404 → ID 자동 제거 → 폴링 자동 종료.

### ~~버그5 — 플라스크 `boarding_time` 누락~~ → ✅ 이미 수정됨

코드 확인 결과 `alarm.py` 257번 줄에 정상 구현되어 있음.
TRANSIT이면 `departure_time + walk_min`, DRIVING이면 `null` 반환.

### ~~버그6 — GroupAllAlarmSheet 스와이프 삭제 로직~~ → ✅ 문제 없음

코드 확인 결과 `myMemberId`로 방장 여부를 판단해 `deleteAppointment` / `removeParticipant` 분기 처리.
두 경우 모두 `alarmService.stop(undefined, alarm.appointmentId)`는 **내 폴링**을 멈추는 것이므로 정상.

### ~~버그7 — 그룹 알람 스위치 OFF 시 프론트 로컬 알람 미억제~~ → ✅ 해결됨

- `alarmService.setActive(isActive)` 메서드 추가 — 스위치 토글 시 runner의 `isActive` 즉시 반영
- OFF 시 `cancelRemainingStages()` 호출 → 등록된 2~4단계 취소
- ON 시 DEPARTING 상태면 현재 단계부터 재등록
- `GroupAlarmSheet`, `GroupAllAlarmSheet`, `DailyAlarmScreen` 토글 핸들러에 `setActive()` 호출 추가

### ~~개인/귀가 스위치 OFF 시 단계별 알람 미취소~~ → ✅ 해결됨

`alarmService.stop()` 내부에 `cancelRemainingStages()` 호출 추가로 해결.
스위치 OFF → `stop()` → OS 등록된 2~4단계 알람 자동 취소.

### ⚠️ 임시 변경 — 테스트용 폴링 주기 20초 → ✅ 원복 완료

**파일 1**: `src/services/alarmService.ts` — ✅ DEFAULT_INTERVAL = 30으로 원복 완료

**파일 2**: `src/tasks/backgroundLocationTask.ts` — ✅ 30000으로 원복 완료

### ~~⚠️ 임시 변경 — 테스트용 interval 30초 고정~~ → ✅ 원복 완료

**파일**: `src/services/alarmService.ts`

`pollPersonal`, `pollGroup` 두 곳 모두 `this.intervalSec = interval`로 원복. 서버가 내려주는 `interval` 값으로 포그라운드 폴링 주기 동적 갱신.

### ~~버그12 — DEPARTING 상태에서 목표 시각 변경 시 새 출발 알람 미발송~~ → ✅ 해결됨

서버가 알람 수정 시 항상 READY로 리셋 + `currentPos = null` 초기화하므로 프론트가 READY를 거쳐 DEPARTING 재감지 → `stagingStarted` 자동 리셋 → 1단계부터 정상 발송.

### ~~버그11 — NEARDEST 상태에서 P >= Q 시 단계별 알람 미발송~~ → ✅ 해결됨

`handlePersonalStatus`/`handleGroupStatus`에서 NEARDEST 진입 시 `departure_alarm_time`과 현재 시각 비교하여 P>=Q이면 `scheduleAlarmStages()` 호출 추가.

### ~~버그10-A — 포그라운드 X 버튼 눌러도 2~4단계 취소 안 됨~~ → ✅ 해결됨

`sendAlarm()`에 `journeyId`/`appointmentId` 파라미터 추가 및 `data`에 포함.
이제 X 버튼 누르면 `onForegroundEvent`에서 ID를 정상 읽어 `cancelRemainingStages()` 작동.

**해결 전 작성된 원인 분석 (참고용)**:

**파일**: `src/utils/notifications.ts` — `sendAlarm()` 함수

**원인**:
- `sendAlarm()`(1단계 즉시 발송)에 `journeyId`/`appointmentId`를 data에 담지 않음
- X 버튼 누를 때 `_layout.tsx` `onForegroundEvent`에서 `data.journeyId = undefined` → `cancelRemainingStages(undefined, undefined)` 호출 → 아무것도 취소 안 됨

**수정 방향**: `sendAlarm()` 파라미터에 `journeyId?`, `appointmentId?` 추가 후 `data`에 포함.

### 버그10-B — 백그라운드에서 단계별 알람 dismiss 시 나머지 단계 미취소 → ✅ 해결됨 (커밋 `9779d4a`)

**파일**: `src/utils/notifications.ts`

**원인**:
- 포그라운드에서 dismiss 버튼 누르면 `_layout.tsx` `onForegroundEvent`에서 `cancelRemainingStages()` 호출 → 정상
- 백그라운드/종료 상태에서 알림 스와이프 dismiss 시 `onForegroundEvent` 실행 안 됨
- `notifications.ts`에 `notifee.onBackgroundEvent`가 있으나 `dismiss` 처리 없음

**해결 방식**:
- `scheduleFutureAlarm()`으로 등록한 trigger notification ID들을 AsyncStorage에 `journeyId`/`appointmentId` 기준으로 저장
- `onBackgroundEvent`에서 `actionId === 'dismiss'` 시 AsyncStorage에서 해당 ID 목록 조회 → `notifee.cancelTriggerNotification()` 직접 호출
- `alarmService.stageTriggerIds`는 메모리 변수라 백그라운드에서 접근 불가 → AsyncStorage 경유로 우회

### ✅ 수정 완료 — ForegroundServiceDidNotStartInTimeException 크래시

**원인**: `alarmService.start()` 내부에서 `startBackgroundLocationUpdates()`를 백그라운드 상태에서 호출 → Android OS가 5초 내 `startForeground()` 미호출로 앱 강제 종료

**수정**: `AppState.currentState === 'active'` 체크 추가 → 포그라운드일 때만 호출
**파일**: `src/services/alarmService.ts`

### ✅ 수정 완료 — notifee Promise.all 동시 호출 SIGABRT 크래시

**원인**: `scheduleAlarmStages()`에서 `Promise.all`로 notifee 알람 등록을 동시 호출 → 네이티브 메모리(Scudo) 충돌 → `signal 6 (SIGABRT)` 크래시

**수정**: `Promise.all` → 순차 실행(`await` 직렬화)
**파일**: `src/services/alarmService.ts`, `src/tasks/backgroundLocationTask.ts`

---

## 플라스크 버그 목록 (해결됨)

### ~~플라스크 버그1 — `estimated_arrival` 계산 오류~~ → ✅ 해결됨

```python
# 수정 후
estimated_arrival = datetime.now() + timedelta(seconds=duration_sec)
```
현재 위치 기준 실제 ETA로 수정. `_compute_appointment_alarm()` 및 `_compute_alarm()` 모두 반영.

### ~~플라스크 버그2 — 막차 모드 `target_time` 의미 오류~~ → ✅ 해결됨

```python
# 수정 후
"target_time": last_arrival_dt.strftime("%Y-%m-%dT%H:%M:%S")
```
막차 출발 시각 → 막차 타고 집 도착 시각으로 수정. 스프링의 자동 ARRIVED 타이밍 정상화.

### ~~버그15 — 스위치 OFF→ON / 앱 재실행 시 DEPARTING 1단계 재발송~~ → ✅ 해결됨

- READY 상태에서 `departure_alarm_time` + `which_station` 수신 시 1~4단계 전부 절대 시각으로 OS 예약
- DEPARTING 진입 시 기존 취소 + `startIdx` 현재 시각 기준으로 해당 단계부터 재등록
- 앱 재실행/알람 수정 후 DEPARTING 직접 진입 시에도 `scheduleAlarmStages()` 호출로 정상 처리
- `backgroundLocationTask.ts`도 동일 로직 적용 (취소 후 재등록, 건너뛰기)

### ~~버그16 — 시간대(TimeZone) 미통일~~ → ✅ 확인 완료 (문제 없음)

**확인 결과**:
- **스프링**: `hibernate.jdbc.time_zone: Asia/Seoul` 설정 완료 ✅
- **MySQL**: TZ 미설정(UTC)이나 스프링 JPA가 KST로 변환해서 저장/읽기 → 실질적 문제 없음 ✅
- **플라스크**: KST 기준으로 수정 완료 ✅
- **프론트**: JS `new Date()`는 기기 시간대 자동 사용, 한국 폰 기준 KST ✅

---

## 수정 완료 목록 (2026-07-31)

| 버그 | 파일 | 상태 |
|------|------|------|
| `flask.url`이 떠난 플라스크 담당 팀원의 개인 서버 IP(`http://15.164.215.15:5000`)를 그대로 참조 → 스프링→플라스크 연동(`/location` 호출 시 위치 앵커·출발 알람 계산) 전체가 조용히 실패(트랜잭션 롤백으로 `current_lat/lng`·`departure_alarm_time` 둘 다 null로 남음) | `application.yml` | ✅ 플라스크를 조직 저장소(`CounterClockEngine2`)로 신규 이관·재배포한 뒤, 같은 EC2에 공존하는 구조에 맞춰 `flask.url`을 해당 인스턴스의 프라이빗 IP(`http://172.31.34.244:5000`)로 수정. 실제 GPS 테스트로 정상 동작 확인 |

**배경**: 기존 플라스크 담당 팀원이 오픈소스 대회에서 이탈하면서, 팀원 개인 계정 기준으로 운영되던 플라스크 배포(Docker Hub 계정, EC2 서버)를 팀 소유로 이관하는 작업을 진행. 이관 자체는 플라스크 저장소(`CounterClockEngine2`) 쪽 작업이라 별도 기록하되, 그 결과로 스프링의 `flask.url` 설정값도 함께 갱신이 필요했음.

실제 테스트에서 출발 알람 시각 정상 확인됨 — 레이어 간 시간대 불일치 없음.
