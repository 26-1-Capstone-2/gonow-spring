# GPS 폴링 버그 목록

마지막 업데이트: 2026-08-17

해결된 버그는 `docs/history/resolved-bugs.md` 참고.

---

## 미수정 버그 목록

> 중요도 높은 순으로 정렬

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

### 🔴 버그41 — 막차 모드 귀가 여정, 목적지(집) 700m 이내에서 최초 계산 시 새벽 4시에 즉시 DEPARTING/NEARDEST로 오작동 [영향 높음, 재현 조건 흔함]

→ 아래 버그41 상세 참고

---

### 🟡 버그45 — 반복 알람이 ARRIVED를 지나도 FGS(포그라운드 서비스)를 유지하도록 `AlarmRunner` 생명주기 재설계 필요 [영향 낮음, 반복 알람 전용]

→ 아래 버그45 상세 참고

---

## 미수정 버그 상세

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

### 🔴 버그41 — 막차 모드 귀가 여정, 목적지(집) 700m 이내에서 최초 계산 시 새벽 4시에 즉시 DEPARTING/NEARDEST로 오작동 [영향 높음, 재현 조건 흔함]

**관련 저장소**: 스프링(`JourneyService.java`) + 플라스크(`alarm.py`)

**파일**: `gonow-flask`의 `gps_api/routes/alarm.py`(`is_last_mode` 분기, 700m 미만 도보 폴백), `gonow`의 `JourneyService.java`(`updateLocation()`, READY 상태 분기 `isNearDest`/`isPastAlarmTime` 체크)

**증상**: 귀가 여정(막차 모드)의 목적지는 항상 집이다. 새벽 4시 READY 전환 직후 앱은(완전히 종료된 상태여도 헤드리스 경로로) 즉시 GPS 폴링을 시작하는데, 이 시점엔 사용자가 보통 아직 집에서 자고 있어 **현재위치≈목적지(집)**가 된다. 이 상태에서 `target_time`이 이 여정에 대해 한 번도 확정된 적 없다면(최초 계산), 다음 둘 중 하나가 새벽 4시에 즉시 벌어진다:
- 목적지 100~700m: 도보 폴백 계산이 "지금 당장 출발"을 기준으로 계산되어 `departure_alarm_time`이 계산되자마자 과거가 됨 → `isPastAlarmTime` true → **새벽 4시에 즉시 READY→DEPARTING**.
- 목적지 100m 이내: 스프링의 `isNearDest` 체크는 플라스크 응답과 무관하게 실제 GPS 거리만으로 즉시 전환 → **새벽 4시에 즉시 READY→NEARDEST**, 곧이어 `ArrivedTransitionScheduler`가 (마찬가지로 잘못 계산된, 몇 분 뒤인) `targetTime` 초과로 자동 ARRIVED 처리.

두 경로 모두 사용자가 자는 동안 여정 상태머신이 새벽에 이미 완료(DEPARTING→MOVING 또는 ARRIVED) 처리되어, **정작 그날 밤 진짜 막차 시각 계산이 일어나지 않는다** — 막차 모드의 가장 흔한 실사용 패턴(전날 밤 집에서 자고, 낮에 나갔다가, 그날 밤 막차로 귀가)에서 사실상 매번 재현될 것으로 추정된다(드문 엣지케이스가 아님).

**원인**: `walk_fallback()`(도보 폴백)이 `target_time=None`일 때 "계산 시점(=지금)"을 기준으로 출발/도착을 계산하기 때문. 지하철 막차 이진탐색(700m 이상 분기)은 시간표 기반이라 계산 시점과 무관하게 항상 미래 시각이 나오는 것과 대조적 — **버그는 정확히 700m 미만(도보 폴백)에서만 발생**하며, 700m 이상이면 문제없다(실측/코드로 확인 완료).

**한 번 확정되면 재발 안 함**: `target_time`이 한 번이라도(즉 최초 계산이 700m 이상에서 이뤄지면) 진짜 미래값으로 확정되고 나면, 이후 하루 종일 700m 이내를 들락거려도 이미 있는 `target_time` 기준으로 정상 역산되므로 안전하다(이미 이 재계산 분기는 별도로 수정 완료 — `walk_fallback()` 도보 API 붙이는 작업 중 발견해서 고침). 문제는 **"target_time이 null인 채로 최초 계산이 700m 이내에서 발생하는" 바로 그 순간에만** 국한된다.

