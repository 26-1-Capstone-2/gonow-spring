# GPS 폴링 버그 목록

마지막 업데이트: 2026-08-09

해결된 버그는 `docs/history/resolved-bugs.md` 참고.

---

## 미수정 버그 목록

> 중요도 높은 순으로 정렬

---

### 🟡 버그3 — 백그라운드 폴링 interval 30초 고정 [영향 낮음]

→ 아래 버그3 상세 참고

---

### 🟡 버그8 — 포그라운드 GPS 중복 찌르기 [영향 매우 낮음]

→ 아래 버그8 상세 참고

---

### 🟡 버그17 — 자정~새벽 4시 날짜 경계 처리 [영향 미확인]

→ 아래 버그17 상세 참고

---

### 🟡 버그21 — 인증 없는 테스트용 스케줄러 트리거 엔드포인트 [영향 낮음, 의도적으로 남겨둠]

→ 아래 버그21 상세 참고

---

### 🟡 버그22 — 막차 모드 첫 계산이 검색 창을 넘긴 새벽 시간대(01~04시)에 걸리면 이미 지난 막차를 그대로 응답 [영향 낮음, 재현 조건 희귀]

→ 아래 버그22 상세 참고

---

### 🟡 버그23 — GPS 획득/권한 실패 시 사용자에게 아무 알림 없이 조용히 실패 [영향 중간]

→ 아래 버그23 상세 참고

---

### 🟡 버그25 — `/location` 폴링 4xx 시 프론트에서 조용히 폴링만 중단, 사용자 알림 없음 [영향 낮음]

→ 아래 버그25 상세 참고

---

### 🟢 버그27 — "도착 예정 알림" 커스텀 사운드를 시스템 소리 목록에 등록하는 방식 [버그 아님, 검증 완료 후 보류]

→ 아래 버그27 상세 참고

---

### 🟡 버그29 — READY 상태 GPS interval 계산이 `departureAlarmTime` 임박을 반영 못 해 DEPARTING 전환이 로컬 1단계 알람보다 늦어짐 [영향 중간]

→ 아래 버그29 상세 참고

---

### 🟡 버그34 — 자가용(DRIVING) 정지 상태에서 재계산 트리거가 없어 `departureAlarmTime`이 새벽 4시 값에 고정됨 [영향 중간]

→ 아래 버그34 상세 참고

---

### 🟡 버그40 — 자가용(DRIVING) `departureAlarmTime` 계산이 항상 계산 시점의 실시간 교통정보만 반영, 실제 출발 시각의 교통상황 예측 안 함 [영향 중간]

→ 아래 버그40 상세 참고

---

## 미수정 버그 상세

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

### 🟡 버그21 — 인증 없는 테스트용 스케줄러 트리거 엔드포인트 [영향 낮음, 의도적으로 남겨둠]

**파일**: `src/main/java/com/timemate/gonow/global/controller/TestController.java`

**증상**: `POST /internal/scheduler/ready`가 `SecurityConfig`에 `permitAll()`로 등록되어 인증 없이 누구나 `ReadyTransitionService.transitionToReady()`를 직접 트리거 가능. 코드에 `// TODO: 테스트 완료 후 이 파일 삭제` 주석이 있어 의도적으로 임시로 남겨둔 것으로 보이나, "현재 구현된 API 엔드포인트" 문서 표에도 전혀 등재되지 않아 존재 자체를 잊고 지나칠 위험이 있음(sync-docs 점검 중 발견).

**수정 방향**: 스케줄러/FCM 수동 테스트가 더 이상 필요 없어지면 `TestController.java` 파일 자체를 삭제.

---

### 🟡 버그22 — 막차 모드 첫 계산이 검색 창을 넘긴 새벽 시간대(01~04시)에 걸리면 이미 지난 막차를 그대로 응답 [영향 낮음, 재현 조건 희귀]

**관련 저장소**: `gonow-flask`(플라스크)

**파일**: `CounterClockEngine/gps_api/routes/alarm.py`(`_compute_alarm`, `is_last_mode` 분기)

