# GPS 폴링 버그 — 해결된 항목 아카이브

마지막 업데이트: 2026-08-20

2026-08-14 프론트 GPS 동적 폴링/지오펜스 안정화(경쟁조건, 콜드스타트 러너 복구, EXIT 지오펜스 OS 한계 등) 상세는 별도 문서 참고: [geofence-polling-stabilization-2026-08-14.md](geofence-polling-stabilization-2026-08-14.md)
2026-08-16 위 안정화 작업이 버그3/버그8(백그라운드 GPS interval 30초 고정 + 포그라운드 중복 호출)의 최종 해결이었음을 확인, `BUGS.md`에서 해결됨으로 이동

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

---

## 수정 완료 목록 (2026-08-10)

### ✅ 버그3/버그8 — 백그라운드 GPS polling interval 30초 고정 + 포그라운드 중복 호출 (GoNow_Fronted, 2026-08-14 재구현·검증 완료 — 아래 "최종 해결" 문단 참고)

**배경**: 지오펜싱 도입을 검토하던 중, "OS 레벨 GPS 구독의 `timeInterval`이 30초로 하드코딩돼 있어 서버가 알려준 실제 필요 주기(최대 300초)를 무시하고 항상 30초마다 GPS 하드웨어를 깨운다"(버그3)와 "포그라운드에서도 이 구독이 그대로 살아있어 `alarmService`의 정밀 폴링과 중복 발화한다"(버그8)는 두 문제를 라이브러리 교체 없이 기존 `expo-location` 안에서 해결하기로 결정. `react-native-background-geolocation` 등 대체 라이브러리는 유료 라이선스 + New Architecture 호환성 미검증 + 지오펜싱 자체는 `expo-location`에 이미 무료 내장(`startGeofencingAsync`)돼 있어 불필요하다고 판단해 채택하지 않음.

**수정 범위**: 서버가 마지막으로 알려준 interval(활성 journey/appointment 중 최솟값)을 OS 레벨 구독 등록 시 사용하도록 변경. "포그라운드 진입 시 Low accuracy로 낮추고 백그라운드 전환 시 재등록"하는 더 적극적인 버전(중복 호출 완전 제거)은, 그 재등록이 안드로이드의 "백그라운드에서 포그라운드서비스 시작 금지" 정책과 충돌할 위험이 있어 실기기 검증 없이는 위험하다고 판단해 이번 범위에서 제외.

**구현**:
1. `src/tasks/backgroundLocationTask.ts`에 `getMinDesiredIntervalMs()` 헬퍼 추가 — `ACTIVE_JOURNEYS_KEY`/`ACTIVE_APPOINTMENTS_KEY`(활성 목록)로 조회 범위를 제한해 `DESIRED_INTERVALS_KEY`에서 최솟값을 계산(값이 없으면 기존과 동일하게 30초 폴백). `startBackgroundLocationUpdates()`의 하드코딩된 `timeInterval: 30000`을 이 함수 호출로 교체, 로그에도 실제 등록값을 찍도록 보강.
2. `src/tasks/backgroundAlarmTask.ts`(새벽 4시 FCM 헤드리스 경로)의 `Location.startLocationUpdatesAsync()` 직접 호출부도 동일한 헬퍼로 교체.
3. **추가 발견한 갭**: 위 등록은 "포그라운드 진입 시점"에만 일어나는데, 그 등록 이후 서버로부터 새 interval을 받아도 다음 포그라운드 재진입 전까지는 반영이 안 되는 문제가 실기기 테스트로 드러남(앱을 한 번 열고 바로 백그라운드로 보내는 흔한 패턴에서 크게 작용). `src/services/alarmService.ts`에 `applyNewInterval()`을 추가해, `pollPersonal`/`pollGroup`이 새 interval을 받을 때(이전 값과 실제로 다를 때만) 저장소 반영과 동시에 `stopBackgroundLocationUpdates()` → `startBackgroundLocationUpdates()`를 즉시 재호출하도록 함 — 이 시점은 `poll()`이 이미 포그라운드임을 확인한 뒤라 안드로이드 정책과 무관하게 안전.

**검증**: 실기기(adb logcat)로 반복 테스트.
- 포그라운드→백그라운드 전환 시 등록 interval이 서버값(예: 300초)과 일치 확인(5분 1초 간격으로 태스크 발화 — 오차 1초 이내).
- interval 변경 즉시(백그라운드 진입 전) 재등록되어, 최초 등록 지연 없이 바로 새 값으로 도는 것 확인.
- `npx tsc --noEmit` 클린 통과.

**1차 롤백(같은 날, 버그42로 기록)**: `backgroundAlarmTask.ts`(완전 종료 상태 + 새벽 4시 FCM 헤드리스 웨이크업) 경로에서 새 코드(`getMinDesiredIntervalMs()`)가 실제로 도는 것 자체를 실기기로 재현하지 못함 — 원인 조사 결과 코드 문제가 아니라 테스트 환경(`adb shell am force-stop`이 FCM 리시버를 비활성화시킴, dev client의 Metro 재연결 지연 가능성, 기기 배터리 최적화 가능성) 쪽으로 추정됨. 이후 지오펜싱 도입(`docs/history/geofencing-migration-plan.md`)을 결정하면서 이 파일이 새벽 4시에 하는 일 자체가 "GPS 폴링 시작"에서 "위치 1회 확인 + 지오펜스 등록"으로 바뀔 예정이라 이 수정이 무의미해질 것으로 판단, `backgroundAlarmTask.ts`의 해당 변경만 먼저 롤백함(이 시점엔 `backgroundLocationTask.ts`/`alarmService.ts`는 아직 유지).

**2차 롤백(2026-08-11, 전면 롤백)**: 위 1차 롤백 논의 중 재검토하다가, 남겨뒀던 `backgroundLocationTask.ts`/`alarmService.ts` 쪽도 문제가 있다는 걸 추가로 발견함 — 기존 30초 고정 덕분에 백그라운드에서도 항상 자주(30초) 확인되던 것이, 버그29(`departureAlarmTime` 임박 반영 못 함) 증상을 의도치 않게 완화해주고 있었음. 이걸 "서버가 알려준 실제 값(최대 300초)"으로 바꾸면 배터리는 아끼지만 백그라운드에서 상태 전환 감지가 최대 300초까지 늦어질 수 있어 버그29의 영향 범위가 오히려 넓어짐 — 배터리 효율보다 반응 정확도를 우선하기로 하고, `backgroundLocationTask.ts`/`alarmService.ts`의 나머지 수정도 전부 롤백(`git restore`, 두 파일 모두 원래 상태로 완전 복원). 버그3/버그8은 다시 `BUGS.md`의 미해결 목록으로 복귀 — 지오펜싱 도입 시 "MOVING 상태 전용"으로 범위를 좁혀서 재구현할 예정(MOVING은 interval이 원래 짧아 위 트레이드오프의 심각도가 낮음). 검증까지 마쳤던 코드(`getMinDesiredIntervalMs()`/`applyNewInterval()` 설계)는 재구현 시 그대로 재사용 가능하므로 이 문서에 남겨둔 구현 내용은 유효한 참고 자료로 유지.

**최종 해결(2026-08-14)**: NEARDEST가 순수 지오펜싱으로 전환되고(2026-08-13) FGS(포그라운드 서비스)와 GPS 폴링을 완전히 분리하는 독립 네이티브 모듈(`modules/foreground-service`)이 도입되면서, 2차 롤백의 롤백 사유 2번("백그라운드 구독 stop→restart가 FGS 재시작을 걸어 크래시 위험")이 구조적으로 사라졌다. 이를 근거로 "MOVING 전용"이 아니라 READY/DEPARTING/MOVING 전체 범위로 재구현·전면 재검증했다([[geofence-polling-stabilization-2026-08-14]] 참고, 커밋 `65e90f6`).

- **버그8(포그라운드 중복)**: `maybeSyncGpsPolling()`이 `AppState.currentState === 'active'`면 무조건 네이티브 GPS 구독을 끄고(`stopGpsPolling()`) 포그라운드 정밀 타이머(`AlarmRunner`)만 GPS를 쓰게 만드는 구조적 수정 — interval 값과 무관한 순수 구조적 보장이라, 3대 불변식 실기기 테스트 전 범위(fg/bg 반복 전환, NEARDEST 경쟁, 콜드스타트 등)로 폭넓게 검증됨. **완전 해결.**
- **버그3(30초 고정)**: `getMinDesiredIntervalMs()`가 활성 journey/appointment의 서버 지시 interval 중 최솟값을 동적으로 계산해 네이티브 구독에 반영하고, 값이 바뀌면 재시작(stop→restart)한다. 로직 자체는 특정 값에 종속되지 않는 범용 코드(`Math.min` + 값 변경 시 재시작)이고, "값이 바뀌면 재시작"되는 경로도 이번 재검증 중 실제로 실행됨(기본 폴백값 30s → 테스트 강제값 15s 전환 시). 다만 이번 재검증 내내 `DEBUG_FORCE_INTERVAL_SEC = 15`(테스트용 상수, 의도적으로 유지 중)가 서버의 실제 interval 값을 항상 15초로 덮어썼기 때문에, 300초 같은 실제 서버 값 규모에서의 최종 실측 확인은 아직 이뤄지지 않았다. 코드 경로가 일반적이라 문제 소지는 낮다고 판단해 해결로 분류하되, 이 상수를 `null`로 되돌릴 때 한 번 더 실측 확인하는 걸 권장.

---

## 수정 완료 목록 (2026-08-11)

### ✅ 버그40 — 자가용(DRIVING) `departureAlarmTime` 계산이 계산 시점 실시간 정보만 반영하던 문제 → 카카오모빌리티 future API 2-pass 도입

**관련 저장소**: `gonow-flask`(`CounterClockEngine2`)

**파일**: `gps_api/core/kakao_route.py`(`fetch_route_future()` 신규 + `_call_directions_api()` 공통 헬퍼로 realtime/future 파싱 로직 통합), `gps_api/routes/personal.py`(`_get_driving_duration()` 신규), `gps_api/routes/alarm.py`(개인/그룹 두 DRIVING 분기 모두 `_get_duration()` 대신 `_get_driving_duration()` 사용하도록 교체), `gps_api/app.py`(부수적으로 발견한 로깅 설정 누락 별도 수정)