**수정 방향(설계 논의 완료, 미착수)**:
- 플라스크: `is_last_mode` + `target_time is None` + 700m 미만 분기에서 "지금+도보시간"을 계산해서 반환하는 대신, `departure_alarm_time`/`target_time`을 **`null`로 응답**하도록 변경. 스프링이 이미 null을 안전하게 처리하는 기존 로직을 그대로 활용(`isPastAlarmTime(null)`은 이미 `false`를 반환하도록 구현돼 있고, `targetTime`도 응답값이 null이면 갱신을 건너뛰도록 이미 조건이 있음 — 이 두 곳은 스프링 코드 변경 불필요).
- 스프링: `JourneyService.updateLocation()`의 READY 분기, `isNearDest` 체크에 `journey.isLastMode() && journey.getTargetTime() == null`이면 이 체크 자체를 건너뛰는 가드 추가 — 100m 이내에서도 target_time 미확정 상태면 NEARDEST로 넘어가지 않도록.
- **검토했다가 기각한 대안**: "700m 밖의 임의 좌표로 출발지를 치환해서 계산"(사용자 제안) — (1) `isNearDest`는 플라스크 응답과 무관하게 스프링이 실제 GPS 거리로만 판단하므로 이 방법으로는 100m 이내 문제를 못 막고, 여전히 스프링 가드가 별도로 필요해서 작업량이 줄지 않음. (2) 사용자가 그날 정말 500m 밖으로 안 나가면(재택 등) 이 가짜 계산값이 그대로 확정되어 엉뚱한 시각에 DEPARTING 알림이 뜨는 새로운 오작동 위험이 생김. (3) 가짜 좌표를 어느 방향으로 잡을지도 불명확(강/막힌 구역 등으로 잡히면 ODsay가 이상한 경로를 줄 수 있음).

**지오펜싱 도입 계획과의 관계 (2026-08-11 추가, 2026-08-17 재검토)**: 애초엔 "지오펜스 등록 시점에 이미 목적지 근처면 등록 자체가 무의미해진다"는 우려로 이 버그를 지오펜싱과 통합 설계해야 할 수도 있다고 봤으나, 실제 구현 결과 이 우려는 발생하지 않는 것으로 확인됐다(READY 지오펜스는 항상 직전 `/location` 응답이 READY임을 전제로만 등록되므로 — `docs/history/geofencing-migration-plan.md` "새로 설계해야 하는 것들 3" 참고). **다만 이건 지오펜싱 쪽 상호작용만 해소된 것이고, 버그41 본체(플라스크 최초 계산 로직)는 독립적으로 여전히 미착수 상태.**

---

### 🟡 버그45 — 반복 알람이 ARRIVED를 지나도 FGS(포그라운드 서비스)를 유지하도록 `AlarmRunner` 생명주기 재설계 필요 [영향 낮음, 반복 알람 전용]

**관련 저장소**: 프론트(`GoNow_Fronted`)

**파일**: `src/services/alarmService.ts`(`AlarmRunner`), `src/tasks/backgroundLocationTask.ts`

**배경**: 지오펜싱 마이그레이션(`docs/history/geofencing-migration-plan.md`, "FGS 생명주기 정책" 섹션) 중 확정된 정책은 "알람이 하나라도 있으면 FGS를 상시 유지, 완전히 없어지면만 끈다"이다 — 안드로이드 12+에서는 백그라운드 상태에서 FGS를 새로 켤 방법이 없기 때문에(`ForegroundServiceStartNotAllowedException`, 실기기로 가설 기각까지 확인됨), 한 번이라도 FGS가 꺼지면 다음 새벽 4시 같은 백그라운드 시점에 다시 켤 방법이 없다.