**증상**: 막차 검색 창은 23시~다음날 01시로 고정. 이진 탐색으로 찾은 `last_departure_dt`가 이 창 안에 있어도, 실제 현재 시각이 01시를 넘긴 상태(예: 새벽 2~4시)에서 `target_time`이 아직 null인 첫 계산이 이뤄지면, 이미 지나버린 과거 시각을 정상 응답처럼 `departure_alarm_time`/`target_time`으로 내려줌(버그19와는 별개, 날짜가 아니라 "이미 지남" 자체를 검증 안 하는 문제).

**재현 조건이 왜 희귀한지**: 이 상황이 실제로 발생하려면 `plan_date=오늘`인 막차 모드 귀가 여정이 "정상 시각에 생성됐지만(스프링의 `JourneyService.validateLastTrainNotAlreadyMissed`가 새벽 1~4시 생성만 차단하므로 이 검증은 통과), 실제 첫 GPS 성공 호출이 그 이후 새벽 1~4시대까지 지연"돼야 함. `alarmService.start()`는 GPS 권한이 없으면 재시도 없이 조용히 끝나므로, 이 경로 자체는 버그23(권한/GPS 실패 무알림)과도 맞물려 있음. 스케줄러로 READY 전환되는 미래 날짜/반복 여정은 READY 자체가 항상 새벽 4시 이후에만 시작되므로 이 문제와 무관.

**한때 수정 시도했다가 롤백한 이력**: `last_departure_dt <= _now_kst()`(단, `target_time is None`일 때만) 체크를 추가했다가, 재현 빈도가 매우 낮은데 비해(위 조건 참고) 이 하나만 고치려고 스프링(버그24)·프론트(버그25) 쪽까지 함께 손대게 되면서 원래 목적(새벽 1~4시 막차 생성 차단, `JourneyService.validateLastTrainNotAlreadyMissed`로 이미 해결됨)과 무관한 범위로 커져 전부 되돌림. 재도입할 경우 재계산 시점엔 검증에서 제외해야 함에 주의 — 확정된 `target_time`으로 재계산하는 MOVING 상태 등까지 걸리면 정상 이동 중인 사용자도 차단하게 됨(한 차례 실제로 겪은 회귀).

**수정 방향(재도입 시)**: `result` 언패킹 직후 `if target_time is None and last_departure_dt <= _now_kst(): abort(404, "오늘 밤 막차는 이미 지났습니다.")` 추가.

---

### 🟡 버그23 — GPS 획득/권한 실패 시 사용자에게 아무 알림 없이 조용히 실패 [영향 중간]

**파일**: `src/services/alarmService.ts`(`AlarmRunner.start()`, `AlarmRunner.poll()`)

**증상**: 두 지점 모두 사용자에게 어떤 피드백도 주지 않음.
- `start()`: `Location.requestForegroundPermissionsAsync()`가 `granted`가 아니면 로그만 남기고 조용히 종료 — 재시도 로직 없음.
- `poll()`: `Location.getCurrentPositionAsync()` 실패(권한 없음/위치 서비스 꺼짐/신호 없음 등) 시 로그만 남기고 `scheduleNextPoll()`로 영원히 조용히 재시도.

**원인**: Android는 몇 달간 안 연 앱의 런타임 권한(위치 포함)을 사용자 조작 없이 자동 회수함("미사용 앱 권한 자동 해제"). 귀가/막차 알람처럼 어쩌다 한 번 쓰는 기능은 이 상태로 방치되기 쉬움 — 사용자는 권한이 꺼진 줄 모른 채 알람이 그냥 안 울리는 것만 경험하게 됨.

**수정 방향**: `notifications.ts`의 `requestNotificationPermission()`이 이미 쓰는 패턴 재사용 — `Alert.alert('위치 권한 필요', '...', [{text:'취소'}, {text:'설정으로 이동', onPress: () => Linking.openSettings()}])`을 GPS 권한 미허용 시에도 동일하게 적용.