**배경**: 실측 검증(아래 "도입 전 사전 실측 검증" 참고, 6개 시나리오, 리드타임 0.3h~44.5h) 결과 절대 오차 평균 ~6.6%, 최악 -13.8%, 리드타임과 오차 크기 사이 뚜렷한 상관관계 없음, 6개 중 5개가 과소평가 방향으로 확인됨 → 2-pass + 안전마진 도입 결정.

**구현**:
- 1차로 realtime API(`/v1/directions`)를 호출해 대략적인 출발 시각(`target_time - 소요시간`)을 추정.
- 그 시각까지 남은 시간(리드타임)이 1시간 이상(`_FUTURE_API_MIN_LEAD_SEC`)이면 2차로 future API(`/v1/future/directions`)를 그 시각 기준으로 재호출해 정확도를 높임. 1시간 미만이면 realtime이 이미 충분히 정확하므로(실측 STEP0/1, 오차 +1.5~2.5%) 1차 결과를 조용히 그대로 사용(로그도 안 남김 — 근접 리드타임마다 매번 로그를 남길 필요가 없다고 판단).
- future API 예측이 과소평가되는 경향(실측 6개 중 5개)을 보정하기 위해 안전마진 10%(`_DRIVING_FUTURE_SAFETY_MARGIN`)를 소요시간에 곱해서 적용. future API 호출이 실패하면 realtime 값에 동일 마진을 적용해 폴백.

**구현 중 발견해서 고친 설계 실수**: 최초 구현에서 future API에 넘길 "출발 예정 시각"을 계산할 때 준비시간+지각버퍼(`total_buffer_min`)까지 함께 빼서, 실제로는 "알람이 울리는 시각"을 넘기고 있었음. future API가 알아야 하는 건 실제로 운전이 시작되는 순간의 교통상황이므로, 그보다 먼저 울리는 알람 시각을 기준으로 물어보면 준비시간만큼 이른 교통상황을 잘못 조회하게 됨(러시아워 경계에 걸리면 오차가 커질 수 있는 지점). 코드 셀프 리뷰 단계에서 발견해 `rough_departure = target_time - duration_sec_1`(버퍼 미포함)로 수정, 더 이상 쓰이지 않게 된 `total_buffer_min` 파라미터도 함수 시그니처에서 제거.

**부수 발견 — 프로덕션 로깅 설정 누락**: gunicorn으로 기동하는 프로덕션 환경(디버그 모드 아님)에서는 Flask `app.logger`의 유효 레벨이 기본 WARNING이라, `.info()` 로그가 핸들러 유무와 무관하게 조용히 버려짐 — 첫 배포 후 EC2에서 `docker logs`로 확인했을 때 2-pass 로그가 전혀 안 찍혀서 발견함. `create_app()`에 `logging.basicConfig(level=logging.INFO, ...)` + `app.logger.setLevel(logging.INFO)`를 추가해 별도 커밋으로 수정.

**검증**: EC2 실제 배포 후 `docker logs -f myapp`로 확인. 리드타임 5.5h/3.5h/2.4h 세 케이스 모두 1차(realtime)≠2차(future) 값이 실제로 다르게 나오는 것과, 최종값이 2차값의 정확히 1.10배(마진 적용)인 것을 확인:
```
[DRIVING 2-pass] lead=5.5h 1차(realtime)=623s 2차(future)=801s 최종(마진 10%)=881s
[DRIVING 2-pass] lead=3.5h 1차(realtime)=625s 2차(future)=970s 최종(마진 10%)=1067s
[DRIVING 2-pass] lead=2.4h 1차(realtime)=625s 2차(future)=734s 최종(마진 10%)=807s
```
근접 리드타임(1시간 미만) 케이스에서 로그가 안 찍히는 것도 설계대로임을 확인(1-pass 조용히 처리).

**커밋**: `653628e`(2-pass 로직), `cd82fa4`(로깅 설정 수정) — `gonow-flask` `main`

**도입 전 사전 실측 검증 (2026-08-08~11, 원래 `docs/status/future-api-validation-status.md`에 기록했다가 이 항목으로 통합 후 그 파일은 삭제됨)**: 2-pass 도입 여부를 결정하기 위해, 코드 작성 전에 카카오모빌리티 API를 직접 호출해 future API의 예측 정확도부터 검증했다. 지원 스크립트(`record.sh`/`verify.sh`/`future_api_validation.py`)는 자동 체크 체인이 끊겨 무효 재현이 반복되면서 폐기하고, `future_api.http`(IntelliJ HTTP Client, 검증 완료 후 API 키 노출 우려로 삭제됨)로 수동 검증 전환.

STEP0 사전 점검(근접 리드타임 3분 후): 예측 1313초 vs 실제 1281초, 오차 +2.5% — future API 자체의 기본 신뢰성 확인.

판교↔강남(중거리)/신논현↔강남(초단거리)/수원↔강남(장거리)/삼성↔강남(단거리) 조합으로 리드타임 0.3h~44.5h 6개 시나리오:

| # | 시나리오 | 경로 | 리드타임 | 예측(초) | 실제(초) | 오차 |
|---|---|---|---|---|---|---|
| 1 | 대조군(근접) | 판교↔강남 | 0.3h | 1284 | 1265 | +1.5% |
| 2 | 퇴근러시 | 판교↔강남 | 20.5h | 1210 | 1325 | -8.7% |
| 3 | 출근러시+초단거리 | 신논현↔강남 | 22.6h | 265 | 265 | 0% |
| 4 | 한산한낮+장거리 | 수원↔강남 | 21.8h | 2465 | 2723 | -9.5% |
| 5 | 심야+단거리 | 삼성↔강남 | 18.3h | 568 | 659 | -13.8%(최악) |
| 6 | 극단(최대 리드타임) | 수원↔강남 | 44.5h | 2463 | 2629 | -6.3% |

**검증 중 겪은 시행착오(참고용)**: STEP3/4/5는 1차 시도에서 자동 체크 체인이 끊겨 대조(realtime) 호출이 목표 시각보다 1.5~8시간 늦게 실행되는 바람에 무효 처리되어 재측정함. STEP5는 2차 시도마저 대조 호출(realtime) 자체를 깜빡해서 05:44분 뒤에야 인지 — 그 시점엔 재실행해도 의미가 없다고 판단해 다음 도래 시각으로 목표를 다시 잡고 재측정.

**결론**: 절대 오차 평균 ~6.6%, 최악 -13.8%. 리드타임과 오차 크기는 뚜렷한 상관관계 없음(리드타임이 가장 긴 STEP6이 오히려 20시간대 시나리오들보다 오차가 작았음) — 오차는 리드타임보다 개별 경로/시간대 특성에 더 좌우되는 것으로 보임. 6개 중 5개가 과소평가(예측 < 실제) 방향이라 GoNow 용도(출발 알람)에서는 "알람이 너무 늦게 울리는" 방향으로 치우침 — 원시 예측값을 그대로 쓰지 않고 안전마진(+10%)을 얹기로 한 근거가 바로 이 방향성 편향.

---

### ✅ 버그34 — 자가용(DRIVING) 정지 상태에서 재계산 트리거 부재 → 별도 수정 없이 버그40(2-pass)으로 실질적으로 해소, 착수하지 않기로 최종 확정

**결론**: 코드 수정 없음(착수하지 않기로 결정).

**원래 문제**: READY 상태에서 anchor로부터 500m 이상 이동하지 않으면 `departureAlarmTime`이 새벽 4시 값에 고정되는 문제 — 재계산 트리거가 `isFirstReceive`/`isOutOfAnchor`(500m 이동)/`isNearDest` 세 조건의 OR뿐이라 순수 시간 경과로는 재계산이 안 됨.

**착수하지 않기로 한 이유**: 버그40 실측 결과 2-pass가 리드타임과 무관하게 안정적인 정확도를 보여서(리드타임이 가장 긴 44.5h 시나리오도 오차가 더 커지지 않음), 새벽 4시에 2-pass로 한 번만 계산해도 이미 "실제 출발 예정 시각의 예측 교통상황"이 반영된 값이 나옴 — 재계산 트리거를 별도로 추가할 필요성이 크게 줄어듦. 게다가 재검토 결과, 이 버그의 재계산 트리거(`isOutOfAnchor`, 500m 이동)는 애초에 "가만히 있는 사용자"에게는 폴링 방식에서도 원래부터 작동하지 않았음(이동 기반이지 시간 기반이 아니므로) — 즉 이 오차가 실제로 영향을 주는 상황(정지 상태)에서는 이 트리거를 고쳐도 도움이 안 됨. 지오펜싱 도입 계획(`docs/history/geofencing-migration-plan.md`)에서도 이 결론을 전제로 별도 안전망을 추가하지 않기로 함.

---

## 수정 완료 목록 (2026-08-13)

### ✅ NEARDEST 지오펜스 EXIT 처리(및 평소 배경 폴링)의 `/location` 호출이 백그라운드에서 25~120초씩 불규칙 지연 → 근본 원인은 JS `setTimeout` 기반 타임아웃의 신뢰성 부족, 네이티브 `XMLHttpRequest.timeout`으로 해결

**파일**: `src/tasks/backgroundLocationTask.ts`(`patchLocation`/`patchLocationOnce` 재작성), `src/tasks/nearDestGeofenceTask.ts`(등록 실패 처리·좌표 확보 순서 수정), `node_modules/expo-location` 패치(`patches/expo-location+19.0.8.patch`), `node_modules/expo-task-manager` 패치(`patches/expo-task-manager+14.0.9.patch`), `package.json`(`"postinstall": "patch-package"` 추가)

**증상**: NEARDEST 지오펜스 EXIT 이벤트 수신과 GPS 좌표 확보는 항상 빨랐다(수십 ms, 백그라운드에서도 동일). 그런데 그 직후 서버에 위치를 보고하는 `/location` 호출(`patchLocation()`)만 백그라운드 상태에서 25초~119.7초까지 불규칙하게 지연됐다가, **정확히 앱을 포그라운드로 전환하는 순간 응답이 도착**하는 패턴이 여러 차례 실측으로 반복 확인됐다. 평소 배경 폴링 태스크(`BackgroundLocation`, 지오펜스와 무관한 별개 실행 경로)의 동일 호출에서도 같은 패턴이 나타나, 지오펜스 전용 문제가 아니라는 게 드러났다. 부수 증상으로 이로 인해 "EXIT 로컬 알림이 백그라운드에서 안 뜨고 앱을 열어야만 뜬다"는 사용자 체감 버그가 발생했다.

