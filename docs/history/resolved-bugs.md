# GPS 폴링 버그 — 해결된 항목 아카이브

마지막 업데이트: 2026-08-09

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
| 버그30: 귀가(home) 3·4단계 알람 문구가 `isLastMode` 무관하게 "막차"로 고정 | `notifications.ts`(`buildAlarmBody`) | ✅ `HOME_LAST_TRAIN_MESSAGES`로 `isLastMode`일 때만 막차 문구 사용하도록 분기. 겸사겸사 1~3단계를 건너뛰고 4단계가 바로 발송되는("이미 늦음") 케이스 전용 문구(`LATE_STAGE4_MESSAGES`)도 신설 |

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

**참고**: 약속 삭제/추방 시 OS에 등록된 로컬 단계별 알람 취소는 이번 범위에서 제외됐다가, 이후 버그14로 별도 처리됨(아래 참고).

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

---

## 수정 완료 목록 (2026-08-01)

| 버그 | 파일 | 상태 |
|------|------|------|
| New Architecture(Fabric) 활성화 상태에서 데드라인 모드 시/분/오전오후 Picker 조작 후 완료(저장) 시 화면 언마운트 과정에서 간헐적으로 앱이 네이티브 크래시로 강제 종료(홈 화면으로 튕김) | `package.json`(`@react-native-picker/picker`) | ✅ `2.11.1` → `2.11.4`로 업데이트 |

**증상**: 귀가 알람(데드라인 모드) 등 `Picker`가 있는 화면에서 시/분/오전오후를 조작한 뒤 완료 버튼을 눌러 화면이 닫힐 때, 간헐적으로 앱이 아무 에러 메시지 없이 홈 화면으로 튕겨나감.

**원인 진단**: `adb logcat -b crash`(앱 프로세스 PID 필터가 아니라 시스템 크래시 버퍼 전체)로 네이티브 백트레이스를 확보해서 확인.
- 크래시 시그니처: `Fatal signal 6 (SIGABRT)`, `Pointer tag for 0x... was truncated` — Android 네이티브 힙 포인터 태깅 검증 실패
- 크래시 스레드: `hades` — Hermes JS 엔진의 백그라운드 동시 GC(HadesGC) 스레드
- 백트레이스 최상단: `abort → free → facebook::react::RNCAndroidDialogPickerProps::~RNCAndroidDialogPickerProps()` — Picker 화면이 언마운트되면서 Fabric Shadow Node 트리 전체가 파괴되는 과정 중, `Picker`의 Props 객체를 해제(`free`)하는 순간 포인터가 이미 손상된 상태로 죽음
- 이 트리 파괴가 Hermes GC 스레드에서 트리거됨(`libhermes.so` 프레임이 스택 최하단에 존재) — 즉 화면 unmount와 GC 타이밍이 겹치는 레이스 컨디션
- `@react-native-picker/picker`(당시 버전 `2.11.1`) + React Native `0.81.5` + `newArchEnabled: true`(New Architecture/Fabric) 조합에서 발생하는 라이브러리 자체의 알려진 메모리 버그. `2.11.3` 릴리즈 노트에 "android crash on React Native 0.81 & new arch"로 명시적으로 기재되어 있어 정확히 일치.

**수정**: `@react-native-picker/picker`를 최신 안정 버전(`2.11.4`, `2.11.3`의 수정사항 포함)으로 업데이트.

**검증**: 새 dev-client APK(EAS Build)로 재빌드 후, `adb logcat -c` + `adb logcat -b crash`로 크래시 버퍼를 비워둔 상태에서 동일 재현 절차(Picker 조작 → 완료)를 수차례 반복 — 크래시 재현 안 됨 확인.

**주의(팀 공유)**: 네이티브 모듈 버전 변경이라 `npx expo start`(JS 갱신)만으로는 반영되지 않음. 이 커밋을 받은 팀원은 각자 `npm install` 후 dev/preview 프로필로 **재빌드해서 새 APK를 재설치**해야 실제로 크래시가 사라진 걸 확인할 수 있음(기존에 설치된 APK에는 옛 버전의 네이티브 코드가 그대로 남아있음).

### ✅ 버그19 — 막차 모드, 자정~새벽4시 사이 첫 계산 시 날짜가 하루 밀림

**관련 저장소**: `gonow-flask`(`CounterClockEngine2`) 단독 수정

**파일**: `CounterClockEngine/gps_api/routes/alarm.py`(`_compute_alarm`의 `is_last_mode` 분기), `CounterClockEngine/gps_api/core/timeutil.py`(신규 `now_kst_service_day()`)

**증상**: 막차 모드 귀가 여정이 READY 상태가 되고 나서 자정~새벽4시 사이에 첫 GPS 위치가 들어와 플라스크를 처음 호출하면, 추천 막차가 실제보다 하루 밀려서 나옴. 예: 8/1 밤에 여정을 만들어 그날 밤(자정 넘긴 8/1 새벽)의 막차를 기대했는데, 8/1 00:30에 첫 계산이 이뤄지면 8/2 밤(8/2 23시~8/3 01시 창)의 막차로 계산됨.

**원인**: 막차 모드는 생성 시점에 `target_time`을 모르기 때문에 첫 계산 시 플라스크가 자기 서버 시계(`_now_kst()`, 날짜 포함)로 탐색 기준 날짜를 대체하는데, 이 값에 "자정~새벽4시는 전날 밤의 연장으로 친다"는 서비스데이 보정이 빠져 있어서 자정을 넘긴 순간 탐색 기준 날짜가 하루 밀려버림 (BUGS.md에 있던 최초 원인 분석과 동일 — 상세 배경은 git 이력 참고).

**수정**: `timeutil.py`에 `now_kst_service_day()` 신설 — 현재 KST 시각이 00:00~04:00이면 날짜를 하루 전으로 보정해서 반환. `alarm.py`의 막차 탐색 기준(`train_search_ref`)이 `target_time`이 아직 없을 때(`None`) 이 보정된 함수를 쓰도록 변경(`_now_kst()` 직접 참조 제거). 도보 폴백(700m 이내)에 쓰이는 `search_ref`는 실제 벽시계 시각이 필요해 보정하지 않고 그대로 둠.