**진행 상황 (생성 시점 체크 — 구현·테스트 완료)**: 알람 생성/수정/참여(초대코드) 시점의 사전 체크(`src/utils/permissions.ts`의 `checkCoreAlarmPermissions()`)를 개인/귀가/그룹 알람의 생성·수정·초대코드 참여(`handleJoin()`) 전부(6개 알람 화면 + `GroupAlarmSheet.tsx`/`GroupAllAlarmSheet.tsx`의 `handleJoin()` 2곳 포함, 총 10개 지점)에 적용했다. 위치·알림 권한을 팝업 없이 조회만 하고(팝업 요청은 뜬금없다는 문제가 있어 제외), 하나라도 꺼져있으면 Alert로 "필수 권한 설정" 화면(`PermissionSetupScreen.tsx`)으로 안내한다. 단 이건 "생성 시점" 스냅샷 체크일 뿐, **생성 후 권한이 다시 꺼지는 경우는 여전히 못 막는다** — 아래 런타임 체크가 이 케이스를 위한 것.

**런타임(실행 시점) 체크는 미구현 — 조사해보니 예상보다 범위가 큼**:
- `AlarmRunner.start()`/`poll()`(`src/services/alarmService.ts`)은 앱이 포그라운드이거나 "백그라운드지만 프로세스가 살아있는" 상태에서만 실행된다.
- 앱이 **완전히 종료된 상태**에서의 새벽 4시 GPS 폴링은 별도의 헤드리스 TaskManager 경로(`src/tasks/backgroundLocationTask.ts` + `backgroundAlarmTask.ts`)가 전담하며, 이 경로는 `AlarmRunner`/`alarmService.start()`를 아예 거치지 않는다(자체 `fetch` 기반 `patchLocation()` 로직을 따로 가짐).
- 즉 `alarmService.ts`만 고치는 건 "앱을 켜둔 채 권한이 꺼지는" 경우만 잡고, "생성 후 권한을 끄고 앱도 완전히 꺼둔 채 자는" 경우는 여전히 무알림으로 남는다 — 제대로 고치려면 `backgroundLocationTask.ts`의 에러/위치없음 분기까지 같이 손봐야 한다.
- 이 알림을 사용자에게 보여주려면 `Alert.alert()`가 아니라 notifee 로컬 알림이 필요(백그라운드/헤드리스에서도 떠야 하므로), 새 알림 채널과 스팸 방지용 쿨다운 저장소도 새로 필요하다. 알림 탭 시 특정 화면으로 이동시키는 인프라(`EventType.PRESS` 처리)는 현재 프로젝트에 전혀 없어서 이것도 신규 구현이 필요하다.
- **보류 사유**: 실제 발생하려면 "권한을 한 번 허용 → 알람 생성 → 이후 수동으로 끄거나 몇 달간 미사용으로 안드로이드가 자동 회수"라는 조건이 겹쳐야 하는 엣지케이스라, 파일 3개를 건드리고 새 알림 채널/쿨다운/탭-네비게이션 인프라까지 새로 만들 만큼 우선순위가 높지 않다고 판단해 보류. 다음에 여유 있을 때 위 아키텍처 사실을 참고해서 이어서 작업.

---

### 🟡 버그25 — `/location` 폴링 4xx 시 프론트에서 조용히 폴링만 중단, 사용자 알림 없음 [영향 낮음]

**파일**: `src/services/alarmService.ts`(`pollPersonal`/`pollGroup`), `src/tasks/backgroundLocationTask.ts`

**증상**: `/location` 호출이 4xx를 받으면(서버 상태 코드가 뭐든) 콘솔 로그만 남기고 폴링을 조용히 중단함(포그라운드) / 추적 목록에서 조용히 제거함(백그라운드). 사용자는 알람이 왜 안 울리는지 알 방법이 없음. 버그23(GPS 획득 실패)과 증상은 비슷하지만 원인 계층이 다름(이쪽은 서버 응답 실패, 버그23은 클라이언트 GPS 획득 자체 실패).

**경위**: 버그22 조사 중 발견해서 한 차례 고쳤다가(`notifications.ts`에 `sendErrorAlarm`/`extractApiErrorMessage` 추가 후 로컬 알림 발송), 버그24와 마찬가지로 원래 목적과 무관한 범위라 되돌림. 단, `extractApiErrorMessage`는 시나리오 A(생성 시점 차단) 메시지 표시에 필요해서 남겨둠 — `HomeAlarmSheet.tsx`/`HomeAllAlarmSheet.tsx`에서 사용 중.