**기각된 가설들 (각각 실측/코드 확인으로 검증 후 기각)**:
1. 시스템 위치 캐시 오염으로 최신 좌표를 못 씀 → 좌표 확보 순서를 신규GPS 우선으로 바꿔도 무관하게 재현돼 기각.
2. 배터리 최적화/백그라운드 데이터 제한/삼성 절전 앱 목록 → 전부 "제한 없음/허용/예외" 정상 확인.
3. App Standby Bucket → `adb shell am get-standby-bucket <패키지>`로 `5`(`EXEMPTED`, 최고 등급) 확인.
4. Doze 화이트리스트 → `adb shell dumpsys deviceidle`로 앱이 화이트리스트에 등록돼 있고 기기도 비-Doze 상태임을 확인.
5. 지오펜스 태스크가 안드로이드 `JobScheduler` 경로를 타서 FGS급 실행 우선순위를 못 받는다 → `TaskManagerUtils.java`의 `createJobInfo()`에 안드로이드 12+ 대상 `setExpedited(true)`를 패치해 재빌드·재검증했으나 **효과 없음**(지연폭 그대로: 57.9초/46.8초). 결정적으로, JobScheduler를 전혀 안 쓰고 FGS가 직접 콜백을 주는 평소 배경 폴링 태스크(`BackgroundLocation`)에서도 동일 패턴(최대 119.7초)이 재현돼 이 가설도 기각. `setExpedited(true)` 패치 자체는 부작용이 없어 코드에는 그대로 남겨둠(아래 "부수적으로 함께 고친 것" 참고).

**최종 원인**: `patchLocation()`이 쓰던 타임아웃 메커니즘(`fetch()` + `AbortController` + JS `setTimeout`)이 백그라운드에서 신뢰할 수 없었다. 지연 구간 도중에도 GPS 네이티브 호출이나 다른 TaskManager 헤드리스 태스크 발화 등 "JS 엔진이 살아있다"는 증거는 계속 나왔는데(로그로 확인), 오직 JS `setTimeout` 기반 타임아웃만 단 한 번도 제때 발동하지 않았다 — 매번 "1차 성공" 로그로 지연시간 전체를 그대로 먹고 끝났고, 재시도 로직 자체가 발동한 사례가 하나도 없었다. 즉 네트워크가 근본적으로 막힌 게 아니라, **리액트 네이티브의 JS `setTimeout` 콜백 전달이 백그라운드에서 지연될 수 있다는 알려진 한계**로 인해 저희 타임아웃 안전장치가 무력화되고 있었던 것으로 보인다.

**해결**: `patchLocationOnce()`를 `fetch()`+`AbortController`(JS 타이머 경유) 대신 `XMLHttpRequest`의 네이티브 `timeout` 속성으로 재작성했다. 이 속성은 JS 타이머를 전혀 거치지 않고 안드로이드 네이티브 `NetworkingModule.kt`에서 OkHttp의 `callTimeout()`으로 직접 연결된다(코드로 직접 확인, `NetworkingModule.kt:329-330`) — JS 스레드 상태와 무관하게 OkHttp 자체 감시 스레드가 시간을 잰다.

```ts
function patchLocationOnce(path: string, token: string, lat: number, lng: number, timeoutMs: number): Promise<any> {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.timeout = timeoutMs; // 네이티브 레벨(OkHttp callTimeout) — JS 타이머 아님
    xhr.open('PATCH', `${BASE_URL}${path}`);
    xhr.setRequestHeader('Authorization', `Bearer ${token}`);
    xhr.setRequestHeader('Content-Type', 'application/json');
    xhr.setRequestHeader('Accept', 'application/json');
    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        try { resolve(JSON.parse(xhr.responseText)); } catch (e) { reject(e); }
      } else {
        reject(new Error(`HTTP ${xhr.status}`));
      }
    };
    xhr.onerror = () => reject(new Error('네트워크 오류'));
    xhr.ontimeout = () => reject(new Error(`timeout after ${timeoutMs}ms`));
    xhr.send(JSON.stringify({ lat, lng }));
  });
}
```

`patchLocation()`(1회 재시도 래퍼)의 나머지 로직(HTTP 4xx/5xx는 재시도 안 함, 타이밍 로그, 디버그 알림)은 `patchLocationOnce()`가 같은 형태의 에러(`HTTP ${status}`)를 던지는 한 그대로 재사용 가능해서 손대지 않았다.

**검증**: 재빌드 후 실기기로 백그라운드 상태에서 100m 경계를 2회 이상 왕복(ENTER/EXIT 반복) + 포그라운드 전환까지 같이 테스트. 전 구간에서 `/location` 응답이 193~476ms로 일관되게 빠르게 왔고, 재시도 로직이 단 한 번도 발동할 필요가 없었다(로그에 "1차 실패" 기록 0건). 기존(25~120초) 대비 확실한 개선을 확인.

**부수적으로 함께 고친 것**:
- `GeofencingTaskConsumer.kt`의 `addGeofences()`/`removeGeofences()`가 성공/실패 여부를 전혀 확인 안 하던 결함을 패치 — Play Services `Task`에 `addOnSuccessListener`/`addOnFailureListener`를 달아서, 실패 시 기존 이벤트 전달 통로(`taskManagerUtils.scheduleJob`)를 재사용해 JS(`nearDestGeofenceTask.ts`)까지 신호(`eventType: -1`)를 보낸다. JS 쪽은 이 신호를 받으면 현재 등록돼 있다고 믿는 모든 key를 **한 번에** 정리(개별 반복 호출 시 `saveRegionsAndSync`가 매번 재등록을 시도해서 스스로 재등록 경쟁을 만드는 문제가 있어 벌크 처리로 수정)하고 전부 폴링으로 자동 폴백한다. (`patches/expo-location+19.0.8.patch`)
- `setExpedited(true)` 패치(`TaskManagerUtils.java`)는 최종적으로 근본 원인이 아닌 것으로 확인됐지만 부작용도 없어 그대로 유지(`patches/expo-task-manager+14.0.9.patch`) — 이 Job 스케줄링 경로는 지오펜스뿐 아니라 `expo-notifications`의 `BackgroundRemoteNotificationTaskConsumer`(우리 `BACKGROUND_ALARM_TASK`, 즉 새벽 4시 FCM 웨이크업)와도 공유되므로, 이 패치가 그쪽에도 함께 적용된다는 점을 인지해둘 것(현재까지 부작용 관측 없음). `LocationTaskConsumer.kt`도 `scheduleJob`을 호출하지만 `deferredUpdatesInterval` 옵션을 쓸 때만 타는 경로라 우리 앱(옵션 미사용)엔 영향 없음을 코드로 확인.
- `backgroundLocationTask.ts`의 `BACKGROUND_LOCATION_TIME_INTERVAL_MS`를 진단용 임시값(1시간)에서 원래 운영값(30초)으로 원복.
- `nearDestGeofenceTask.ts`의 EXIT 좌표 확보 순서를 캐시 우선 → 신규GPS 우선으로 변경(정확도 우선, 캐시는 실패 시 폴백으로 유지).

**남은 과제(의도적으로 미룸)**: 지오펜스 목록을 짧은 시간에 반복 등록/해제하면(`GeofencingClient.addGeofences`/`removeGeofences`가 둘 다 fire-and-forget이라 순서 보장이 없음) 이론적으로 여전히 경쟁 상태가 발생할 수 있다. 오늘 결론상 이게 실제 지연의 원인은 아니었던 것으로 확정됐지만(위 "최종 원인" 참고), 근본적으로 고친 건 아니라서 지오펜스를 짧은 간격으로 반복 등록/해제하는 시나리오(예: 트러블슈팅 중 알람을 여러 번 빠르게 만들고 지움)에서 다시 마주칠 가능성은 남아있다.

**교훈(향후 백그라운드 네트워크 작업에 재사용)**: 리액트 네이티브에서 백그라운드 상태의 네트워크 호출에 타임아웃을 걸어야 한다면, `fetch()`+`AbortController`(JS 타이머 경유)가 아니라 `XMLHttpRequest.timeout`(네이티브 레벨, OkHttp `callTimeout()`으로 직결)을 쓸 것 — JS 타이머가 백그라운드에서 지연 전달되는 문제를 원천적으로 피할 수 있다. `fetch()`는 RN에서 결국 `XMLHttpRequest`를 감싼 JS 폴리필(`whatwg-fetch`)일 뿐이라, XHR을 직접 쓴다고 "더 구식/저수준"이 되는 게 아니라 같은 엔진의 다른 진입점을 쓰는 것뿐이다.

**설계 자체는 끝나있었음(참고용, 필요해지면 재검토)**: `JourneyService.updateLocation()` READY 분기에 `isAlarmImminent` 조건 추가, `GeoConstants.ALARM_IMMINENT_THRESHOLD_MINUTES` 상수 신설 — 위 결론에 따라 실제 구현은 하지 않음.

### ✅ FGS(포그라운드 서비스)와 GPS 폴링을 완전히 분리하는 독립 네이티브 모듈 도입 (GoNow_Fronted)

**파일**: `modules/foreground-service/`(신규 — `ForegroundAlarmService.kt`, `ForegroundServiceModule.kt`, `index.ts` 등), `src/tasks/backgroundLocationTask.ts`, `src/services/alarmService.ts`, `src/tasks/nearDestGeofenceTask.ts`, `src/tasks/backgroundAlarmTask.ts`

**증상**: `expo-location`의 `Location.startLocationUpdatesAsync(taskName, { foregroundService: {...} })`는 FGS와 GPS 구독을 하나의 API로 묶어서, "FGS(상단바 알림)는 유지하되 GPS 폴링만 멈춘다"는 조합을 표현할 수 없었다. NEARDEST를 순수 지오펜싱으로 전환한 뒤에도(위 항목 참고) 이 제약 때문에 지오펜싱만으로 충분한 상태에서조차 GPS 폴링이 계속 돌아, 지오펜싱 도입의 배터리 절약 목적 중 "GPS 칩 사용량 감소"만 실현이 안 되고 있었다.