**증상**: ARRIVED 도달 시 `handlePersonalStatus`/`handleGroupStatus`가 반복 여부와 무관하게 무조건 `AlarmRunner.stop()`을 호출해 runner가 사라진다. 그런데 반복 여정은 서버 스케줄러가 다음 해당 요일 새벽 4시에 이 여정을 다시 `READY`로 되돌리므로(`CLAUDE.md` 스케줄러 섹션), 이 runner가 마지막 하나였다면 그 사이 FGS가 꺼져 있다가 다음 새벽 4시 전환 때 또 "백그라운드에서 FGS 못 켬" 문제를 겪는다 — **매 회차마다 반복될 수 있음.**

**수정 방향(미착수)**: `AlarmRunner`의 생명주기 자체를 손봐야 한다 — ARRIVED에서도 반복 여정이면 runner를 완전히 지우지 않고 "다음 회차 대기" 상태로 남기는 로직 필요, `AlarmTarget`에 반복 여부/`repeatDays` 전달도 추가해야 함. 범위가 있는 별도 작업이라 지오펜싱 마이그레이션 착수 당시 의도적으로 미룸.

---

## 인과관계 요약

```
현재 남은 실질적 문제:

버그17 (자정~새벽 4시 날짜 경계)     → 정책 결정 후 수정, 현행 유지도 가능
버그21 (인증 없는 테스트 스케줄러 엔드포인트) → 의도적으로 남겨둠, 테스트 필요 없어지면 파일 삭제
버그22 (막차 첫 계산이 새벽 1~4시대에 걸리면 이미 지난 시각 그대로 응답) → 재현 조건 희귀해서 수정 시도했다가 롤백, 버그23과 맞물림
버그23 (GPS 획득/권한 실패 시 무알림) → 영향 중간, 미사용 앱 권한 자동 해제로 실제 발생 가능성 있음
버그25 (/location 폴링 4xx 시 무알림) → 버그22 조사 중 발견, 수정했다가 롤백(단 메시지 파싱 유틸은 시나리오 A용으로 유지)
버그27 (도착 예정 알림 커스텀 사운드 시스템 목록 등록) → 버그 아님. 프로토타입으로 검증까지 마쳤으나 우선순위 낮아 코드 롤백, 레시피만 기록해둠
버그41 (막차 모드 귀가 여정, 목적지 700m 이내에서 최초 계산 시 새벽4시에 즉시 DEPARTING/NEARDEST 오작동) → 코드로 원인 확정·수정 설계까지 완료. 애초 우려했던 지오펜싱과의 통합 설계 필요성은 실제 구현 결과 발생하지 않는 것으로 확인됨(READY 지오펜스 등록 시점 전제 덕분 — 위 버그41 상세 참고). 지오펜싱과 독립적으로 여전히 미착수, 착수는 보류
버그45 (반복 알람 ARRIVED 이후 FGS 미유지) → 지오펜싱 마이그레이션(READY 완료, 2026-08-17) 중 확정된 정책("알람이 있는 한 FGS 상시 유지")의 남은 구멍. 반복 여정에서만 재현되는 낮은 우선순위 TODO, 착수는 보류

해결된 버그(버그1, 2, 3, 4~7, 8, 9, 10-A, 10-B, 11, 12, 13, 14, 15, 16, 18, 19, 20, 24, 26, 28, 29(안드로이드 전용 서비스 기준 지오펜싱 마이그레이션으로 해소), 30, 34(별도 수정 없이 버그40으로 실질 해소), 35, 36, 37, 38, 39, 40(카카오모빌리티 future API 2-pass 도입), 42(코드 롤백으로 해당없음 처리), 43(READY 분기 플라스크 중복 호출 가드 추가), 44(반복 여정 앵커/출발시각 스테일 값 리셋), 임시 interval 변경, Picker New Architecture 네이티브 크래시, 출발 알람 채널 사전 생성/단계별 커스텀 사운드, 배터리 최적화 상태 확인 네이티브 모듈, 단계별 알람 중복 발송/재발송, 지오펜싱 마이그레이션(READY/DEPARTING/MOVING/NEARDEST 4개 상태, 2026-08-17 완료) 등)는 docs/history/resolved-bugs.md 참고. (버그3/8은 2026-08-10 한 차례 해결 후 2026-08-11 롤백, 2026-08-14 재구현으로 최종 해결됨)
```