**적용 방식 참고**: 당초 BUGS.md에는 "꼼수(스프링만)" vs "정석(스프링+플라스크 `search_anchor` 필드 신설)" 두 방향이 검토 중이었으나, 실제로는 세 번째 방식(**플라스크 단독으로 자체 시계 참조 지점만 보정**)으로 적용됨 — 스프링의 `journey.getTargetTime()` 전달 방식은 그대로 두고, 플라스크가 `target_time` 부재 시 대체하는 자기 시계 값 자체를 서비스데이 기준으로 고쳐서 스프링 쪽 수정 없이 해결.

**배포**: `main` 브랜치 push 시 GitHub Actions(`deploy.yml`)가 Docker 이미지 빌드 → Docker Hub push → EC2 SSH 접속 후 컨테이너 재기동까지 자동 수행. 커밋 `686740a`의 워크플로우 실행(2026-07-30 19:40 UTC ≈ KST 7/31 새벽 4:40)이 `success`로 완료되어 배포까지 반영 확인됨.

**검증**: 실기기로 재현 테스트 완료(사용자 확인).

---

## 수정 완료 목록 (2026-08-03)

| 항목 | 파일 | 상태 |
|---|---|---|
| 알림 채널이 첫 알람 전엔 시스템 설정에 안 보이던 문제 | `src/utils/notifications.ts`(`setupNotificationCategories`) | ✅ 앱 시작 시 채널 미리 생성하도록 수정 |
| 1~4단계 알람이 시스템 기본음만 사용(단계별 커스텀 소리 없음) | `app.json`, `src/utils/notifications.ts` | ✅ 단계별 커스텀 사운드(mp3/wav) 적용 + 소리 설정 화면 신규 |
| 배터리 최적화 제외 상태를 앱에서 확인할 방법 없음(완료 배지 표시 불가) | `modules/battery-optimization`(신규), `src/utils/permissions.ts` | ✅ 로컬 네이티브 모듈로 상태 확인 구현 |
| 단계별 출발 알람(1~4단계) 중복 발송 / "즉시 출발" 알람 재발송(구 버그18) | `src/utils/notifications.ts`, `src/services/alarmService.ts`, `src/tasks/backgroundLocationTask.ts` | ✅ `syncStagedAlarms()` 공유 함수(지문 비교+직렬화 락) 도입, whichStation null 흔들림 폴백 처리 |
| `backgroundLocationTask.ts` 미정의 함수(`removeStagingKey`) 호출로 그룹 약속 4xx 시 크래시(구 버그26) | `src/tasks/backgroundLocationTask.ts` | ✅ `cancelStagedAlarms(key)`로 교체 |

### ✅ 출발 알람 채널 사전 생성 안 됨 → 단계별 커스텀 소리/진동 설정 화면 신규 구축 (GoNow_Fronted)

**배경**: 알림 채널을 미리 만들어두는 함수(`setupNotificationCategories`)가 실제로는 빈 껍데기(no-op)로 방치돼 있어서, 사용자가 실제 알람을 한 번도 받아보기 전까지는 시스템 설정의 "알림 카테고리" 화면 자체가 텅 비어 보였음 — 커스터마이징 진입점 자체가 없는 상태였고, 소리도 전부 시스템 기본음만 재생됨.

**수정 및 신규 구현**:
- `setupNotificationCategories()`가 실제로 `ensureChannels()`를 호출하도록 수정 — 앱 최초 실행 시 채널 6개(1~4단계 + 실행 중 알림 2개)가 즉시 생성됨.
- 1~4단계 채널에 커스텀 사운드(무료 라이선스 mp3/wav, `assets/sounds/`) 적용 — `app.json`의 `expo-notifications` 플러그인 `sounds` 배열에 등록해 빌드 시 네이티브 리소스로 자동 번들링.
- Android 8.0 미만(채널 개념 자체가 없는 기기) 대응: 채널 생성 시점뿐 아니라 알림 발송 시점의 `sound` 필드도 함께 지정해 두 경로 모두 커버(개발 중 한 차례 후자를 빠뜨려서 8.0 미만 기기에서 무음이 될 뻔했던 걸 발견해 수정).
- "출발 알람 소리 설정" 화면 신규 추가(`AlarmSoundSettingsScreen.tsx`, 프로필 설정 화면에서 진입): 단계별 시스템 소리/진동 설정 화면 바로가기(`android.settings.CHANNEL_NOTIFICATION_SETTINGS` 딥링크), 기본값으로 초기화, 미리듣기 버튼.
- **채널 초기화 구현 중 발견한 안드로이드 제약**: 채널을 삭제 후 같은 ID로 재생성해도 안드로이드가 이전 사용자 설정을 그대로 복원함(un-delete) — 처음 구현한 "삭제 후 재생성" 방식이 실제로는 전혀 작동하지 않았음. 채널ID를 버전 관리 방식(`gonow-alarm-1-r1`, `-r2`...)으로 전환해 매번 새 채널을 발급하는 방식으로 재구현, 예전 채널은 생성 직후 자동 정리.

**검증**: 실기기 테스트 완료(사용자 확인) — 앱 설치 직후 채널 노출, 커스텀 소리 재생, 시스템 설정 바로가기, 초기화, 미리듣기 전부 정상 동작 확인.

**커밋**: `aa85cb9`(fix 브랜치)

### ✅ 배터리 최적화 제외 상태 확인 기능 신규 추가 (GoNow_Fronted, 이 프로젝트 최초의 커스텀 네이티브 모듈)

**배경**: "필수 권한 설정" 화면에서 알림/위치/정확한 알람 3개 항목은 실시간으로 "✅ 완료" 배지가 뜨는데, 배터리 최적화 제외만 상태 확인 API가 없어 배지 없이 "설정으로 이동" 버튼만 있었음 — 사용자 입장에서 비직관적이라는 피드백으로 시작.

**구현**: `modules/battery-optimization/`(로컬 Expo 모듈, 이 프로젝트에서 처음으로 직접 작성한 네이티브 코드) — `PowerManager.isIgnoringBatteryOptimizations()`를 코틀린으로 감싸서 JS에 노출. Android 6.0(API 23) 미만 기기 방어 코드 포함(그 이전엔 배터리 최적화 개념 자체가 없어 메서드가 존재하지 않음).