**수정 방향**: `sendErrorAlarm(title, message)`를 `notifications.ts`에 재도입(`ensureChannels()` → `notifee.displayNotification()`, `sendArrivalAlarm` 등과 동일 패턴)하고, 두 파일의 4xx 분기에서 `extractApiErrorMessage`로 뽑은 메시지를 알림으로 발송 후 정리 로직 실행.

---

### 🟢 버그27 — "도착 예정 알림" 커스텀 사운드를 시스템 소리 목록에 등록하는 방식 [버그 아님, 검증 완료 후 보류]

**현재 상태**: `gonow-arrival-expected` 채널은 다른 도착 채널(`arrival-check`, `arrival-complete`)과 동일하게 notifee `sound: 'default'`(시스템 기본음)로 되어 있음. 아래 방식은 실제로 프로토타입까지 만들어 실기기에서 성공 확인했으나, 지금 당장 커스텀 사운드 자체에 대한 우선순위가 낮아 코드는 전부 롤백함. 나중에 이 채널(또는 다른 채널)에 커스텀 사운드를 넣고 싶어지면 이 기록을 참고해서 다시 구현하면 됨 — 삽질했던 부분(특히 MIME 타입)은 이미 다 걸러졌음.

**왜 이게 필요했는지**: notifee의 `sound` 필드는 `'default'`(시스템 기본음) 또는 앱에 번들된 raw 리소스 파일명(`app.json`의 `expo-notifications` 플러그인 `sounds` 배열에 등록) 둘 중 하나만 지정 가능함. 후자로 커스텀 사운드를 넣어도 그 소리는 앱 전용 raw 리소스(`android.resource://.../raw/...`)일 뿐이라, 안드로이드 시스템 설정(설정 앱에서 채널 소리를 직접 바꾸는 화면)의 "소리 선택" 목록에는 뜨지 않음 — 사용자가 나중에 다른 소리로 바꿨다가 다시 원래 커스텀 소리로 되돌리고 싶어도 목록에 없어서 못 고름.

**검증된 해결 방법 (실기기 테스트 완료)**: Expo 로컬 네이티브 모듈(Kotlin)을 만들어서 notifee를 거치지 않고 안드로이드 API를 직접 호출:
1. `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI`에 사운드 파일을 정식으로 "등록"함 — `ContentValues`에 `DISPLAY_NAME`, `MIME_TYPE`, `IS_NOTIFICATION=1`, `RELATIVE_PATH=Environment.DIRECTORY_NOTIFICATIONS`, `IS_PENDING=1`을 채워서 `resolver.insert(...)` → 반환된 URI로 `resolver.openOutputStream(uri)`를 열어 앱 raw 리소스의 바이트를 그대로 복사 → `IS_PENDING=0`으로 마무리. (API 29/`Build.VERSION_CODES.Q` 이상에서만 동작, scoped storage 정책 때문에 필요한 절차)
2. 이렇게 등록된 `content://` URI로 `NotificationChannel.setSound(uri, AudioAttributes...)`를 호출한 뒤 `NotificationManager.createNotificationChannel()`로 채널을 직접 생성(notifee를 거치지 않음 — notifee의 `sound` 타입은 `'default'`/raw 파일명만 받고 임의의 `content://` URI는 못 받음).
3. 이렇게 만든 채널은 안드로이드 시스템 설정의 소리 선택 목록에도 정식으로 나타남(직접 확인함).

**핵심 함정 — 반드시 기억할 것**: `MediaStore.Audio.Media.MIME_TYPE`을 `"audio/wav"`처럼 문자열로 하드코딩하면 실기기에서 `IllegalArgumentException: Unsupported MIME type audio/wav`로 즉시 실패함(`resolver.insert()` 단계). `android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension("wav")`로 동적으로 조회해서 써야 함(안 되면 `"audio/x-wav"`로 폴백). 이 문제를 `adb logcat` + 자체 추가한 `Log.e/w/i`(태그 `GoNowSoundRegistry`) 로그로 진단하는 데 한 라운드를 썼음 — 재구현 시 처음부터 동적 조회로 작성하면 이 삽질을 건너뛸 수 있음.