**해결**: 알림 표시만 전담하는 순수 Android `Service`(`ForegroundAlarmService.kt`, 위치 콜백 전혀 참조 안 함)를 새 로컬 Expo 모듈로 만들고, GPS 구독은 항상 `foregroundService` 옵션 없이만 시작하도록 통일했다. `startAlarmForegroundService()`/`stopAlarmForegroundService()`(FGS 전담)와 `startGpsPolling()`/`stopGpsPolling()`(GPS 전담)이 완전히 독립된 함수가 됐고, `maybeSyncGpsPolling()`이 `ACTIVE_JOURNEYS_KEY`/`ACTIVE_APPOINTMENTS_KEY` 기준으로 GPS 폴링 필요 여부만 별도 재판단한다. 부수적으로, expo-location API의 한계 때문에 있었던 "FGS 없는 구독 발견 시 stop→재시작(승격)" 로직(`LOCATION_FGS_ACTIVE_KEY` 기반, 과거 크래시 전례가 있던 위험한 패턴)이 개념 자체가 사라져 통째로 제거됐다.

**검증**: 실기기 dumpsys로 ① FGS만 켰을 때 GPS 요청 0건 ② FGS+GPS 동시 실행 ③ GPS만 끄고 FGS는 유지되는 것 ④ 실제 알람 생성→백그라운드 전환→NEARDEST 흐름에서 GPS 폴링이 멈추고 FGS는 67초 넘게 `isForeground=true` 유지되는 것까지 전부 확인. `npx tsc --noEmit` 통과.

**부수적으로 함께 고친 것(같은 세션 코드 리뷰로 발견)**:
- **`DESIRED_INTERVALS_KEY`(서버가 지시한 폴링 주기) 경쟁 조건**: 이 키를 잠금 없이 건드리는 지점이 헤드리스 틱(`backgroundLocationTask.ts`, 블랭킷 덮어쓰기), `fallbackToPolling()`(`nearDestGeofenceTask.ts`, 삭제), 포그라운드 폴링(`alarmService.ts`, fire-and-forget 갱신) 세 곳이나 있어서, 헤드리스 틱이 값을 읽은 "직후" 다른 경로가 특정 key를 지워도 틱이 자기 스냅샷을 통째로 다시 쓰면서 그 삭제를 되살릴 수 있었다 — NEARDEST 재진입 시 도착 확인 알림이 최대 5분 지연되는 버그(서버가 내려준 긴 주기가 EXIT 후에도 안 지워짐)가 이 경쟁 구간에서 재발할 수 있는 구조였다. `withIntervalsLock` + `setDesiredInterval()`/`clearDesiredInterval()` locked 헬퍼를 도입하고, 헤드리스 틱은 통째 덮어쓰기 대신 자신이 실제로 바꾼 key만 델타로 모아 최신 상태에 병합하도록 변경(`ACTIVE_JOURNEYS_KEY` 등에 이미 쓰던 것과 동일한 원칙).
- **로그아웃(`AlarmManager.stopAll()`) 시 `stopBackgroundLocationUpdates()` 최대 N+2회 중복 호출**: `forEach(r => r.stop())`가 각 runner의 `onFinish`를 개별적으로 트리거해 매번 재확인하고, `stopAll()` 자신도 한 번 더 호출하고, 호출부(`ProfileSettingsScreen.tsx`)도 또 한 번 명시적으로 호출 + `ACTIVE_JOURNEYS_KEY`/`ACTIVE_APPOINTMENTS_KEY`를 잠금 없이 직접 덮어쓰고 있었다. 무해했지만(각 함수가 "이미 처리됨"을 자체 체크) 비효율적이라, `stopAll()`이 각 runner의 `onFinish`를 무력화한 뒤 신설한 `clearActiveIds()`(locked)로 한 번만 정리하도록 정리.
- **위치 권한 미승인 상태에서 FGS 시작 시 앱 전체 크래시**: `ForegroundAlarmService.kt`가 권한 확인 없이 `startForeground(..., FOREGROUND_SERVICE_TYPE_LOCATION)`을 호출해서, 신규 설치 기기(위치 권한 승인 전)에서 로그인 직후 기존 알람을 발견해 FGS를 켜려는 순간 `SecurityException`으로 앱이 죽는 크래시가 실기기(신규 빌드 최초 실행)로 확인됐다. `startAlarmForegroundService()`(JS)에 `Location.getForegroundPermissionsAsync()` 체크를 추가해 미승인 시 조용히 skip하도록 하고, `ForegroundAlarmService.kt`에도 `SecurityException` try/catch(방어용, 이 서비스만 `stopSelf()`로 조용히 중단)를 추가. 로컬 release 빌드로 재현·재검증 완료(크래시 없이 정상 기동 확인).
- **dev-client 환경에서 지오펜스 EXIT 처리 중 동적 import 실패로 헤드리스 태스크 전체 크래시**: `fallbackToPolling()`이 포그라운드일 때 `await import('@/src/services/alarmService')`로 순환 참조를 회피하는데, dev-client는 이 모듈을 앱 시작 시 전부 들고 있지 않고 Metro 서버(`127.0.0.1:8081`)에서 필요할 때 받아온다 — USB를 뽑고 밖에서 테스트하면 이 터널이 끊겨서 `LoadBundleFromServerRequestError`로 실패하고, 이 예외가 안 잡혀서 `NEARDEST-GEOFENCE-TASK` 전체(`TaskManager: Task ... failed`)가 죽는 게 실외 실기기 테스트로 재현됐다. `/location` 호출과 지오펜스 해제 자체는 이미 성공한 뒤였음에도 폴링 재개(`alarmService.resumeFromGeofence()`)만 못 하고 죽는 형태. 동적 import를 try/catch로 감싸서 실패 시 예외를 삼키고 아래 백그라운드 폴링 재시작 경로로 폴백하도록 방어 코드 추가 — **단, 근본 해결은 아니고 dev-client가 아닌 standalone 빌드(EAS build 또는 `expo run:android --variant release`)로 테스트하면 애초에 이 실패 자체가 안 일어난다.** 실외 재테스트에서 포그라운드·백그라운드 EXIT 둘 다 크래시 없이 정상 처리되는 것까지 확인.

## 수정 완료 목록 (2026-08-17)

### ✅ 버그44 — 반복 여정이 ARRIVED → READY로 넘어갈 때 앵커/출발 알람 시각이 어제 값으로 남아있어 당일 재계산 없이 곧바로 DEPARTING 오판 가능 (스프링)

**파일**: `JourneyRepository.java`, `ReadyTransitionService.java`

**증상**: `ReadyTransitionService.transitionToReady()`가 반복 여정을 `ARRIVED → READY`로 벌크 전환할 때 `status`만 바꾸고 `current_lat/lng`(앵커)·`departure_alarm_time`은 그대로 남겨뒀다(수정 API 호출 시엔 두 값을 null로 리셋하는 코드가 있는데, 이 스케줄러 경로엔 없었음). `JourneyService`의 READY 분기는 "앵커가 없거나(`isFirstReceive`) 앵커에서 500m 이상 벗어났을 때만" 플라스크를 재호출해 `departureAlarmTime`을 재계산하는데, 반복 여정은 보통 매일 비슷한 위치(집)에서 첫 GPS를 찍으므로 이 재계산 게이트를 통과 못 하는 경우가 흔하다 — 그러면 어제 계산된 스테일 `departureAlarmTime`으로 곧바로 `isPastAlarmTime` 체크를 해버려서, 오늘 좌표를 한 번도 재계산 안 한 채 DEPARTING으로 잘못 전환될 수 있었다. 지오펜싱 Phase 3(READY) 설계 중 코드 재검토로 발견 — 기존 폴링 방식에도 이미 있던 버그이지만, 새로 만들 `DepartingTransitionScheduler`가 GPS 재계산 게이트 없이 DB의 `departureAlarmTime`만 보고 벌크 전환하는 구조라 스테일 값에 훨씬 직접적으로 노출되는 것을 계기로 먼저 고쳤다.

**해결**: `JourneyRepository`에 `bulkResetAnchorAndAlarmForReadyTransition(today, todayBit)` 신설 — `bulkUpdateToReady`와 동일한 WHERE 조건(대상 집합이 정확히 일치해야 함)으로 `current_lat/lng`·`departure_alarm_time`을 NULL로 리셋하는 네이티브 쿼리. `ReadyTransitionService.transitionToReady()`에서 `bulkUpdateToReady` **직전**에 호출(상태가 READY로 바뀌기 전에 리셋해야 WHERE의 `status IN (SCHEDULED, ARRIVED)` 조건이 유효함). SCHEDULED에서 넘어오는 여정은 두 값이 이미 null이라 리셋이 무해 — 반복/비반복 구분 없이 항상 호출해도 안전. 리셋 결과 반복 여정은 매일 첫 GPS 수신 시 무조건 "최초 좌표 수신"으로 처리돼 플라스크가 재호출된다(기존 로직 그대로 재사용, 새 분기 불필요).

**검증**: `./gradlew compileJava` 통과. 실제 반복 여정 시나리오(새벽 4시 스케줄러 → 당일 첫 GPS)로 실측 검증은 아직 안 함 — 다음 스케줄러 사이클 또는 통합 테스트에서 확인 필요.

### ✅ 버그43 — READY 상태에서 앵커 500m 이탈과 출발 알람 시각 도달이 같은 요청에 겹치면 플라스크가 두 번 호출됨 (스프링)

**파일**: `JourneyService.java`(`updateLocation()` READY 분기), `ParticipantService.java`(동일 구조)

**증상**: READY 분기는 `isOutOfAnchor`(앵커 500m 이탈) 등으로 플라스크를 1차 호출해 `departureAlarmTime`을 갱신한 뒤, 그 갱신된 값으로 곧바로 `isPastAlarmTime`을 재판정한다. 이 판정이 참이면(마침 그 순간 출발 알람 시각도 지나있으면) DEPARTING으로 전환하면서 **같은 요청 안에서 플라스크를 또(2차) 호출**했다 — 두 호출 다 완전히 같은 좌표로 부르므로 결과는 항상 동일, 순수하게 API 호출만 낭비. 자주는 아니지만 사용자가 실제로 이동을 시작하는 시점이 딱 이 순간이면 현실적으로 발생 가능한 엣지케이스(한 여정당 최대 1회).