**개발 중 잡은 버그**: `requireNativeModule()`을 파일 최상단에서 즉시 호출하도록 짜서, 네이티브 모듈 로드 실패 시(재빌드 전 등) **함수 호출 시점이 아니라 import 시점**에 에러가 터져 `permissions.ts`를 쓰는 화면 전체가 죽을 수 있는 위험이 있었음 — 모듈 로드 자체를 try/catch로 감싸 안전한 폴백(`true` 반환)을 제공하도록 커밋 전에 수정.

**검증**: 재빌드 후 실기기 테스트 완료(사용자 확인) — 배터리 카드에 완료 배지 정상 표시.

**커밋**: `1184cbf`(fix 브랜치)

### ✅ 버그18 — 단계별 출발 알람(1~4단계) 중복 발송 / "즉시 출발" 알람 재발송 (GoNow_Fronted + 스프링 원인 1건 확인)

**배경**: 실기기 테스트에서 4단계("즉시 출발") 알람이 중복으로 뜨거나, 이미 다 울린 지 한참 지난 후 뜬금없이 재발하는 문제가 포그라운드/백그라운드 조합에 따라 불안정하게 재현됨. 조사 결과 4가지 원인이 얽혀 있었음.

**원인 1 — 포그라운드/백그라운드가 독립적으로 취소·재등록 반복**: `alarmService.ts`(포그라운드)와 `backgroundLocationTask.ts`(백그라운드)가 단계별 알람 스케줄링 로직을 완전히 별도로 구현하고 있었고, "이미 등록했는지" 판단용 영속 상태가 전혀 없었음(`stagingStarted`는 인스턴스 메모리 플래그라 백그라운드 전환 시 얼어붙었고, `alarmSentThisRun`은 태스크 호출마다 새로 만들어지는 지역 변수였음).

**원인 2 — 경합 조건**: `alarmService.ts`의 `poll()`이 `AppState`를 전혀 확인하지 않아 백그라운드에서도 안 죽고 있다가 포그라운드 복귀 순간 밀린 타이머가 한꺼번에 발화, `backgroundLocationTask.ts`와 거의 동시에 `/location`을 호출해 지문 비교-재등록 로직이 원자적이지 않아 둘 다 재등록을 진행하는 경우가 있었음.

**원인 3 — whichStation의 null 흔들림 (진짜 근본 원인, 스프링 코드로 직접 검증)**: `LocationUpdateResponse.java`/`ParticipantLocationUpdateResponse.java`의 `from()`이 `departureAlarmTime`은 엔티티 영속값을 항상 반환하지만, `whichStation`/`boardingTime`은 그 요청에서 플라스크를 실제로 재호출했을 때만 채우고 아니면 null을 반환함. `JourneyService.updateLocation()`의 DEPARTING "유지" 분기(300m 이내 대기 중)는 플라스크를 호출하지 않으므로, 이 상태가 지속되는 내내 `which_station: null`이 반복적으로 내려옴 — 프론트가 이 흔들림을 "진짜 변경"으로 오판해 매번 재등록, 이미 지난 4단계가 계속 재발송됨.

**수정**:
- `src/utils/notifications.ts`에 `syncStagedAlarms()` 공유 함수 신규 — AsyncStorage 기반 "스테이징 지문"(departureAlarmTime + whichStation)으로 이미 등록된 데이터인지 판단, 다르면 취소 후 재등록. `alarmService.ts`/`backgroundLocationTask.ts`의 중복 구현을 이 함수로 통합.
- `withStagingLock()`(key별 Promise 체이닝 직렬화 락)으로 원자성 보장(원인 2 보완).
- `whichStation`이 이번 폴링에 안 내려오면(null) 이전에 알던 값을 그대로 유지하도록 폴백(`whichStation ?? prev?.whichStation ?? null`) — 원인 3의 직접적인 수정.
- `alarmService.ts`의 `poll()`에 `backgroundLocationTask.ts`와 대칭되는 `AppState.currentState !== 'active'` 스킵 체크 추가(원인 2 보완).
- 스프링은 건드리지 않고 프론트에서만 해결. 다만 `whichStation`/`boardingTime`을 엔티티에 영속화하지 않는 비대칭 설계 자체는 근본적으로 스프링 쪽 개선 여지로 남아있음(다른 클라이언트가 이 API를 쓰게 되면 같은 혼란을 다시 겪을 수 있음).

**검증**: 실기기 반복 테스트(포그라운드/백그라운드 여러 조합, 장시간 백그라운드 방치 포함) 완료(사용자 확인).

### ✅ 버그26 — `backgroundLocationTask.ts`의 미정의 함수(`removeStagingKey`) 호출로 그룹 약속 4xx 시 크래시

버그18 수정 작업 중 같은 파일을 손보면서 함께 수정. `await removeStagingKey(key);`(정의되지 않은 함수, 그룹 약속 `/location` 4xx 처리 분기)를 `await cancelStagedAlarms(key);`로 교체.

---

## 수정 완료 목록 (2026-08-07~08) — 코드 수정 완료, 서버 재기동 실측 검증 필요

> 카카오맵 TRANSIT 딥링크 기능(`docs/reference/kakao-map-deeplink-spec.md` §2.3/2.4) 설계 중, ODsay 파라미터 실측 검증 과정에서 부수적으로 발견된 기존 버그 3건. 전부 `gonow-flask`(`CounterClockEngine2`) 단독 수정. 상세 실측 근거는 `kakao-map-deeplink-spec.md` §4.4~4.6, ODsay/TMAP/카카오 3사 비교 조사는 `docs/reference/transit-provider-research.md` 참고 — 여기서는 요약만 기록.

### ✅ 버그31 — `TransportMode`(`SUBWAY`/`BUS`) 선호가 잘못된 ODsay 파라미터(`SearchType`)로 전송되어 사실상 한 번도 반영 안 됐을 가능성

**파일**: `gonow-flask/CounterClockEngine2/CounterClockEngine/gps_api/core/transit_route.py`

