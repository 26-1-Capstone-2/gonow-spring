# GPS 폴링 버그 목록

마지막 업데이트: 2026-08-11

해결된 버그는 `docs/history/resolved-bugs.md` 참고.

---

## 미수정 버그 목록

> 중요도 높은 순으로 정렬

---

### 🟡 버그3/버그8 — 백그라운드 GPS polling interval 30초 고정 + 포그라운드 중복 호출 [영향 낮음, 지오펜싱 도입 시 함께 재작업 예정]

→ 아래 버그3/버그8 상세 참고

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

### 🔴 버그41 — 막차 모드 귀가 여정, 목적지(집) 700m 이내에서 최초 계산 시 새벽 4시에 즉시 DEPARTING/NEARDEST로 오작동 [영향 높음, 재현 조건 흔함]

→ 아래 버그41 상세 참고

---

### 🟡 버그43 — READY 상태에서 앵커 500m 이탈과 출발 알람 시각 도달이 같은 요청에 겹치면 플라스크가 두 번 호출됨 [영향 낮음, 순수 비효율]

→ 아래 버그43 상세 참고

---

## 미수정 버그 상세

---

### 🟡 버그3/버그8 — 백그라운드 GPS polling interval 30초 고정 + 포그라운드 중복 호출 [영향 낮음, 지오펜싱 도입 시 함께 재작업 예정]

**파일**: `src/tasks/backgroundLocationTask.ts`(`startBackgroundLocationUpdates()`), `src/services/alarmService.ts`(`AlarmRunner`)

**증상**: OS 레벨 GPS 구독의 `timeInterval`이 30초로 하드코딩돼 있어, 서버가 알려준 실제 필요 주기(최대 300초)를 무시하고 항상 30초마다 GPS 하드웨어를 깨움(버그3). 포그라운드에서도 이 구독이 그대로 살아있어 `alarmService`의 정밀 폴링과 GPS 하드웨어가 중복 발화함(버그8, 단 `/location` 네트워크 호출 자체는 중복 안 됨 — 태스크 핸들러가 포그라운드면 스킵함).

**2026-08-10에 한 차례 수정·실기기 검증까지 완료했다가, 2026-08-11에 전면 롤백함 — 롤백 사유**:
1. **버그29를 오히려 악화시킬 수 있음(재검토로 새로 발견)**: 기존 30초 고정 덕분에, 서버 interval 계산값이 크게 나와도(예: READY 상태 300초) 백그라운드에서는 의도치 않게 항상 자주(30초) 확인되고 있었음 — 즉 버그29(`departureAlarmTime` 임박 반영 못 함) 증상이 백그라운드에서는 30초 고정 "버그" 덕분에 오히려 완화돼 있었던 셈. 이걸 "서버가 알려준 실제 값"으로 바꾸면, 백그라운드에서 최대 300초까지 상태 전환 감지가 늦어질 수 있어 버그29의 영향 범위가 넓어짐 — 배터리 효율과 반응 정확도 사이의 트레이드오프였는데, 정확도를 우선하기로 결정.
2. **지오펜싱 도입 시 이 파일 자체가 재작업 대상**: READY/DEPARTING/NEARDEST가 지오펜싱으로 바뀌면 `backgroundLocationTask.ts`는 "모든 상태의 폴링"이 아니라 "**MOVING 상태만**의 폴링"을 담당하도록 범위가 좁아져야 함(상세 설계는 `docs/planning/geofencing-migration-plan.md`). MOVING은 interval이 원래 짧게(30~120초) 계산돼서 위 1번 트레이드오프의 심각도가 훨씬 낮음 — 즉 "MOVING 전용으로 범위를 좁혀서" 다시 넣는 게 최종 형태이므로, 지금 전체 상태에 적용된 버전을 유지할 이유가 없어 롤백.

**수정된 코드였던 내용(재구현 시 참고, `docs/history/resolved-bugs.md`의 "버그3/버그8" 항목에 상세 기록)**: `getMinDesiredIntervalMs()` 헬퍼(활성 journey/appointment의 저장된 interval 중 최솟값 계산), `applyNewInterval()`(interval이 바뀌는 즉시, 아직 포그라운드인 시점에 백그라운드 구독 재등록). 실기기 검증까지 끝난 코드라 지오펜싱 작업 시 MOVING 전용으로 범위만 좁혀서 재사용 가능.