**해결**: 2차 호출 직전에 `if (flaskResponse == null)` 가드 추가 — 1차 호출로 이미 응답을 받았으면 재사용하고 재호출을 생략. 상태 전이(`updateStatus(DEPARTING)`) 로직 자체는 무변경.

**검증**: `./gradlew compileJava` 통과.

---

## 지오펜싱 Phase 3(READY) 실기기 테스트 중 발견·수정한 프론트 버그 6건 (2026-08-17)

READY 지오펜싱(`readyGeofenceTask.ts`) 실기기 검증 과정에서, 상태 전환 로직 자체와는 별개로 "도착 확인" 흐름과 FGS(포그라운드 서비스) 생명주기 레이어에서 6건의 버그를 추가로 발견·수정했다. 전부 개인/그룹/귀가 알람을 여러 개 연달아 만들고 포그라운드/백그라운드/스와이프를 빠르게 번갈아 테스트해야만 드러나는, "도착 확인이라는 사건이 다른 앱 생명주기 이벤트와 우연히 겹치는" 종류의 경쟁 조건이 대부분이다. 지오펜스 등록/재센터링/핸드오프 등 오늘 검증한 핵심 상태머신 로직과는 무관한 별도 레이어라, 아래 버그들의 발견·수정이 그쪽 검증 결과에 영향을 주지 않는다.

### ✅ `resumeIfDue()`가 READY/DEPARTING 지오펜스 전담 구간도 재폴링시켜 지오펜스가 반복 재등록됨

**파일**: `alarmService.ts` (`AlarmRunner.resumeIfDue()`)

**증상**: 포그라운드 복귀 시 호출되는 `resumeIfDue()`에 "이미 지오펜스로 넘어간 상태면 재폴링 스킵" 가드가 `NEARDEST`만 걸려 있었다(과거 NEARDEST에서 실제로 겪었던 버그의 수정 흔적). Phase 3로 READY/DEPARTING도 지오펜싱 기반이 됐는데 이 가드가 안 넓혀져 있어서, 포그라운드 복귀가 반복되면 이미 지오펜스로 감시 중인 READY 알람이 30초 간격으로 계속 재등록되는 게 실기기 로그로 확인됐다(불필요한 GPS/서버 호출·지오펜스 재등록 반복).

**해결**: `if (this.status === 'NEARDEST') return;` → `if (['NEARDEST', 'READY', 'DEPARTING'].includes(this.status)) return;`로 확장.

**검증**: 실기기 로그로 재등록 반복이 사라진 것 확인.

### ✅ `resumePolling()`이 활성 추적 가드에 막혀 지오펜스 재개 시 실제 `/location` 호출이 안 나감

**파일**: `alarmService.ts` (`AlarmManager.resumeFromGeofence()` → `AlarmRunner.resumePolling()`)

**증상**: `departing_transition` FCM 등으로 지오펜스에서 폴링을 재개시키는 `resumePolling()`이 `this.poll()`(기본값 `skipActiveCheck=false`)을 호출했다. 그런데 이 key는 지오펜스로 넘어갈 때 이미 "활성 추적 목록"(`ACTIVE_JOURNEYS_KEY`)에서 빠진 상태라, `poll()` 내부의 "활성 추적 대상 아님 — 낡은 타이머로 인한 호출 차단" 가드에 걸려 재개가 조용히 무산됐다(로그에 "poll() 진입"만 찍히고 그 뒤로 응답이 전혀 없는 증상으로 발견). `start()`의 최초 1회 호출과 똑같이 이 체크를 건너뛰어야 하는 케이스였는데 누락돼 있었다.

**해결**: `resumePolling()`이 `this.poll(true)`로 호출하도록 수정.

**검증**: 실기기 로그로 `skipActiveCheck:true` 확인 + 그 직후 `/location` 응답이 정상적으로 오는 것 확인.

### ✅ 앱이 완전 종료된 상태에서 알림 액션으로 콜드부팅되면 액션 처리가 통째로 누락됨

**파일**: `app/_layout.tsx`

**증상**: `notifee.onForegroundEvent`/`onBackgroundEvent`는 "리스너 등록 이후에 발생하는 새 이벤트"만 잡는다. 앱이 완전 종료된 상태에서 알림 액션(예: "도착 확인" YES 버튼, `launchActivity: 'default'`로 앱을 콜드부팅시킴)을 누르면, 그 액션 자체가 콜드부팅을 유발한 원인인데 두 리스너 어디에도 안 걸려서 완전히 무시됐다 — 앱은 재실행되지만 `/arrive` 호출도, FGS 정리도, 아무 처리도 안 일어남. `getInitialNotification()`(콜드부팅을 유발한 액션을 별도로 조회하는 API)을 아예 안 쓰고 있던 게 원인. dlog에 "도착확인 YES버튼" 로그 자체가 안 남는 것으로 발견.

**해결**: 포그라운드 핸들러의 액션 처리 로직을 `handleNotificationActionPress()` 함수로 분리하고, `init()`에서 `notifee.getInitialNotification()`을 조회해 존재하면 같은 함수로 처리하도록 추가.

**검증**: 실기기로 앱 완전 종료 후 알림 액션 → dlog에 "콜드부팅 유발 알림 액션 처리" + "도착확인 YES버튼" 로그 정상 확인, `/arrive` 호출 및 FGS 정리까지 정상 동작 확인.

### ✅ 도착 확인 직후 `startReadyAlarms()`의 재조정 로직이 서버 반영 전 stale 상태를 읽고 알람을 도로 살림

**파일**: `alarmService.ts`, `app/_layout.tsx` (`doStartReadyAlarms()`)

**증상**: 알림 액션으로 앱이 포그라운드로 올라오는 순간, "도착확인 YES버튼" 처리(`/arrive` 호출 + `alarmService.stop()`)와 `_layout.tsx`의 "포그라운드 복귀 시 서버 알람 목록 재조회 → 안 돌고 있는데 아직 활성 상태면 재시작" 로직(`startReadyAlarms()`)이 거의 동시에 실행됐다. `/arrive`가 서버에 반영되기 전에 `startReadyAlarms()`의 `getAlarms()`가 먼저 응답하면 아직 `NEARDEST`인 옛 상태를 그대로 읽어와서, 방금 `stop()`한 알람을 "안 돌고 있으니 재시작해야겠다"고 오판해 되살렸다. 서버가 뒤늦게 ARRIVED를 확인해주면서 결국엔 스스로 정리되지만, 그 사이 불필요한 서버 호출·지오펜스/FGS 토글이 낭비됐다.

**해결**: `AlarmManager`에 `recentlyStoppedAt`(key별 마지막 `stop()` 시각, 15초 TTL) 추가, `wasRecentlyStopped()`로 조회 가능하게 노출. `doStartReadyAlarms()`의 재시작 분기(개인/귀가/그룹 3곳)에 `isRunning()` 체크와 나란히 `wasRecentlyStopped()` 체크를 추가해 최근 stop된 key는 재시작을 건너뜀.

**검증**: 실기기 로그로 "방금 stop()됨(서버 반영 전 stale 응답으로 추정) — 재시작 스킵" 확인, 재시작 자체가 더 이상 안 일어남.

### ✅ `startAlarmForegroundService()`/`stopAlarmForegroundService()`의 TOCTOU 경쟁으로 FGS 알림이 중복으로 뜸

**파일**: `backgroundLocationTask.ts`

**증상**: 두 함수 다 "AsyncStorage에서 현재 상태 읽기 → 판단 → 쓰기" 구조인데 직렬화가 안 돼 있었다. 도착 확인 한 번에 서로 다른 두 호출부(`alarmService.stop()`의 onFinish, `startReadyAlarms()`의 `syncForegroundService()`)가 6ms 안에 겹치면, 둘 다 "아직 안 바뀐" 값을 읽어버려서 "이미 꺼져있으면 skip"하는 방어 코드가 무력화되고 "FGS 꺼짐" 알림이 두 번 떴다(위 재시작 경쟁과 맞물려 더 자주 발생).

**해결**: `withNavInfoLock`/`withIntervalsLock`과 동일한 프라미스 체인 락(`withFgsLock`)을 신설해 `startAlarmForegroundServiceInternal`/`stopAlarmForegroundServiceInternal`을 감싸서 직렬화.

**검증**: 실기기 로그로 "실제로 끔" 알림이 정확히 한 번만 뜨고, 두 번째 시도는 "이미 꺼져있어 스킵"으로 정상 차단되는 것 확인.

### ✅ 백그라운드 도착확인 경로가 `alarmService.runners`에 좀비 러너를 남김

**파일**: `notifications.ts` (`onBackgroundEvent`의 `arrival-yes` 처리)

**증상**: 백그라운드 경로는 헤드리스 컨텍스트 안전성 때문에 의도적으로 `alarmService.stop()`을 안 부르고, 대신 영속 저장소(`ALARM_NAV_INFO_KEY`) 기준으로 FGS만 별도 정리하도록 설계했다(설계 자체는 올바름). 그런데 앱 프로세스가 실제로는 살아있는 경우(스와이프만 하고 완전 종료는 아닌, 실측상 흔한 케이스)엔 `alarmService.runners`에서 해당 러너가 안 지워진 채로 좀비로 남았다 — 당장 기능 문제는 없지만, 오래 쌓이면 다른 판단(예: 같은 key 재사용 시점)에 영향을 줄 수 있는 잠재 위험.

**해결**: `AlarmManager`에 `forgetIfExists()` 추가 — `onFinish`(FGS 재확인) 콜백을 억제한 채 메모리에서만 러너를 제거. 진짜 헤드리스 컨텍스트에서 호출되면 애초에 러너가 없어 조용히 no-op이라 안전. 백그라운드 `arrival-yes` 처리에서 FGS 정리와 나란히 호출하도록 추가.

**검증**: 실기기 로그로 "좀비 러너 정리" 로그 확인.

### ✅ 버그29 — READY 상태 GPS interval 계산이 `departureAlarmTime` 임박을 반영 못 해 DEPARTING 전환이 로컬 1단계 알람보다 늦어짐 → 지오펜싱 마이그레이션으로 해소

**관련 저장소**: `gonow-flask`(원인 코드), 연관 로직: 스프링 `JourneyService.updateLocation()`(READY 분기)