**원인**: 지하철/버스 선택값을 `SearchPathType`이 아니라 `SearchType`(ODsay의 "도시내/도시간" 구분 파라미터)에 넣고 있었음. 실측 결과 `SearchType=1`/`2`는 ODsay가 `error -99`(검색결과 없음)로 응답 → 기존 폴백 로직(그 모드로 결과 없으면 ALL 재시도)이 매번 조용히 발동해 `SUBWAY`/`BUS` 선호가 무시된 채 항상 `ALL`처럼 동작했을 것으로 추정.

**수정**: `SearchType`은 `0` 고정, `SearchPathType`에 이동수단 값(`SEARCH_TYPE_MAP` → `SEARCH_PATH_TYPE_MAP`로 개명) 전달.

**검증**: 서버 재기동 후 실측 대기(코드 수정만 완료).

### ✅ 버그32 — 도시간(시외) 장거리 여정이 `SearchType=0` 고정으로 인해 비정상 경로(4시간+ 우회) 응답

**파일**: 동일 파일, 버그31과 함께 발견됨(버그31을 고치며 `SearchType`을 `0` 고정하면 새로 발생하는 문제)

**증상**: 서울↔대전(약 143km) 검색 시 에러 없이 "성공"으로 응답하지만, 시내버스망만으로 우회 경로를 억지로 짜맞춰 250분(4시간 15분)짜리 비정상 경로를 반환. KTX/고속버스는 검색 대상에서 아예 빠짐.

**수정**: 두 지점 간 직선거리가 70km(수도권 광역 통근권 기준)를 넘으면 `SearchType=1`(도시간)로 자동 전환 — `optimizer.py`의 `haversine` 재사용, `INTERCITY_DISTANCE_THRESHOLD_M` 상수 신설.

**검증**: 서버 재기동 후 실측 대기.

### ✅ 버그33 — `PriorityType`(`MIN_TRANSFER`/`MIN_WALK`)가 애초에 존재하지 않는 ODsay `OPT` 정렬 개념에 매핑되어 있었음

**파일**: 동일 파일 + `gps_api/routes/alarm.py`

**원인**: ODsay `OPT`은 `0`(추천경로)/`1`(타입별정렬) 두 값뿐이고 "최소환승"/"최소도보" 같은 정렬 기준 자체가 없음. 기존 코드는 `MIN_TRANSFER`→`OPT=1`, `MIN_WALK`→`OPT=2`로 보내고 있었는데, `OPT=2`는 문서에도 없는 값이라 실측상 `OPT=1`과 동일하게 동작했음(정의된 기본값 `0`으로 처리될 거라는 예상과 다름). 또한 `path = paths[0]`(ODsay "추천" 1위)을 무조건 신뢰하고 있었는데, 실측 결과 이게 진짜 최단시간조차 보장하지 않는 것으로 확인됨(동률 시 배차간격 기준으로 갈리는 것으로 추정).

**수정**: `OPT`은 `0`(추천경로) 고정으로 보내고, Flask가 ODsay 응답 후보 리스트를 직접 재정렬하도록 변경: `MIN_TIME`→`info.totalTime`, `MIN_TRANSFER`→`busTransitCount+subwayTransitCount`, `MIN_WALK`→`info.totalWalk` 각각 최솟값 선택(`PRIORITY_OPT_MAP` → `PRIORITY_RANK_MAP`로 개명, 값 자체는 원래도 정확해서 그대로 유지).

**2026-08-08 추가 — `MIN_WAIT` 신설 + 동점 시 계단식 정렬**: `PriorityType`에 `MIN_WAIT`(최소대기, `info.totalIntervalTime` 기준) 추가해 카카오맵 자체 정렬 옵션 4종과 완전히 1:1 매칭(`kakao-map-deeplink-spec.md` §2.4 표 참고). 또한 1순위 기준이 동점인 후보가 여러 개 나올 수 있다는 게 확인돼(예: 최소환승으로 설정했는데 환승 횟수가 같은 경로가 2개 이상), `rank_key`가 단일 값이 아니라 `(1순위, 2순위, 3순위, 4순위)` 튜플을 반환하도록 변경 — 유저가 고른 기준을 1순위로 두고, 2순위는 항상 `totalTime`, 나머지는 환승→도보→대기 순으로 채우는 계단식 정렬로 동점을 해소.

**검증**: 서버 재기동 후 실측 대기.

---

### ✅ 버그36 — 운영 DB `priority_type` 컬럼이 네이티브 MySQL ENUM이라 `MIN_WAIT` 추가 후 저장 시도 시 500 에러

**파일**: `src/main/java/com/timemate/gonow/domain/member/entity/MemberSetting.java`(`priorityType` 필드), 운영 MySQL `member_setting` 테이블

**증상**: `PriorityType`에 `MIN_WAIT`를 추가하고 배포까지 완료했는데(GitHub Actions 배포 성공 확인됨), 프론트에서 "최소 대기"로 설정 저장 시 "저장 실패, 다시 시도해주세요" 에러. 다른 값(`MIN_TIME` 등)은 정상 저장됨.

**원인**: 운영 DB 스키마를 직접 확인한 결과, `priority_type` 컬럼이 Hibernate에 의해 MySQL 네이티브 `ENUM('MIN_TIME', 'MIN_TRANSFER', 'MIN_WALK')` 타입으로 생성돼 있었음(VARCHAR가 아님). 운영은 `ddl-auto: update`(로컬은 `create`)라 자바 enum에 `MIN_WAIT`를 추가해도 기존 컬럼의 허용값 목록을 자동으로 안 넓혀줌 — JSON 역직렬화(자바 enum 파싱)는 성공하므로 400이 아니라 500이 뜨고, MySQL이 INSERT/UPDATE 시점에 허용 목록에 없는 값이라고 거부해서 `GlobalExceptionHandler`의 catch-all(500)로 떨어짐. 프론트는 모든 에러를 "저장 실패"로 뭉뚱그려서 원인 파악이 어려웠음.

**진단 방법**: `curl`로 운영 서버에 직접 회원가입 → 로그인 → `PATCH /api/members/me/setting`(`priority_type: MIN_WAIT`) 요청을 보내 400이 아니라 500이 뜨는 걸 확인 → JSON 파싱은 성공, DB 저장 단계 실패로 원인을 좁힘. 이후 실제 테이블 스키마를 직접 확인해 네이티브 ENUM 컬럼임을 확정.