**수정 방향(보류 — 지오펜싱 도입과 함께 진행)**: 별도로 착수하지 않고, `docs/planning/geofencing-migration-plan.md`의 구현 범위에 "MOVING 전용 interval 동적화"로 포함시켜 함께 설계·구현.

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

**지오펜싱 도입 계획과의 관계 (2026-08-11 추가)**: READY가 지오펜싱 기반으로 바뀌면(`docs/planning/geofencing-migration-plan.md` 참고) 이 버그의 원인이었던 "폴링 주기 계산"이라는 개념 자체가 READY 상태에서 없어진다. 다만 이 버그가 다루는 `P >= Q`(출발 알람 시각 도달) 감지는 순수 시간 기반이라 지오펜스 이벤트만으로는 여전히 못 잡는다 — 지오펜싱 설계 시 이 시간 기반 체크를 어떻게 트리거할지 별도로 정해야 한다(위 문서의 "미결정 사항" 참고). 즉 이 버그를 지금 이대로 고치기보다, 지오펜싱 설계에 이 트리거를 포함시키는 쪽으로 통합하는 게 합리적.

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

**지오펜싱 도입 계획과의 관계 (2026-08-11 추가)**: 위 수정 방향은 "새벽 4시에 폴링으로 첫 `/location` 호출이 들어온다"는 전제로 짜여 있다. READY가 지오펜싱 기반으로 바뀌면(`docs/planning/geofencing-migration-plan.md` 참고) 첫 위치 확인이 "폴링"이 아니라 "지오펜스 등록 전 앵커 확정용 단발성 GPS 획득"으로 바뀌는데, 이미 그 시점에 목적지 500m/100m 이내라면 지오펜스를 등록하는 의미 자체가 없어진다(등록해도 이미 안에 있어 EXIT 이벤트가 안 옴). 즉 이 버그는 지오펜싱 설계와 맞물려 함께 재설계해야 할 가능성이 높음 — 지오펜싱 착수 전 이 버그를 독립적으로 먼저 고칠지, 지오펜싱과 통합 설계할지는 미결정(계획 문서의 "미결정 사항" 참고).

---

### 🟡 버그43 — READY 상태에서 앵커 500m 이탈과 출발 알람 시각 도달이 같은 요청에 겹치면 플라스크가 두 번 호출됨 [영향 낮음, 순수 비효율]

**파일**: `JourneyService.java`(`updateLocation()` READY 분기), `ParticipantService.java`(동일 구조)

**증상**: READY 분기는 이렇게 짜여 있다.
```java
if (isNearDest || isFirstReceive || isOutOfAnchor) {   // 500m 이탈 등
    journey.updateCurrentPoint(newPoint);
    flaskResponse = callFlaskAndUpdate(...);             // 1차 호출, departureAlarmTime 갱신
}
if (isNearDest) {
    journey.updateStatus(NEARDEST);
} else if (isPastAlarmTime(journey.getDepartureAlarmTime())) {  // 방금 갱신된 값으로 재판정
    journey.updateStatus(DEPARTING);
    flaskResponse = callFlaskAndUpdate(...);             // 2차 호출
}
```
`isOutOfAnchor`(앵커 500m 이탈)가 참이라 1차 호출이 발생했는데, 그 호출이 갱신한 새 `departureAlarmTime`이 하필 이미 지난 시각이면(`isPastAlarmTime` 참) 바로 이어서 DEPARTING 분기로 들어가 **같은 요청 안에서 플라스크를 또 호출**한다. 결과값 자체는 정확하지만(둘 다 같은 입력으로 같은 계산을 함) 카카오/ODsay API를 불필요하게 두 번 때린다. DRIVING 2-pass(버그40) 로직까지 겹치면 최악의 경우 한 요청 안에서 카카오 API를 4번(1차 realtime+future, 2차 realtime+future)까지 호출할 수 있다.

**발생 조건**: 앵커가 500m 이탈한 그 순간에 마침 출발 알람 시각도 지나 있는 경우 — 자주는 아니지만, 사용자가 실제로 차를 몰고 출발하는 시점이 딱 이 순간이라면 꽤 현실적으로 일어날 수 있다. READY는 이 조건이 한 번 충족되면 바로 DEPARTING으로 넘어가므로, 한 여정당 최대 1회만 발생 가능한 엣지케이스다.

**발견 경위**: 지오펜싱 도입 논의(`docs/planning/geofencing-migration-plan.md`) 중 READY 분기 코드를 재검토하면서 발견(2026-08-11). 지오펜싱과는 무관한 독립적인 기존 버그.