**롤백된 코드 위치(재구현 시 참고, 프론트 저장소 `GoNow_Fronted` 기준, 전부 삭제됨)**:
- `modules/notification-sound-registry/` — 로컬 Expo 네이티브 모듈 전체(`android/src/main/java/expo/modules/notificationsoundregistry/NotificationSoundRegistryModule.kt`에 위 로직 구현, `registerArrivalExpectedChannel()` / `registerSoundInMediaStore()` / `findExistingSound()` 3개 함수 — 채널ID `gonow-arrival-expected`, raw 리소스명 `arrived` 하드코딩)
- `src/utils/notifications.ts`의 `ensureChannels()` — `arrival-expected` 채널 생성 직전에 네이티브 모듈 호출 후 실패 시 기존 notifee 방식으로 폴백하는 조건부 블록이 있었음(지금은 단순 notifee 호출로 되돌림)
- `app.json`의 `expo-notifications` 플러그인 `sounds` 배열에 있던 `./assets/sounds/arrived.wav` 등록(지금은 제거됨, 실제 파일도 삭제)
- 다른 두 도착 채널(`arrival-complete`, `arrival-check`)에도 필요하면 동일한 패턴을 그대로 확장 적용 가능 — 이번엔 `arrival-expected` 하나만 실험함

**주의(재도입 시)**: `gonow-arrival-expected`/`gonow-arrival-complete`는 스프링 `ArrivalChannel` enum이 채널ID 문자열을 고정값으로 알고 있어서(FCM 발송용), 이 방식을 다시 넣어도 채널ID 자체(`gonow-arrival-expected`)는 절대 바꾸면 안 됨 — 소리만 새로 등록해서 같은 채널ID에 `setSound()`로 붙이는 것까지만 안전함.

---

### 🟡 버그29 — READY 상태 GPS interval 계산이 `departureAlarmTime` 임박을 반영 못 해 DEPARTING 전환이 로컬 1단계 알람보다 늦어짐 [영향 중간]

**관련 저장소**: `gonow-flask`(플라스크), 연관 로직: 스프링 `JourneyService.updateLocation()`(READY 분기), 프론트 로컬 알림(`notifications.ts`)

**파일**: `CounterClockEngine/gps_api/core/optimizer.py`(`calculate_next_interval`, `_cosine_blend_interval`), `CounterClockEngine/gps_api/routes/alarm.py`(`_adaptive_gps_interval`)

**증상**: 실기기 테스트 중 발견 — `departureAlarmTime`(예: 02:34:25)이 되어 프론트의 로컬 1단계 알람("지금 나가야 함")은 정확한 시각에 울렸는데, 서버 DB의 `journey.status`는 한참 지나서도(02:29 시점 기준 이전 폴링에서 이미 `READY`) 계속 `READY`로 남아있어 `DEPARTING`으로 전환되지 않음.

**원인**:
1. `READY → DEPARTING` 전환은 `JourneyService.updateLocation()`이 **`/location` 호출을 받을 때만** `P >= Q`를 체크한다(시간 기반 스케줄러 없음, GPS 폴링 도착에 완전히 반응형). 즉 다음 GPS가 언제 오느냐가 전환 시점을 좌우함.
2. 그런데 폴링 주기(`interval`)를 정하는 `_adaptive_gps_interval`/`calculate_next_interval`은 **목적지까지의 거리·시간(`target_time` 기준)만 반영**하고, `departure_alarm_time`까지 얼마나 남았는지는 계산에 전혀 넣지 않는다.
3. 게다가 출발 전이라 사용자가 정지 상태(`stationary`)면 활동 배율이 `×3.0`(`_ACT_ANCHORS`)까지 걸려서, 다른 요소가 중간 수준이어도 최종 interval이 최대 상한(`INTERVAL_MAX_S=300`)으로 쉽게 클램프됨 — 실측 사례에서 `departureAlarmTime` 불과 5분 전 폴링 응답이 `interval:300`(5분)을 반환해서, 다음 폴링이 도착할 때까지 `DEPARTING` 전환 자체가 최대 5분까지 늦어질 수 있었음.