**수정**: 운영 MySQL에 직접 SQL 실행:
```sql
ALTER TABLE member_setting
MODIFY COLUMN priority_type ENUM('MIN_TIME', 'MIN_TRANSFER', 'MIN_WALK', 'MIN_WAIT') NOT NULL DEFAULT 'MIN_TIME';
```
기존 값을 그대로 포함시켜서 기존 데이터 유실 없이 안전하게 확장.

**향후 재발 방지**: `@Enumerated(STRING)`으로 매핑된 필드에 새 enum 상수를 추가할 때마다(`PriorityType` 외에도 `TransitType`/`AppointmentStatus`/`JourneyStatus`/`ParticipantStatus` 등), 코드 배포와 별개로 운영 DB에 위와 같은 수동 `ALTER TABLE MODIFY COLUMN`이 항상 필요함 — 배포 체크리스트에 추가 검토 필요.

---

### ✅ 버그35 — 프론트 `npx tsc --noEmit` 타입 에러 7건 (`myStatus`/목적지 좌표 옵셔널 필드 미가드)

**파일(프론트, `GoNow_Fronted`)**: `HomeAlarmSheet.tsx`, `PersonalAlarmSheet.tsx`, `HomeAllAlarmSheet.tsx`, `PersonalAllAlarmSheet.tsx`, `DailyAlarmScreen.tsx`

**증상**: `npx tsc --noEmit` 실행 시 7건의 타입 에러 발생. TRANSIT 딥링크 작업(`isDriving`→`transportMode` 리팩터링) 검증 중 발견됐으나, `git diff` 대조 결과 그 리팩터링과는 무관한 기존 이슈로 확인됨.

**원인 및 수정**:
- **패턴 A (5건)**: `alarm.myStatus`가 옵셔널(`string | undefined`)인데 존재 체크 없이 `['READY', 'DEPARTING', 'MOVING', 'NEARDEST'].includes(alarm.myStatus)`에 그대로 넘겨서 발생. 5개 파일 모두 동일하게 `!!alarm.myStatus && [...].includes(alarm.myStatus)`로 존재 체크를 추가.
- **패턴 B (2건, `PersonalAlarmSheet.tsx`)**: `handleSave()`의 목적지 좌표 검증이 `if (!isEditMode && (!editAlarm.dest_lat || !editAlarm.dest_lng))`로, **수정 모드일 때만 검증을 건너뛰도록** 돼 있었음. 목적지 좌표가 필수라는 규칙은 생성/수정과 무관하게 항상 성립해야 하므로, 타입 우회가 아니라 `!isEditMode &&` 조건 자체를 제거해 항상 검증하도록 수정 — 실제로 놓치고 있던 검증 공백이었음.

**검증**: `npx tsc --noEmit` 재실행으로 7건 전부 해소 확인(무관한 기존 이슈인 버그28만 남음).

---

### ✅ 버그9 — 3초 내 중복 `active` 이벤트 시 상단바 알림(GPS 추적 중) 순간 깜빡임

**파일**: `app/_layout.tsx`(AppState `change` 리스너)

**증상**: Android에서 알림 탭·GPS 권한 다이얼로그 닫힘 등으로 `AppState`의 `active`가 짧은 간격(3초 이내)으로 연속 발화하는 경우, 상단바의 "GoNow 알람 실행 중" 알림이 순간적으로 꺼졌다 켜짐.

**원인**: `active` 핸들러가 3초 중복 판별 가드보다 **먼저** `stopBackgroundLocationUpdates()`를 무조건 호출하고 있었음. 원래 이 무조건 stop은 헤드리스(앱 완전 종료 후 FCM으로 깨어난) 상태에서 `foregroundService` 없이 시작된 위치 추적을(`backgroundAlarmTask.ts` 참고 — Android 정책상 백그라운드에서는 foregroundService 없이만 시작 가능) 포그라운드 진입 시 `foregroundService` 포함 버전으로 승격시키기 위한 목적으로 필요했음. 하지만 이 승격은 매 resume 사이클의 **최초 1회**만 의미가 있는데도, 그 직후 3초 내 중복 `active`가 오면 이미 정상 승격된 추적을 또 stop→(중복 분기에서) start로 헛돌리면서 알림이 깜빡였음.

**수정**: `lastForegroundAt` 갱신과 중복 판별을 `stopBackgroundLocationUpdates()` 호출보다 앞으로 이동. 최초(비중복) `active`에서는 기존과 동일하게 stop→`startReadyAlarms()`→(실행 중이면) start가 그대로 실행되어 헤드리스→포그라운드 승격 로직은 그대로 보존되고, 중복으로 판별된 이후 이벤트는 아무 것도 하지 않고 즉시 반환하도록 변경. 부수 효과로, `lastForegroundAt`을 첫 `await` 이전(동기 구간)에 세팅하게 되어 두 `active` 이벤트가 겹칠 때 두 번째 이벤트가 갱신 전 값을 보고 "중복 아님"으로 오판하던 레이스도 함께 제거됨.

**검증**: `npx tsc --noEmit`으로 회귀 없음 확인(무관한 기존 이슈인 버그28만 남음). 재현 조건이 까다로워(우연한 이벤트 겹침 필요) 실기기 재현 테스트 대신 코드 검토로 안전성 확인 후 반영.

---

### ✅ 버그28 — 백그라운드 위치 추적 알림에 존재하지 않는 `notificationChannelId` 옵션 사용

**파일**: `src/tasks/backgroundLocationTask.ts`(`startBackgroundLocationUpdates()`)

**증상**: `foregroundService` 옵션에 `notificationChannelId: CHANNEL_SILENT`를 지정해서 상단바 "GoNow 알람 실행 중" 알림을 무음 채널로 보내려 했으나, 설치된 `expo-location` 타입 정의(`LocationTaskServiceOptions`)엔 이 필드 자체가 없어 `npx tsc --noEmit`에서 컴파일 에러(`TS2353`)로 잡히던 상태였음.