**원래 증상**: `READY → DEPARTING` 전환이 GPS 폴링 도착 시점에만 반응하는데, 폴링 주기 계산(`_adaptive_gps_interval`)이 `departureAlarmTime` 임박 여부를 반영하지 않아 최대 5분까지 전환이 지연될 수 있었음(상세는 `docs/history/geofencing-migration-plan.md` 참고).

**해결**: GoNow는 순수 안드로이드 대상 서비스라 iOS 분기를 별도로 고려할 필요가 없다. READY가 지오펜싱 기반으로 전환되면서(2026-08-17) 이 버그의 원인이었던 "폴링 주기 계산"이라는 개념 자체가 READY 상태에서 없어졌고, `P >= Q`(출발 알람 시각 도달)는 신규 서버 스케줄러 `DepartingTransitionScheduler`(매분 폴링형, GPS interval과 무관)가 대신 감시하도록 대체됐다. 플라스크의 interval 계산 로직 자체는 손대지 않았지만, 안드로이드 전용 서비스에서는 이 로직을 더 이상 거치지 않으므로 실질적으로 해소.

### ✅ 버그17 — 자정~새벽 4시 날짜 경계 처리 → 코드 재검토 결과 버그 아님으로 종결

**관련 레이어**: 프론트, 스프링 스케줄러

**원래 우려**: 자정 넘긴 새벽 1~4시에 앱을 켜면 캘린더가 "오늘"을 표시하는데, 그날 알람이 아직 스케줄러(새벽 4시)를 안 거쳐 SCHEDULED로 보여서 혼란을 줄 수 있다는 가설. "00:00~04:00을 전날로 간주해서 날짜 조회 시 하루 빼자"는 정책안이 있었음.

**재검토(2026-08-18) 결과 — 실제로는 문제가 없음을 코드로 확인**:
1. `resolveInitialStatus(planDate)`(`JourneyService.java`/`AppointmentService.java` 양쪽 동일 로직)는 **생성 시점**에 `planDate.isEqual(LocalDate.now())`면 즉시 READY로 만든다. 즉 당일 생성한 알람은 시각과 무관하게 곧바로 READY이고, 새벽 4시 스케줄러를 기다릴 필요 자체가 없다.
2. 새벽 4시 스케줄러(`bulkUpdateToReadyInternal`, `plan_date <= today AND status IN (SCHEDULED, ARRIVED)`)가 실제로 건드리는 건 **며칠 전에 미리 만들어둔 미래 날짜 알람**뿐이다. 이런 알람이 새벽 1~4시에 아직 SCHEDULED로 보이는 건, 그 알람이 몇 시간~며칠 뒤에나 있을 일정이라 GPS 추적이 아직 필요 없다는 뜻이므로 **정확한 상태 표시**다.
3. 서버 타임존은 `GonowApplication.java`의 `TimeZone.setDefault(Asia/Seoul)`로 고정돼 있어 `LocalDate.now()`가 사용자 실제 시계와 어긋날 여지가 없다.
4. 캘린더가 어느 날짜 칸에 표시되는지(`plan_date`)와 알람 상태(`myStatus`)는 `AlarmResponse`에서 완전히 독립된 필드다 — 날짜 버킷을 하루 밀어도 상태 문구(SCHEDULED)는 그대로라, 애초에 제안된 해결책이 우려했던 증상 자체를 못 고친다.
5. 막차 모드는 생성 시점의 `plan_date`(나간 날)를 그대로 쓰는 게 의도된 설계다 — "어젯밤 나갔다가 새벽에 들어왔다"를 캘린더가 나간 날 기준으로 보여주는 건 직관과 일치하며, 이걸 다른 날짜로 재해석하는 로직은 서버 어디에도 없고 있을 필요도 없다.
6. 프론트 "오늘" 계산은 캘린더/목록 화면(`MainCalendarScreen.tsx` 등 7개 파일)에만 쓰이고, 실제 알람 엔진(`alarmService.ts`, 지오펜스 태스크들)에는 전혀 관여하지 않는다 — GPS/지오펜스/로컬 알림 동작은 이 계산과 완전히 무관하게 서버 스케줄러·FCM·절대 시각 기준으로만 움직인다.

**결론**: 데이터 유실, 잘못된 상태 표시, 시간대 불일치 등 실질적 문제를 하나도 발견하지 못함 — 애초에 이론적으로 제기된 우려였고 실사용 재현 사례도 없었음. 코드 변경 없이 종결.

---

## `dlog` 통일 (2026-08-17)

위 6건 중 여러 개가 "이 지점은 `console.log`만 있고 `dlog`가 없어서 원인 파악에 시간이 걸렸다"는 공통 패턴으로 발견됐다. `dlog()`는 내부적으로 `console.log()`도 호출하므로(`deviceLogger.ts`) `console.log`의 완전 상위호환이라는 게 확인돼, 알람/GPS/지오펜스/알림 서브시스템 8개 파일(`alarmService.ts`, `backgroundLocationTask.ts`, `_layout.tsx`, `notifications.ts`, `readyGeofenceTask.ts`, `departingGeofenceTask.ts`, `nearDestGeofenceTask.ts`, `movingGeofenceTask.ts`) 안의 `console.log` 181개를 전부 `dlog`로 통일했다(단순 문자열 인자 155개는 스크립트로 일괄 변환, 에러 객체를 포함한 다중 인자 26개는 수동으로 병합). 이 서브시스템 밖(로그인/설정 등 무관한 화면)은 대상에서 제외 — 앞으로도 이 8개 파일 안에서는 `console.log` 대신 항상 `dlog`를 쓰는 것을 규칙으로 확정.

---

## 수정 완료 목록 (2026-08-18)

### ✅ 버그45 — 반복 알람이 ARRIVED를 지나도 FGS(포그라운드 서비스)를 유지하도록 `AlarmRunner` 생명주기 재설계

**관련 저장소**: 프론트(`GoNow_Fronted`)

**파일**: `src/services/alarmService.ts`(`AlarmRunner`/`AlarmManager`), `src/tasks/backgroundLocationTask.ts`, `src/tasks/departingGeofenceTask.ts`(`finishAsArrived()`, `readyGeofenceTask.ts`/`movingGeofenceTask.ts` 공유), `src/tasks/backgroundAlarmTask.ts`, `src/utils/notifications.ts`, `src/screens/auth/LoginScreen.tsx`, 그 외 4개 알람 생성/수정 화면(`PersonalAlarmSheet.tsx`, `HomeAlarmSheet.tsx`, `PersonalAllAlarmSheet.tsx`, `HomeAllAlarmSheet.tsx`), `DailyAlarmScreen.tsx`, `app/_layout.tsx`

**배경**: 지오펜싱 마이그레이션에서 확정된 정책은 "알람이 하나라도 있으면 FGS를 상시 유지, 완전히 없어지면만 끈다"(안드로이드 12+에서 백그라운드 상태로는 FGS를 새로 못 켜기 때문). 그런데 ARRIVED 도달 시 반복 여부와 무관하게 무조건 `AlarmRunner.stop()`이 호출돼 nav info까지 지워졌다 — 반복 여정은 다음 회차에 서버가 다시 READY로 되돌리는데, 그 사이 FGS가 꺼져 있으면 다음 회차 진입(주로 백그라운드)때 다시 FGS를 못 켜는 문제가 매 회차 재발할 수 있었다.

**해결**: ARRIVED 도달 시 반복 여정(`repeatDays !== 0`)이면 `ALARM_NAV_INFO_KEY` 엔트리만 남기고(파킹) 나머지(타이머/단계별 알람/지오펜스/`AlarmManager.runners`)는 그대로 정리하도록 수정. ARRIVED로 가는 경로가 하나가 아니라 4개(①`/location` 폴링 응답 자연 전환, ②사용자의 "도착 확인" 버튼, ③서버 강제 전환 `sync_event: auto_arrived` FCM, ④목적지 100m 지오펜스 ENTER 감지)임을 확인하고 전부 대칭 적용:
- `AlarmManager.stop()`에 `preserveIfRepeating` 파라미터 추가 — "도착확인/auto_arrived"(true) vs "삭제/비활성화/SCHEDULED 복귀"(기본값 false, 기존 호출부 무변경) 구분
- 백그라운드 전용 헬퍼 `removeOrParkAlarmNavInfo()`(`backgroundLocationTask.ts`) 신설, `notifications.ts`/`backgroundAlarmTask.ts`/`departingGeofenceTask.ts`의 직접 `removeAlarmNavInfo()` 호출 대체
- `AlarmTarget`에 `repeatDays` 필드 추가, `alarmService.start()` 호출부 9곳에 전부 플러밍
- FGS on/off 판단을 포그라운드(`AlarmManager`)·백그라운드(`backgroundLocationTask.ts`) 양쪽에서 `runners.size===0`이어도 `hasAnyTrackedAlarm()`이 true면(파킹 엔트리 존재) GPS만 끄고 FGS는 유지하도록 통일
- 삭제/로그아웃 경로(`AlarmManager.stop()`/`stopAll()`)에 파킹 엔트리 방어적 정리 로직 추가

**코드 리뷰(`/code-review high`)로 추가 발견·수정**:
- `AlarmManager.forgetIfExists()`(백그라운드 도착확인 시 좀비 러너 정리용)가 `runner.stop()`을 인자 없이 불러 nav info를 먼저 지워버리는 바람에, 뒤이은 `removeOrParkAlarmNavInfo()` 판단이 항상 "반복 아님"으로 오판하던 경쟁 조건 — `runner.stop(true)`로 수정
- 지오펜스 ENTER 경로(`finishAsArrived()`)와 auto_arrived FCM 경로(`backgroundAlarmTask.ts`)에 `forgetIfExists()` 호출이 빠져 있어(도착확인 버튼 경로에만 있었음), 프로세스가 살아있는 채로(스와이프만 한 흔한 케이스) 이 경로로 ARRIVED가 처리되면 `AlarmManager.runners`에 좀비 러너가 남아 다음 회차 `isRunning()` 오판 위험 — 두 곳 모두 추가
- 같은 문제가 `backgroundLocationTask.ts`의 헤드리스 폴링 자체(최초 파킹 구현 지점)에도 있었음 — 동일 수정
- `DailyAlarmScreen.tsx`의 `toggleAlarm()`이 `alarmType`을 `isLastMode`로 잘못 유추해 데드라인 모드 HOME 여정이 토글 ON 시 PERSONAL로 등록되던 기존 버그(이번 작업과 무관하지만 같은 함수를 건드리는 김에 수정) — 호출부가 이미 아는 타입을 명시적으로 전달하도록 변경
- `AlarmManager`의 `onFinish` 콜백과 `stop()`의 파킹 방어 분기가 동일한 GPS/FGS 3단계 판단을 중복 구현 — `reconcileGpsAfterRunnerGone()`으로 통합
- `stop()`의 파킹 방어 분기가 한 번도 시작된 적 없는 key에도 매번 지오펜스 해제 4종+정리를 돌던 비효율 — `hasAlarmNavInfo()` 사전 체크로 제거
- 로그아웃이 파킹 엔트리를 전부 지우는 게(정상 동작) 재로그인 시 문제가 됨 — `LoginScreen.tsx`의 복구 로직이 `ARRIVED`(파킹 대상)를 걸러내서 재로그인해도 파킹이 영구히 복원 안 되던 문제(사용자 질문으로 발견) → `my_status==='ARRIVED' && repeat_days` 조건의 별도 필터로 nav info 복원 + FGS 재시작 로직 추가