**수정 방향(논의만 하고 보류, 아직 미착수)**: `departure_alarm_time`까지 남은 시간을 urgency 계산에 새 신호로 추가하거나, `READY` 상태에서 알람 시각이 임박(예: 남은 시간 < 일정 임계값)하면 정지 활동 배율과 무관하게 interval을 짧게(예: 10~30초) 강제하는 로직을 별도로 추가. 플라스크(`gonow-flask`) 리포 수정 필요.

---

### 🟡 버그34 — 자가용(DRIVING) 정지 상태에서 재계산 트리거가 없어 `departureAlarmTime`이 새벽 4시 값에 고정됨 [영향 중간]

**관련 저장소**: 스프링

**파일**: `JourneyService.java`(`updateLocation()`, READY 상태 분기)

**증상**: 자가용 여정에서 사용자가 새벽 4시 READY 전환 이후 실제 출발 시각까지 anchor 지점에서 500m 이상 이동하지 않으면(예: 목표 시각이 오후인데 그때까지 집에서 대기), `departureAlarmTime`이 재계산되지 않고 그대로 유지된다.

**원인**: `updateLocation()`의 재계산 트리거는 `isFirstReceive`(최초 1회)·`isOutOfAnchor`(anchor로부터 500m 이상 이동)·`isNearDest`(목적지 근접) 세 조건의 OR로만 작동하며, 순수 시간 경과만으로 재계산을 유도하는 트리거가 없다(`ReadyTransitionScheduler`는 매일 새벽 4시 SCHEDULED→READY 전환만, `ArrivedTransitionScheduler`는 매분 targetTime 초과 확인만 수행 — 둘 다 `departureAlarmTime` 재계산은 하지 않음). 지하철(TRANSIT)은 시간표 기반이라 계산 시점과 무관하게 값이 동일해서 이 트리거 설계로 충분하지만, 자가용은 계산 시점의 실시간 교통 상황에 따라 값 자체가 달라지므로 같은 트리거로는 부족하다. 대화 중 사용자 질문("새벽 4시 1회 + 500m마다 재계산이면 충분히 안전한가")을 계기로 코드 조사해서 발견함.

**수정 방향(착수 보류 — 버그40 실측 결과 대기 중)**: READY 상태에서 500m 미이동이어도, `departureAlarmTime`까지 남은 시간이 일정 임계값(예: 1~2시간) 이내로 좁혀지면 시간 기반으로 최소 1회 이상 재계산을 강제하는 로직 추가 검토(구현 설계 자체는 끝나서 플랜까지 작성해뒀음 — `JourneyService.updateLocation()` READY 분기에 `isAlarmImminent` 조건 추가, `GeoConstants.ALARM_IMMINENT_THRESHOLD_MINUTES` 상수 신설). 버그29(interval이 `departureAlarmTime` 임박을 반영 못 함)와 연관 있어 보이나 별개 문제로 구분 — 버그29는 "이미 확정된 `departureAlarmTime`에 도달하는 시점"의 폴링 주기 문제고, 이번 건은 "`departureAlarmTime` 값 자체의 정확도"가 시간 경과로 열화되는 문제.