**조사**: `node_modules`에 포함된 안드로이드 네이티브 구현(`expo-location/android/.../LocationTaskService.kt`)을 직접 확인. `startForeground()`가 읽는 옵션은 `notificationTitle`/`notificationBody`/`notificationColor` 3개뿐이고, 채널ID는 `appId + ":" + taskName`으로 **네이티브 내부에서 고정**되어 앱이 지정할 방법 자체가 없음(즉 `notificationChannelId`는 애초에 전달될 경로가 없는 죽은 옵션). 그리고 이 고정 채널을 생성하는 `prepareChannel()`이 `NotificationManager.IMPORTANCE_LOW`로 만들고 있어서, 소리·진동 없이 알림창에만 조용히 뜨는 것이 이미 보장돼 있었음 — 원래 의도(무음 상단바 알림)는 이 옵션과 무관하게 이미 100% 달성된 상태였음.

**수정**: `notificationChannelId: CHANNEL_SILENT` 줄과 더 이상 쓰이지 않는 `CHANNEL_SILENT` import 제거. 실제 동작(무음 여부)에는 아무 변화 없음 — 죽은 코드와 타입 에러만 제거.

**검증**: 네이티브 소스로 무음 여부를 확정할 수 있어 실기기 테스트 없이 반영. `npx tsc --noEmit` 재실행으로 에러 0건 확인(버그9/버그35 수정 이후 마지막으로 남아있던 이 에러까지 전부 해소).

---

### ✅ 버그1 — FCM Data 포그라운드 수신 시 `backgroundAlarmTask`와 타이밍 gap [코드 변경 없음, 문서만 정정]

**결론**: 코드 수정 없음. 기존 BUGS.md 서술을 코드로 재검증하다가 **문서 자체가 낡아 있음**을 발견해서 바로잡음.

**원래 서술의 문제**: "`fcmSub`의 `alarmService.start()` → `handOffFromBackground()`가 AsyncStorage ID를 지워서 정리된다"고 적혀 있었는데, `handOffFromBackground()`라는 함수는 현재 코드 전체(`GoNow_Fronted`)에 존재하지 않음(전체 검색 0건). 과거 리팩터링 어느 시점에 이름이 바뀌었거나 구조가 달라졌는데 문서만 안 따라간 것으로 보임.

**실제 코드로 재확인한 현재 동작**: `backgroundAlarmTask.ts`(`BACKGROUND_ALARM_TASK`)와 `_layout.tsx`의 `fcmSub` 둘 다 각자 `AppState.currentState === 'active'`를 체크해서 서로 겹치지 않게 분업한다. `AppState`가 실제로 `active`로 갱신되는 시점엔 OS 스케줄링 특성상 미세한 지연이 있어, 아주 드물게 두 경로가 동시에 같은 FCM Data를 처리할 여지가 있음 — 다만 이 경우도 `_layout.tsx`의 `active` 핸들러가 진입 시 무조건 호출하는 `stopBackgroundLocationUpdates()`([[버그9]] 수정 대상이기도 했던 바로 그 호출)가 헤드리스 쪽에서 시작됐을 수 있는 위치 추적을 곧바로 정지시켜서 정리된다. 즉 정리 메커니즘 자체는 실재하되, 이름과 위치가 문서와 다를 뿐.

**남은 특성(고칠 수 없음)**: `AppState` 갱신 타이밍은 OS 내부 스케줄링이라 앱 코드로 100% 결정론적으로 통제할 수 없음 — 짧은 틈 자체를 원천 차단할 방법은 없고, 발생해도 위 정리 로직으로 무해하게 수습됨.

**수정**: BUGS.md에서 낡은 서술과 함께 항목 제거(더 이상 추적할 미해결 항목이 아님).

---

### ✅ 버그2 — 앱 완전 종료 후 재실행 시 GPS 폴링 복구(로그인 전 공백) [코드 변경 없음, 이미 해결된 상태였음을 재확인]

**결론**: 코드 수정 없음. 원래 우려했던 문제는 이전 세션에서 이미 해결돼 있었고, 이번엔 그 사실과 "남아있는 항목이 왜 더 이상 손댈 게 없는지"를 재확인만 함.

**원래 우려했던 문제**: 앱을 완전히 종료했다가 재실행하면, 로그인해도 GPS 폴링(알람의 핵심 동작)이 다시 시작되지 않는 게 아닌가 하는 우려.

**이미 적용된 해결책**: `LoginScreen.tsx`의 `handleLogin()`이 로그인 성공 직후(토큰 저장 다음) `getAlarms()`로 오늘 알람을 조회해서, `READY`/`DEPARTING`/`MOVING`/`NEARDEST` 상태이고 `isActive`인 알람을 전부 `alarmService.start()`로 즉시 재시작한다(각 알람마다 `isRunning()` 중복 방지 체크 포함). 코드로 직접 재확인 완료.

**더 이상 손댈 게 없는 이유**: 남아있던 "로그인 화면 진입 ~ 로그인 버튼 누르기 전" 구간의 폴링 공백은, 인증 토큰이 없으면 인증이 필요한 API를 애초에 호출할 수 없다는 로그인 시스템의 근본 전제 때문이라 코드로 없앨 방법이 없음 — 버그가 아니라 로그인 구조 자체의 당연한 특성.

**수정**: BUGS.md에서 항목 제거(더 이상 추적할 미해결 항목이 아님).

---

### ✅ 버그14 — 추방/방 삭제 시 OS 등록 단계별 알람 미취소

**파일**: `app/_layout.tsx`(fcmSub `appointment_deleted`/`removed_from_appointment` 분기), `src/tasks/backgroundAlarmTask.ts`

**증상**: 방장이 참가자를 추방하거나 약속 자체를 삭제하면 피해 참가자에게 FCM Data(`sync_event: appointment_deleted` 또는 `removed_from_appointment`)가 이미 전송되고 있었지만, 프론트가 이 이벤트를 받아도 열려있는 상세화면을 닫아주기만 할 뿐, 그 전에 OS(notifee)에 미리 예약해둔 2~4단계 출발 알람은 취소하지 않았음 — 이미 추방/삭제된 약속인데도 예정 시각이 되면 단계별 알람이 그대로 울림.