**실기기 검증(2026-08-18)**: 개인/귀가 알람 × 4개 ARRIVED 경로 중 3개(폴링 자연 전환, 도착확인 버튼 포그라운드/백그라운드, 지오펜스 ENTER — 실제 도보 이동으로 확인)를 `adb logcat`으로 검증. 추가로 파킹 중 삭제, 로그아웃(FGS 종료 확인), 로그아웃→재로그인(파킹 복원+FGS 재시작 확인), 다음 회차 재개(`POST /internal/scheduler/ready`로 즉시 재현, 스와이프 상태에서 FCM 수신+GPS 폴링 재개+FGS 신규 시작 없이 유지됨 확인)까지 전부 실기기로 확인. auto_arrived FCM 경로(targetTime+1시간 대기 필요)만 실기기 미검증 — 다른 3개 경로와 완전히 동일한 코드 패턴(`forgetIfExists()`+`removeOrParkAlarmNavInfo()`)이라 리스크 낮음으로 판단.

### ✅ 버그46 — `resumeIfDue()`가 낡은 `this.status`로 포그라운드 재폴링을 조용히 스킵 — MOVING 여정이 영원히 폴링 재개 안 됨

**관련 저장소**: 프론트(`GoNow_Fronted`)

**파일**: `src/services/alarmService.ts`(`AlarmRunner.resumeIfDue()`)

**배경**: 지오펜싱 마이그레이션(READY/DEPARTING이 2026-08-17에 지오펜싱으로 전환됨)으로 READY→DEPARTING→MOVING 전환이 전부 백그라운드/헤드리스 지오펜스 이벤트로 일어나게 됐다. `resumeIfDue()`(포그라운드 복귀 시 폴링 재개 여부 판단)는 NEARDEST 전용으로 만들어진 낡은 사전 필터 `if (['NEARDEST','READY','DEPARTING'].includes(this.status)) return;`를 그대로 재사용하고 있었는데, `this.status`는 그 `AlarmRunner` 인스턴스가 마지막으로 "스스로" 관찰한 상태일 뿐이다.

**증상**: READY→DEPARTING→MOVING 전환이 지오펜스(백그라운드)로 일어나면 포그라운드 러너 인스턴스는 이 전환들을 못 보고 `this.status`가 옛 값에 계속 묶인다. 실제로는 MOVING(폴링 필요)인데 앱을 포그라운드로 돌려도 `resumeIfDue()`가 낡은 값만 보고 "지오펜스 전담 구간"으로 오판해 로그 한 줄 없이 조용히 재폴링을 스킵 — 그 러너는 이후 몇 번을 포그라운드로 돌아와도 다시는 폴링을 재개 못 했다. 사용자가 실기기로 "백그라운드/스와이프에서는 잘 오는데 포그라운드에서는 전혀 안 온다" 패턴으로 발견.

**해결**: 바로 다음 줄에 이미 있던 훨씬 신뢰할 수 있는 체크(`isKeyActivelyTracked()` — `ACTIVE_JOURNEYS_KEY`/`ACTIVE_APPOINTMENTS_KEY` 멤버십, 감지 주체와 무관하게 항상 정확)만으로 판단하도록 `this.status` 기반 사전 필터를 완전히 제거.

**실기기 검증(2026-08-18)**: 실제 도보 이동으로 READY→DEPARTING까지 지오펜스로 진행 후, 백그라운드에서 앵커 EXIT로 MOVING 확정된 직후 포그라운드 전환 — `AlarmRunner.status`가 여전히 낡은 `DEPARTING`이었는데도 `isKeyActivelyTracked()`만으로 정확히 판단해 5초 뒤 재폴링이 발동하고 `DEPARTING → MOVING` 상태전이가 정상 반영되는 것을 로그로 직접 확인. 이후 여러 차례 포그라운드/백그라운드 전환에도 MOVING 폴링이 계속 정상 유지되다 ARRIVED 파킹까지 정상 완료.

---

### ✅ 버그48 — 자가용(DRIVING) 모드에서 출발지-목적지가 5m 이내면 카카오모빌리티가 경로 탐색을 거부해 생성 직후 알람이 통째로 사라짐

**관련 저장소**: `gonow-flask`(플라스크)

**파일**: `CounterClockEngine2/CounterClockEngine/gps_api/routes/personal.py`(`_get_driving_duration()`)

**증상**: 자가용(DRIVING) 모드 개인/귀가 여정을 현재 위치와 목적지가 거의 같은 곳으로 만들면(실기기 재현 사례: "지성타운"→"지성타운"), 생성 직후 첫 GPS 폴링에서 알람이 통째로 사라짐. 실기기 로그로 "서버 HTTP 400 → ID 제거 (삭제된 알람)"가 생성 1초 만에 찍히는 것으로 발견됨.

**원인**: `_get_driving_duration()`이 카카오모빌리티 Directions API(`kakao_fetch_route`)를 호출하기 전에 "출발지-목적지가 너무 가까운 경우"를 걸러주는 사전 체크가 없었음 — 대중교통(막차) 모드엔 `walk_fallback()`(`transit_route.py`)이 있었지만 DRIVING엔 대응물이 없었다. 카카오모빌리티 공식 문서(`developers.kakaomobility.com/guide/navi-api/reference.html`, WebFetch로 직접 확인)에 따르면 `result_code: 104`는 "출발지와 도착지가 5m 이내로 설정된 경우 경로를 탐색할 수 없음"을 의미하며, 이 경우 `kakao_route.py`의 `_call_directions_api()`가 `ValueError`를 던져 플라스크가 502를 반환하고, 스프링 `GlobalExceptionHandler.handleRestClientResponse()`가 이를 고정 문구 400으로 감싸 프론트에 전달한다. 프론트는 이 4xx를 "삭제된 알람"으로 오인해(`alarmService.ts`/`backgroundLocationTask.ts`) 방금 생성한 알람을 완전히 정리(`stop()`)해버린다 — 이 마지막 단계 자체는 여전히 열려있는 버그25(4xx 시 조용히 전체 정리)와 동일한 증상이지만, 이번 건의 근본 원인(카카오 API 거부)은 별개로 새로 발견된 것.

**범위 밖**: `result_code 101/102/103`(시작/도착/경유 지점 "주변 도로를 탐색할 수 없음" — 건물 내부·공원 등 좌표 자체가 차량 도로에 안 붙어 있는 경우)은 거리 문제가 아니라 위치 자체의 문제라 이번 거리 기반 수정으로는 못 막는다. 재현 사례와 무관해 이번 수정 범위에서 제외 — 필요해지면 버그25(4xx 일반 처리)와 묶어 별도로 다룰 것.

**해결**: `_get_driving_duration()` 맨 앞에 `walk_fallback()`과 동일한 패턴의 거리 사전 체크 추가 — `haversine()`(`optimizer.py`, 기존 유틸 재사용)로 계산한 직선거리가 100m 미만이면 카카오 API 호출 자체를 스킵하고 트리비얼 소요시간(60초)을 즉시 반환. 100m는 카카오 문서상 5m 경계에 GPS 오차 감안 안전마진을 둔 값이자, `alarm.py`의 NEARDEST 지오펜스 반경과 동일한 "목적지에 거의 다 왔다"는 기존 상수를 재사용한 것. 60초는 다운스트림(`_adaptive_gps_interval()`)에 0을 넘기는 위험을 피하고 `walk_fallback()`의 "최소 1분, 절대 0 아님" 원칙과 일관되게 맞춘 값. `_get_driving_duration()`은 개인/귀가(`_compute_alarm()`)와 그룹(`_compute_appointment_alarm()`) 양쪽에서 공용으로 호출되므로 이 함수 하나만 고쳐서 세 유형 모두 동시에 방어됨.

---

## 수정 완료 목록 (2026-08-19)

### ✅ 버그27 — "도착 예정 알림" 커스텀 사운드를 시스템 소리 목록에 등록하는 방식 → 앱 내 소리/진동/무음 토글로 재설계해 최종 해결

**관련 저장소**: `GoNow_Fronted`(프론트) + `gonow`(스프링)

**경위**: 원래 버그27은 "MediaStore에 사운드를 정식 등록해서 OS 설정의 소리 선택 목록에 노출시키는" 네이티브 모듈 방식으로 한 차례 프로토타입까지 만들어 실기기 검증을 마쳤으나(과거 기록은 git 히스토리에 남아있지 않음 — 커밋 전 롤백됨), 우선순위 낮음으로 보류돼 있었다. 이번에 다시 착수하면서 실제로 구현·실기기 테스트하는 과정에서 근본적인 설계 재검토가 있었다.

**1단계 — MediaStore 등록 방식 재구현**: `modules/notification-sound-registry/`(Kotlin 로컬 Expo 모듈)를 새로 작성 — `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI`에 `arrived.wav`를 등록(`MimeTypeMap`으로 MIME 타입 동적 조회 — 하드코딩 시 `IllegalArgumentException` 실기기 재현 확인, `IS_PENDING` 플래그로 파일 복사 중 끊긴 깨진 항목 재사용 방지)하고 `NotificationChannel.setSound(content:// URI, ...)`로 채널 생성. 도착 여부 확인 채널에 우선 적용해 실기기에서 시스템 설정 "직접 설정" 목록 노출까지 확인.