**수정 방향**: 1차 호출 결과로 이미 `isPastAlarmTime`이 참이 되면 2차 호출을 생략하고 1차 `flaskResponse`를 그대로 재사용하도록 분기 정리.

---

## 인과관계 요약

```
현재 남은 실질적 문제:

버그3/버그8 (백그라운드 GPS interval 30초 고정 + 포그라운드 중복 호출) → 2026-08-10 한 차례 수정·검증 완료했으나, 버그29 악화 위험 + 지오펜싱 도입 시 재작업 필요성 때문에 2026-08-11 전면 롤백. geofencing-migration-plan.md에서 "MOVING 전용"으로 범위 좁혀서 재구현 예정
버그17 (자정~새벽 4시 날짜 경계)     → 정책 결정 후 수정, 현행 유지도 가능
버그21 (인증 없는 테스트 스케줄러 엔드포인트) → 의도적으로 남겨둠, 테스트 필요 없어지면 파일 삭제
버그22 (막차 첫 계산이 새벽 1~4시대에 걸리면 이미 지난 시각 그대로 응답) → 재현 조건 희귀해서 수정 시도했다가 롤백, 버그23과 맞물림
버그23 (GPS 획득/권한 실패 시 무알림) → 영향 중간, 미사용 앱 권한 자동 해제로 실제 발생 가능성 있음
버그25 (/location 폴링 4xx 시 무알림) → 버그22 조사 중 발견, 수정했다가 롤백(단 메시지 파싱 유틸은 시나리오 A용으로 유지)
버그27 (도착 예정 알림 커스텀 사운드 시스템 목록 등록) → 버그 아님. 프로토타입으로 검증까지 마쳤으나 우선순위 낮아 코드 롤백, 레시피만 기록해둠
버그29 (READY interval 계산이 departureAlarmTime 임박 미반영) → 실기기 테스트로 발견, 최대 5분까지 DEPARTING 전환 지연 가능. 지오펜싱 도입 시 이 트리거를 `DepartingTransitionScheduler`(신규 서버 스케줄러)로 해결하는 것으로 설계 확정 — geofencing-migration-plan.md 참고, 별도 착수 없이 지오펜싱 작업에 흡수
버그41 (막차 모드 귀가 여정, 목적지 700m 이내에서 최초 계산 시 새벽4시에 즉시 DEPARTING/NEARDEST 오작동) → 코드로 원인 확정·수정 설계까지 완료했으나, 지오펜싱 도입 시 "새벽4시 첫 위치 확인" 흐름 자체가 바뀌므로 지오펜싱과 함께 재설계 필요. 착수는 보류, 추후 진행
버그43 (READY 상태 앵커 500m 이탈 + 알람시각 도달이 겹치면 플라스크 중복 호출) → 지오펜싱 논의 중 코드 재검토로 발견한 독립적 비효율. 영향 낮고 수정 방향은 간단(2차 호출 생략) — 우선순위 낮아 미착수
지오펜싱 도입 (신규 계획, 버그 아님) → 버그3 완전 해결(백그라운드에서 interval 실시간 재조정) 시도 중 위험한 코드(FGS 재시작, 과거 크래시 전례 있음)가 필요하다는 걸 확인, READY/DEPARTING/NEARDEST를 지오펜싱으로 대체하면 이 문제 자체가 구조적으로 사라진다는 결론. 상세 설계는 docs/planning/geofencing-migration-plan.md 참고 — 착수 전 버그41과 통합 설계 필요

해결된 버그(버그1, 2, 4~7, 9, 10-A, 10-B, 11, 12, 13, 14, 15, 16, 18, 19, 20, 24, 26, 28, 30, 34(별도 수정 없이 버그40으로 실질 해소), 35, 36, 37, 38, 39, 40(카카오모빌리티 future API 2-pass 도입), 42(코드 롤백으로 해당없음 처리), 임시 interval 변경, Picker New Architecture 네이티브 크래시, 출발 알람 채널 사전 생성/단계별 커스텀 사운드, 배터리 최적화 상태 확인 네이티브 모듈, 단계별 알람 중복 발송/재발송 등)는 docs/history/resolved-bugs.md 참고. (버그3/8은 한 차례 해결 후 다시 롤백되어 이 목록에서 제외 — 위 "현재 남은 실질적 문제" 참고)
```