**원인 분석 중 발견한 사실**: 이 FCM Data는 앱이 포그라운드일 땐 `_layout.tsx`의 `fcmSub`(`Notifications.addNotificationReceivedListener`)가 받고, 포그라운드가 아니면(백그라운드로 전환만 됐어도, 완전 종료여도 상관없이) `backgroundAlarmTask.ts`의 헤드리스 태스크가 대신 받는 구조다. 그런데 `backgroundAlarmTask.ts`는 애초에 `sync_event` 필드 자체를 전혀 들여다보지 않고 있어서, 이 경로로 온 삭제/추방 이벤트는 완전히 무시되고 있었다 — "앱을 완전히 꺼놨을 때"뿐 아니라 "그냥 백그라운드에 둔 채(강제 종료 아님)"에도 해당되는, 원래 BUGS.md의 "영향 낮음" 표기보다 실제로는 더 자주 걸릴 수 있는 경로였음.

**수정**:
- `_layout.tsx`의 두 FCM 핸들러에 `alarmService.stop(undefined, appointmentId)` 추가. `AlarmManager.stop()`이 내부적으로 `runner.stop() → cancelRemainingStages() → cancelStagedAlarms(key)`를 이미 연쇄 호출하는 구조를 그대로 활용(별도 취소 로직 신규 작성 불필요).
- `backgroundAlarmTask.ts`에 `sync_event === 'appointment_deleted' | 'removed_from_appointment'` 분기를 새로 추가. 헤드리스 환경이라 `alarmService`의 in-memory runner가 없으므로, `cancelStagedAlarms(`a_${appointmentId}`)`를 직접 호출하고 `ACTIVE_APPOINTMENTS_KEY`에서도 해당 ID를 즉시 제거(제거 안 해도 다음 `/location` 폴링이 서버 404를 받으면 `backgroundLocationTask.ts`의 기존 4xx 처리 로직이 결국 같은 정리를 하긴 하지만, 그 사이 폴링 간격(최대 5분)만큼 예약 알람이 늦게 취소될 수 있어 즉시 처리로 그 지연을 없앰).

**검증**: `npx tsc --noEmit` 통과. 재현하려면 실제로 그룹 약속을 만들고 추방/삭제 후 예약된 단계별 알람이 안 울리는지 실기기 확인이 필요하나, 기존에 검증된 `cancelStagedAlarms`/`AlarmManager.stop()` 경로를 그대로 재사용하는 구조라 코드 검토로 안전성 확인 후 반영.

---

### ✅ 버그37 — 방장이 그룹 약속을 수정해도 참가자 기기의 알람이 즉시 갱신되지 않고 다음 폴링까지 지연됨

**파일**: `app/_layout.tsx`(fcmSub `participant_status` 분기), `src/services/alarmService.ts`(`AlarmManager.start()`)

**증상**: 방장이 그룹 약속의 목표 시각(또는 목적지/날짜)을 수정하면, 방장 본인 기기는 OS에 예약된 단계별 알람이 즉시 새 시각으로 재등록되는데, 참가자 기기는 즉시 반영되지 않고 실기기 로그상 최대 인터벌(관찰된 값: 300초)만큼 지연된 뒤에야 재등록됨.

**원인**: 방장은 `GroupAlarmSheet.tsx`의 `handleSave()`가 수정 저장 직후 조건 없이 `alarmService.start(...)`를 호출해 즉시 강제 재시작되는 반면, 참가자는 `app/_layout.tsx`의 "방장 알람 수정 시 참가자 상태 동기화 FCM" 핸들러가 `participant_status === 'READY'`일 때 `if (alarmService.isRunning(undefined, appointmentId)) return;`으로 **이미 폴링 중이면 무조건 스킵**하고 있었음. 참가자는 참여 이후 알람이 항상 이미 실행 중이라 이 스킵 분기에 거의 매번 걸려, FCM으로는 아무 것도 갱신되지 않고 자기 자신의 다음 정기 폴링(서버가 마지막으로 내려준 `interval`만큼) 때가 되어서야 뒤늦게 새 `departureAlarmTime`을 받아 재등록됐음.

**수정**:
- `_layout.tsx`의 `isRunning()` 스킵 가드 제거 — 방장과 동일하게 참가자도 FCM 수신 즉시 `alarmService.start(...)`로 강제 재시작.
- 이 과정에서 새로 발견한 부수 버그도 함께 수정: `AlarmManager.start()`가 이미 실행 중인 runner를 새 runner로 교체할 때 `isActive`(참가자 개인 알람 스위치)를 항상 `true`로 기본 초기화하고 있어서, 알람 스위치를 꺼둔 참가자가 방장 수정 FCM을 받을 때마다 스위치가 도로 켜지는 회귀가 생길 뻔했음(`getAppointment()` 응답에 참가자별 `is_active`가 아예 없어 값을 넘겨받을 방법도 없었음). `AlarmManager.start()`에서 기존 runner가 있고 호출부가 `isActive`를 명시하지 않았으면 기존 runner의 `isActive`를 그대로 이어받도록 수정 — 이 보호는 `start()`를 호출하는 모든 곳(참가자 이동수단 변경 등)에 공통 적용됨.

**검증**: `npx tsc --noEmit` 통과. 실기기로 방장이 시각 수정 → 참가자 기기 로그에서 다음 폴링을 기다리지 않고 즉시 `[trigger] 취소 시도` → 새 시각 재등록 로그가 뜨는지 확인 필요.

---

### ✅ 버그38 — 방장이 자기 이동수단만 바꿔도 참가자 전원의 알람 정보가 불필요하게 리셋됨

**파일**: `AppointmentService.java`(`updateAppointment()`)

**증상**: 그룹 약속 수정 API(`PATCH /api/appointments/{id}`)는 목적지/날짜/시간/방장 이동수단을 필드 구분 없이 항상 하나의 요청으로 받는데(프론트가 매번 6개 필드를 전부 담아 보냄), 서버가 이 중 **무엇이 바뀌었는지 구분하지 않고 항상** 참가자 전원의 상태·`departureAlarmTime`·`currentPos`를 리셋하고 FCM을 발송했음. 참가자별 이동수단은 목적지/날짜/시간과 달리 각자 독립적으로 계산되어 다른 참가자 경로에 영향이 없으므로, 방장이 자기 이동수단만 바꿨을 때도 참가자 전원이 리셋당하는 건 불필요한 낭비였음(참가자 전원 대상 FCM 발송, 서버의 플라스크 재계산 유발, [[버그37]] 수정 이후엔 참가자 기기의 불필요한 강제 재시작까지 연쇄됨).