**2단계 — MediaStore 방식의 부작용 발견**: 등록된 사운드가 안드로이드 파일 관리자의 "오디오 파일" 목록에도 그대로 노출되고, 시스템 설정의 빨간 마이너스 버튼으로 사용자가 실수로 삭제할 수 있음을 실기기로 확인. 도착 예정/도착 완료 채널(FCM 발송, 채널ID를 스프링이 고정값으로 알고 있어야 함)은 삭제되면 복구 수단이 없어(`resetAlarmChannel()` 방식이 채널ID를 바꿔서 스프링과 어긋나므로 사용 불가) 리스크가 큼.

**3단계 — 근본 설계 전환**: "OS 설정 화면에서 직접 고를 수 있게" 만드는 게 애초 목적이었는데, 대신 **앱 자체에 소리/진동/무음 3-way 토글 UI**를 만들면 OS 설정 노출 자체가 필요 없어진다는 결론에 도달. 채널을 모드별로 3개씩 미리 만들어두고(안드로이드 채널은 한 번 만들면 sound/vibration을 재변경 불가 — 그래서 "바뀔 필요 없는" 채널을 모드 수만큼 준비) 실제 발송 시점에 저장된 모드에 맞는 채널ID를 선택하는 방식으로 재설계:
- **도착 여부 확인 / 출발 1~4단계** (100% 로컬 트리거): `AsyncStorage`에 모드 저장, 발송 함수가 그때그때 읽어서 채널 선택. 출발 1~4단계는 단계마다 독립적인 모드 지정 가능(예: 4단계만 소리, 나머지 무음).
- **도착 예정 / 도착 완료** (FCM, 앱이 완전히 꺼진 상태에서도 OS가 서버 페이로드의 채널ID로 직접 표시 — 로컬 저장만으론 발송 시점에 서버가 알 방법이 없음): 스프링 `MemberSetting`에 `arrivalExpectedSoundMode`/`arrivalCompleteSoundMode`(둘 다 nullable, 부분 업데이트) 필드 신설, `PATCH /api/members/me/arrival-sound` 신설. `ArrivalChannel.getChannelId(AlarmSoundMode)`가 base 채널ID에 모드를 조합(`gonow-arrival-expected-sound`/`-vibrate`/`-silent` 등 총 6개). `ParticipantService.sendGroupNotification()`이 참가자들을 선호도별로 그룹핑해서 그룹마다 별도로 FCM 발송하도록 변경(기존엔 전체에게 한 번에 발송).

**4단계 — 네이티브 모듈 삭제**: 위 재설계로 모든 채널이 "OS 설정 노출 불필요"해지면서 MediaStore 등록(1단계에서 만든 네이티브 모듈) 자체가 무의미해짐 — 전 채널을 notifee 번들 리소스 방식(`sound: 'arrived'` 등)으로 통일하고 `modules/notification-sound-registry/` 삭제. 커스텀 사운드는 `arrived.wav` 하나를 도착 계열 세 알림이 공유(소리 종류가 너무 다양하면 오히려 어떤 알림인지 구분하기 어려워진다는 판단).

**부수 발견/수정**: 실기기 테스트 중 GPS 호출/FGS 켜짐·꺼짐 등 디버그 알림(`sendDebugNotification`)이 `arrived.wav`를 공유해서 실제 도착 알림과 소리로 구분이 안 되는 문제를 발견 — 전용 무음 채널(`gonow-debug-silent`)로 분리.

**최종 아키텍처**: 출발 1~4단계(4×3) + 도착 여부 확인(3) + 도착 예정(3) + 도착 완료(3) = 총 21개 알림 채널, 전부 notifee 번들 리소스 방식. `ArrivalSoundUpdateRequest`가 두 필드 다 nullable인 이유는 프론트가 토글 하나 누를 때마다 즉시 저장하는 UX라(화면 전체를 "저장" 버튼으로 한 번에 제출하는 방식이 아님) 바뀐 필드만 보내면 되게 하기 위함 — 처음엔 두 필드를 `@NotNull`로 묶어서 매번 같이 보내게 했었으나, 리뷰 과정에서 불필요한 프론트-백엔드 결합(과 이론상의 레이스 컨디션)임을 확인하고 부분 업데이트로 수정.

---

## 수정 완료 목록 (2026-08-20)

### ✅ 버그41 — 막차 모드 귀가 여정, 목적지(집) 700m 이내에서 최초 계산 시 새벽 4시에 즉시 DEPARTING/NEARDEST로 오작동

**관련 저장소**: `gonow-flask`(플라스크) + `gonow`(스프링)

**원인**: 막차 모드는 생성 시점엔 `target_time`이 없다(그날 GPS로 확정). 새벽 4시 READY 전환 직후 사용자가 아직 집(목적지)에서 자고 있는 흔한 상황이 겹치면, 플라스크 `alarm.py`의 `_compute_alarm()` 도보 폴백 분기(700m 미만, `walk_fallback()`)가 `target_time=None`일 때 "계산 시점(=지금)"을 기준으로 출발 시각을 계산해버려, `departure_alarm_time`이 계산되자마자 과거가 되거나(100~700m → 즉시 DEPARTING) 스프링의 `isNearDest` 체크가 플라스크 응답과 무관하게 GPS 거리만으로 즉시 NEARDEST로 전환해버렸다(100m 이내). 두 경로 모두 그날 밤 진짜 막차 계산이 이루어지지 않은 채 여정이 새벽에 끝나버림 — 막차 모드의 가장 흔한 실사용 패턴(전날 밤 집에서 자고, 낮에 나갔다가, 그날 밤 막차로 귀가)에서 사실상 매번 재현됐다.

**추가로 발견한 연관 문제(기존 버그41 문서엔 없던 내용)**: 반복(`repeat_days`) 막차 여정은 새벽 4시 리셋 쿼리(버그44)가 앵커/출발알람시각만 리셋하고 `target_time`은 리셋하지 않아, 둘째 날부터는 "최초 계산" 신호(`target_time == null`) 자체가 성립하지 않았다. 스테일한 어제 확정값이 남아있으면 다른(더 나쁜) 코드 경로로 빠지거나, GPS가 오기도 전에 "지각 정리" 스케줄러(`ArrivedTransitionScheduler`)가 여정을 조용히 자동 ARRIVED 처리해버릴 수 있어, 반복 막차 알람은 매일 밤 통째로 죽을 위험이 있었다.

**설계 검토 — 새 상태(`WAITING_OUTING` 등) 도입 vs 가드 조건**: 국소적인 설계 공백인지, FSM 자체를 다시 짜야 하는지 따져본 결과, "막차 시각이 없으면 아직 확정 전"이라는 하루짜리 전제를 반복 여정(여러 날 재사용되는 엔티티)에 그대로 적용한 게 진짜 원인으로 좁혀졌다. 새 상태를 도입하면(READY 진입 전 별도 대기 상태) 운영 MySQL의 네이티브 `ENUM` 컬럼 수동 ALTER, 새 지오펜스 태스크 신설, 프론트 전역 상태 분기 추가, 상태머신 스펙 재작성 등 비용이 커서 기각 — 대신 기존 `isPastAlarmTime()`이 이미 null-safe하게 짜여있던 것(DEPARTING 판정)과 대칭을 맞춰, NEARDEST 판정에도 같은 가드를 추가하는 쪽으로 결정.

**수정 내용**:
- **A. 플라스크** (`gps_api/routes/alarm.py`): `is_last_mode` + `is_short`(700m 미만) 분기에서 `target_time is None`이면 "지금 당장 출발"로 지어내지 않고 `target_time`/`departure_alarm_time`/`boarding_time`/`which_station`을 전부 `None`으로 응답. `interval`은 급할 게 없는 상태라 `INTERVAL_MAX_S`로 반환. `target_time`이 이미 있는 기존 경로는 무변경.
- **B. 스프링** (`Journey.java` + `JourneyService.java`): `Journey`에 `isLastTrainTimeConfirmed()`(`!isLastMode || targetTime != null`) 추가, `updateLocation()`의 READY 분기에서 NEARDEST/DEPARTING 전이 두 곳 모두 이 가드로 감쌈. (최초 이름은 부정형 `isLastTrainTimePending()`이었으나, 호출부 2곳이 전부 `!`로 뒤집어 쓰는 걸 IDE가 감지해서 긍정형으로 리네이밍 — 이중부정 제거)
- **C. 스프링** (`JourneyRepository.java`): `bulkResetAnchorAndAlarmForReadyTransition()`이 새벽 4시 리셋 시 `is_last_mode = TRUE`인 여정의 `target_time`도 함께 `NULL`로 리셋하는 쿼리(`bulkResetLastModeTargetTimeInternal`)를 추가 호출. 데드라인 모드는 `target_time`이 사용자 입력값이라 조건에서 명시적으로 제외. 두 리셋 쿼리 모두 반환값을 아무도 안 써서(카운트가 "0"이어도 이상하지 않은 좁은 대상이라 로그로도 가치가 낮음) `void`로 정리.

**검증**: 로컬에 Docker Desktop(기존에 MySQL용으로 설치돼 있던 것) 위에서 플라스크는 저장소의 기존 `Dockerfile`로 이미지를 빌드해 임시 컨테이너로, 스프링은 `./gradlew bootRun`으로 직접 기동해 end-to-end로 확인:
- 플라스크 단독: 700m 이내+`target_time` 없음 → `null` 응답 확인, 700m 이내+확정값 있음 → 기존처럼 정상 역산(회귀 없음).
- 스프링 통합: 막차 모드 여정에 목적지=현재위치(0m)로 `/location` 호출 → `journeyStatus: READY` 유지 확인(수정 전이었다면 즉시 `NEARDEST`).
- 반복 여정 리셋: 반복 막차 여정을 "어제 ARRIVED로 완료, target_time 확정됨" 상태로 DB에서 직접 세팅 후 `POST /internal/scheduler/ready`(버그21의 테스트 트리거) 실행 → `target_time`/`departure_alarm_time`/앵커 전부 `NULL`로 리셋 확인. 대조군(반복 데드라인 모드)은 같은 실행에도 `target_time`이 그대로 보존됨을 확인.

검증 후 로컬 전용 임시 설정(`application.yml`의 `flask.url` → `localhost:5000`)은 원복, 검증용 컨테이너는 모두 삭제.