**버그40과의 관계 (재검토함 — 착수 보류 사유)**: 처음엔 "두 수정이 서로 대체하지 않고 함께 적용해야 완전히 해결된다"고 판단했으나, 다시 짚어보니 이건 안 맞을 수 있다. 버그34의 재계산 트리거(1-pass 실시간 API를 여러 번 부르는 것)가 필요한 이유는 애초에 "새벽4시에 부른 1-pass 실시간 API가 그 순간의 교통정보만 반영하기 때문"이다. 그런데 버그40의 2-pass(미래 API)가 실측에서 충분히 정확하다고 확인되면, **새벽4시에 딱 한 번 2-pass로 계산해도 이미 "실제 출발 예정 시각의 예측 교통상황"을 반영한 값이 나온다** — 그럼 버그34가 풀려던 문제 자체가 재계산 트리거 없이도 상당 부분 해결돼서, 버그34는 추가 복잡도만 늘리는 불필요한 작업이 될 수 있다. 반대로 2-pass 실측 오차가 크게 나오면(특히 장시간 리드타임에서), 2-pass만으론 부족하다는 뜻이라 버그34가 여전히 필요하거나 더 중요해진다. **그래서 버그34 구현은 버그40 실측 결과(특히 장시간 리드타임 시나리오)가 나올 때까지 보류한다** — 결과에 따라 버그34를 넣을지, 아예 2-pass만 넣고 버그34는 스킵할지 결정.

---

### 🟡 버그40 — 자가용(DRIVING) `departureAlarmTime` 계산이 항상 계산 시점의 실시간 교통정보만 반영, 실제 출발 시각의 교통상황 예측 안 함 [영향 중간]

**관련 저장소**: 플라스크(`kakao_route.py` — 카카오모빌리티 실시간 소요시간 조회)

**증상**: `departureAlarmTime`을 계산하는 모든 시점(새벽 4시 최초 계산, 500m 이탈 재계산, 버그34 수정 후 추가될 시간 기반 재계산 등 무엇이 트리거하든) 전부 그 계산이 이뤄지는 "순간"의 실시간 교통정보만 사용한다. 자가용은 계산 시점과 실제 출발 시각 사이에 시간차가 크면(예: 새벽 4시에 계산했는데 실제 출발은 오후 퇴근 시간대), 그 사이 교통상황 변화를 전혀 반영하지 못해 계산값이 실제와 어긋난다. 지하철(TRANSIT)은 시간표 기반이라 이 문제 자체가 없음 — DRIVING 전용 문제.

**원인**: 카카오모빌리티 실시간 길찾기(`/v1/directions`)는 "지금 바로 출발한다면"을 가정한 소요시간만 제공한다. 계산 시점과 실제 출발 시각이 다르면 이 값은 미래 예측이 아니라 계산 시점의 스냅샷일 뿐이라, 재계산이 얼마나 자주 일어나든(버그34가 고쳐지든 안 고쳐지든) 이 오차는 남는다.

**수정 방향(미착수) — 카카오모빌리티 `/v1/future/directions`(미래 운행 정보 길찾기) 활용**: 카카오모빌리티가 미래 시각(`departure_time`, 현재 이후 필수) 기준 소요시간 예측을 제공하는 별도 API를 갖고 있음(현재 `kakao_route.py`가 쓰는 실시간 `/v1/directions`와는 다른 엔드포인트). 2-pass로 적용 가능: 1차로 실시간 `/v1/directions`를 호출해 대략적인 소요시간을 구하고, 그 값으로 추정한 출발시각을 `departure_time`으로 삼아 2차로 `/v1/future/directions`를 호출해 그 미래 시각 기준 예측치로 보정.

**주의(실측 검증 필요, 미검증 상태로 도입 금지)**: 2-pass만으로 오차가 항상 충분히 작아진다는 보장은 없음 — 1차 추정이 크게 벗어날수록(예: 장거리 구간) 수렴이 느릴 수 있고, 정체 시작 경계 부근처럼 소요시간이 급격히 변하는 구간에서는 오차가 더 클 수 있음. 카카오모빌리티 API의 실제 응답 지연시간·쿼터 정책도 아직 실측한 바 없음. 도입 시 다양한 거리·시간대 조합으로 실제 API를 여러 번 호출해 수렴 여부와 오차 범위를 실측 검증한 뒤 pass 횟수/임계값을 확정할 것 — "2-pass면 충분하다"를 검증 없이 전제하지 말 것.

**버그34와의 관계 (재검토함)**: 처음엔 두 수정이 독립적이라 함께 적용해야 한다고 봤으나, 다시 보면 이 실측 결과가 좋게 나올 경우 버그34 자체가 불필요해질 수 있다 — 상세 근거는 버그34 항목의 "버그40과의 관계" 참고. **버그34 착수 여부는 이 실측 결과를 보고 결정한다.**