**원인**: 요청으로 들어온 값과 기존 저장값이 실제로 다른지 비교하는 로직 자체가 없었음 — 요청이 오면 무조건 `bulkResetStatusByAppointmentId` + `bulkResetAlarmInfoByAppointmentId` + FCM 발송을 실행.

**수정**: `isScheduleChanged()` 헬퍼를 추가해 날짜/시간/목적지(이름·주소·위도·경도)가 실제로 바뀌었는지 먼저 비교. 위도/경도는 `BigDecimal`이라 DB에서 다시 읽은 값과 scale이 달라질 수 있어(예: 저장 시 scale 8, 요청 파싱 시 다른 scale) `.equals()` 대신 `.compareTo() == 0`으로 수치만 비교. 하나라도 바뀌었으면 기존과 동일하게 참가자 전원 리셋 + FCM 발송, 방장 이동수단만 바뀐 경우엔 방장 본인 참가자 레코드만 `updateStatus`/`updateCurrentPos(null)`/`updateAlarmInfo(null, null)`로 리셋.

**검증**: `./gradlew compileJava` 통과.

---

### ✅ 버그39 — 참가자가 자기 이동수단을 바꿔도 본인 알람이 재계산되지 않음

**파일**: `ParticipantService.java`(`updateTransportType()`), `GroupAlarmSheet.tsx`/`GroupAllAlarmSheet.tsx`(`handleSave()` 참가자 분기)

**증상**: 일반 참가자가 자기 이동수단을 바꾸는 API(`PATCH /api/appointments/{id}/participants/transport`)는 `participant.transportType`만 갱신하고 본인 제외 나머지 참가자에게 UI 새로고침용 FCM(`participants_changed`)을 보낼 뿐, **본인의 `departureAlarmTime`/`currentPos`는 전혀 건드리지 않았음** — 다음 GPS 폴링에서도 "앵커 있음(최근 계산됨)"으로 판단돼 재계산 자체가 안 일어나, 이동수단을 바꿔도 예전 이동수단 기준 알람이 그대로 유지됐음.

**원인**: [[버그38]]과 같은 계열의 근본 원인(앵커 리셋 누락)이 다른 메서드에 별도로 존재했던 것 — `AppointmentService.updateAppointment()`(방장 전용)는 앵커 리셋 로직이 있었지만, `ParticipantService.updateTransportType()`(참가자 전용)에는 애초에 없었음.

**수정**:
- 스프링: 이동수단이 실제로 바뀐 경우에만(`participant.getTransportType() != request.transportType()`) `updateCurrentPos(null)` + `updateAlarmInfo(null, null)`로 본인 앵커 리셋.
- 프론트: `GroupAlarmSheet.tsx`/`GroupAllAlarmSheet.tsx`의 참가자 이동수단 변경 저장 성공 시, 방장의 `handleSave()`와 동일하게 `alarmService.start(...)`를 호출해 저장 즉시 로컬에서 강제 재폴링·재등록되도록 함(이 호출도 [[버그37]]에서 고친 `isActive` 보존 로직의 보호를 자동으로 받음).

**검증**: `./gradlew compileJava` + `npx tsc --noEmit` 둘 다 통과.

---

### ✅ 버그24 — 플라스크 4xx/5xx 응답이 구분 없이 500으로 처리됨

**파일**: `GlobalExceptionHandler.java` (스프링), `app.py` (플라스크, `gonow-flask`)

**증상**: 플라스크가 정상적으로 응답했지만 계산을 거부하는 경우(막차 없는 지역 404, 라우팅 API 실패 502
등), 스프링에 `RestClientResponseException`(`HttpClientErrorException`/`HttpServerErrorException`)을
잡는 핸들러가 없어 catch-all `Exception → 500`으로 처리됨. 프론트 입장에서 "계산 불가"와 "서버 진짜
고장" 둘 다 구분 안 되는 500으로 보였음.

**원인**: `RestClientConfig`가 만드는 기본 `RestClient`는 4xx/5xx 응답을 예외로 던지는데,
`GlobalExceptionHandler`가 연결 실패(`ResourceAccessException`)만 503으로 별도 처리하고 "연결은 됐지만
플라스크가 에러 상태코드로 응답한 경우"는 전혀 구분하지 않았음. 조사 중 플라스크 쪽도 한 가지 더 확인됨 —
`app.py`가 400/404/500에만 `@app.errorhandler`(JSON 응답)를 등록해뒀는데, `alarm.py`/`personal.py`가
실제로 쓰는 `abort(502, ...)`(라우팅 API 실패)에는 매칭되는 핸들러가 없어서 502는 HTML 에러 페이지로
나가고 있었음 — 의도적 설계가 아니라 400/404/500만 처리하다가 나중에 502 사용처가 추가될 때 핸들러
등록을 빠뜨린 것으로 보임(주석도 없고, 다른 코드가 이 HTML 응답에 의존하는 곳도 없어서 안전하게 보강).

**수정**:
- 스프링: `RestClientResponseException` 핸들러 추가 → 400 + `ApiResult.fail("경로를 계산할 수 없습니다. 잠시 후 다시 시도해주세요.")`. 플라스크 응답 본문은 상태코드마다 형식이 달라(JSON/HTML 혼재) 파싱하지 않고 고정 문구만 반환, 실제 상태코드/본문은 `log.warn`으로 서버 로그에만 기록.
- 플라스크: `app.py`에 `@app.errorhandler(502)` 추가 → 기존 400/404 핸들러와 동일한 패턴(`jsonify({"error": str(e)}), 502`)으로 통일. 스프링 쪽 수정이 응답 본문을 안 읽기로 했기 때문에 이 자체는 이번 수정의 필수 전제조건은 아니었지만, 발견한 김에 함께 정리함.

**검증**: `./gradlew compileJava` 통과.