**실측 진행 현황 (2026-08-08 시작)**: `gonow-flask`의 `verification_geofencing/`에 실측 도구 일체(`future_api.http`, `record.sh`, `verify.sh`, 현황 문서 `future_api_validation_status.md`) 준비 완료. 판교↔강남(중거리)/신논현↔강남(초단거리)/수원↔강남(장거리)/삼성↔강남(단거리) 조합으로 리드타임 0.3h~44.5h 6개 시나리오 진행 중.
- STEP0(사전 점검, 리드타임 3분): 예측 1313초 vs 실제 1281초, 오차 +2.5% — future API 기본 신뢰성 확인
- STEP1(대조군, 리드타임 20분): 예측 1284초 vs 실제 1265초, 오차 +1.5% — 검증 완료
- STEP2(퇴근러시, 20.5h) / STEP3(출근러시, 10.5h) / STEP4(한산한낮, 16h) / STEP5(심야, 4h) / STEP6(극단, 44.5h): 목표 시각 도래 대기 중 (진행 상황은 `future_api_validation_status.md` 참고, 세션이 끊겨도 그 폴더의 `verify.sh` 재실행으로 이어서 확인 가능)

---

## 인과관계 요약

```
현재 남은 실질적 문제:

버그3 (백그라운드 30초 고정)          → 배터리 비효율, 기능 영향 없음, 후순위
버그8 (포그라운드 GPS 중복 찌르기) → 배터리 낭비, 기능 영향 없음, 후순위
버그17 (자정~새벽 4시 날짜 경계)     → 정책 결정 후 수정, 현행 유지도 가능
버그21 (인증 없는 테스트 스케줄러 엔드포인트) → 의도적으로 남겨둠, 테스트 필요 없어지면 파일 삭제
버그22 (막차 첫 계산이 새벽 1~4시대에 걸리면 이미 지난 시각 그대로 응답) → 재현 조건 희귀해서 수정 시도했다가 롤백, 버그23과 맞물림
버그23 (GPS 획득/권한 실패 시 무알림) → 영향 중간, 미사용 앱 권한 자동 해제로 실제 발생 가능성 있음
버그25 (/location 폴링 4xx 시 무알림) → 버그22 조사 중 발견, 수정했다가 롤백(단 메시지 파싱 유틸은 시나리오 A용으로 유지)
버그27 (도착 예정 알림 커스텀 사운드 시스템 목록 등록) → 버그 아님. 프로토타입으로 검증까지 마쳤으나 우선순위 낮아 코드 롤백, 레시피만 기록해둠
버그29 (READY interval 계산이 departureAlarmTime 임박 미반영) → 실기기 테스트로 발견, 최대 5분까지 DEPARTING 전환 지연 가능. 플라스크 수정 필요, 착수 전
버그34 (자가용 정지 시 재계산 트리거 부재로 departureAlarmTime이 새벽 4시 값에 고정) → 대화 중 질문 계기로 코드 조사해서 발견, 구현 설계 완료. **착수는 버그40 실측 결과 나올 때까지 보류** (2-pass가 충분히 정확하면 불필요해질 수 있음)
버그40 (자가용 departureAlarmTime 계산이 매번 계산 시점 실시간 정보만 반영, 미래 예측 안 함) → 버그34 조사 중 별도 문제로 분리됨. 실측 검증 진행 중(`gonow-flask/verification_geofencing/`), 결과 나오면 버그34 착수 여부 + 2-pass 도입 여부 결정

해결된 버그(버그1, 2, 4~7, 9, 10-A, 10-B, 11, 12, 13, 14, 15, 16, 18, 19, 20, 24, 26, 28, 30, 35, 36, 37, 38, 39, 임시 interval 변경, Picker New Architecture 네이티브 크래시, 출발 알람 채널 사전 생성/단계별 커스텀 사운드, 배터리 최적화 상태 확인 네이티브 모듈, 단계별 알람 중복 발송/재발송 등)는 docs/history/resolved-bugs.md 참고.
```
