# GPS 폴링 버그 목록

마지막 업데이트: 2026-08-03

해결된 버그는 `docs/history/resolved-bugs.md` 참고.

---

## 미수정 버그 목록

> 중요도 높은 순으로 정렬

---

### 🟡 버그14 — 추방/방 삭제 시 OS 등록 단계별 알람 미취소 [영향 낮음]

→ 아래 버그14 상세 참고

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

### 🟡 버그24 — 플라스크 4xx/5xx 응답이 구분 없이 500으로 처리됨 [영향 낮음]

→ 아래 버그24 상세 참고

---

### 🟡 버그25 — `/location` 폴링 4xx 시 프론트에서 조용히 폴링만 중단, 사용자 알림 없음 [영향 낮음]

→ 아래 버그25 상세 참고

---

### 🟢 버그27 — "도착 예정 알림" 커스텀 사운드를 시스템 소리 목록에 등록하는 방식 [버그 아님, 검증 완료 후 보류]

→ 아래 버그27 상세 참고

---

### 🟡 버그28 — 백그라운드 위치 추적 알림에 존재하지 않는 `notificationChannelId` 옵션 사용 [영향 미확인, 실기기 검증 필요]

→ 아래 버그28 상세 참고

---

### 🟡 버그29 — READY 상태 GPS interval 계산이 `departureAlarmTime` 임박을 반영 못 해 DEPARTING 전환이 로컬 1단계 알람보다 늦어짐 [영향 중간]

→ 아래 버그29 상세 참고

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

### 🟡 버그14 — 추방/방 삭제 시 OS 등록 단계별 알람 미취소 [영향 낮음]

**원인**:
- 방장이 참가자 추방 또는 방 삭제 시 피해 참가자는 서버 404로 폴링만 중단됨
- OS에 이미 등록된 2~4단계 단계별 알람은 취소되지 않고 예정 시각에 계속 울림

**수정 방향**: FCM으로 추방/삭제 사실을 피해 참가자에게 알리고, 프론트에서 `cancelRemainingStages()` 호출. 또는 `notifee.cancelAllTriggerNotifications()`로 전체 취소.

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

### 🟡 버그24 — 플라스크 4xx/5xx 응답이 구분 없이 500으로 처리됨 [영향 낮음]

**파일**: `src/main/java/com/timemate/gonow/global/exception/GlobalExceptionHandler.java`

**증상**: 플라스크가 정상적으로 응답했지만 계산을 거부하는 경우(예: 막차 자체가 없는 지역 — 기존부터 있던 404, 라우팅 API 실패 502 등), `RestClientResponseException`(`HttpClientErrorException`/`HttpServerErrorException`)을 잡는 핸들러가 없어 catch-all `Exception → 500`으로 처리됨. 프론트 입장에서 "계산 불가"와 "서버 진짜 고장" 둘 다 구분 안 되는 500으로 보임.

**경위**: 버그22(막차 이미 지남) 조사 중 발견해서 한 차례 고쳤다가(`RestClientResponseException` 핸들러 → 400 + 고정 문구), 원래 목적(새벽 1~4시 막차 생성 차단)과 무관한 범위라 버그22와 함께 되돌림.

**수정 방향**: `RestClientResponseException` 핸들러 추가 → 400 + `ApiResult.fail("경로를 계산할 수 없습니다. 잠시 후 다시 시도해주세요.")`. 재도입 시 고정 문구가 상황(예: "이미 지남"처럼 재시도해도 의미 없는 경우)과 안 맞을 수 있음— 플라스크의 실제 메시지를 얼마나 그대로 전달할지 같이 고민 필요.

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

### 🟡 버그28 — 백그라운드 위치 추적 알림에 존재하지 않는 `notificationChannelId` 옵션 사용 [영향 미확인, 실기기 검증 필요]

**관련 저장소**: `GoNow_Fronted`(프론트)

**파일**: `src/tasks/backgroundLocationTask.ts:61` (`startBackgroundLocationUpdates()`)

**증상**:
```ts
await Location.startLocationUpdatesAsync(BACKGROUND_LOCATION_TASK, {
  accuracy: Location.Accuracy.High,
  timeInterval: 30000,
  distanceInterval: 0,
  foregroundService: {
    notificationTitle: 'GoNow 알람 실행 중',
    notificationBody: '출발 시간을 모니터링하고 있어요.',
    notificationColor: '#4CAF50',
    notificationChannelId: CHANNEL_SILENT,   // ← 무음 채널로 보내려는 의도
  },
});
```
설치된 `expo-location`(`~19.0.8`) 타입 정의(`node_modules/expo-location/build/Location.types.d.ts:180-197`)를 직접 확인한 결과, `LocationTaskServiceOptions`가 실제로 지원하는 필드는 `notificationTitle`/`notificationBody`/`notificationColor`/`killServiceOnDestroy` 4개뿐이고 `notificationChannelId`는 이 버전 API에 존재하지 않는다(타입 정의가 오래돼서가 아니라 애초에 없는 옵션).

**원인 추정**: 백그라운드 GPS 추적 중 상단바에 상시로 뜨는 "GoNow 알람 실행 중" 알림을 `CHANNEL_SILENT`(무음 채널)로 보내려는 의도였을 것으로 보이나, 이 프로퍼티가 네이티브 모듈에 전달될 때 조용히 무시될 가능성이 높음 — 즉 해당 알림이 무음 채널이 아니라 안드로이드 기본 채널(소리/진동 있을 수 있음)로 뜨고 있을 가능성이 있음.

**미확인 사항 (실기기 검증 필요)**: 타입 정의상 없는 필드라는 것만 코드로 확인했고, 실제 런타임에서 이 알림이 소리/진동을 내는지는 아직 실기기로 검증하지 않음.

**수정 방향**: (1) 실기기에서 백그라운드 추적 시작 시 상단바 알림이 무음인지 직접 확인. (2) 무음이 아니라면, `expo-location`이 공식 지원하는 방식으로 채널을 지정하거나(문서 확인 필요), 애초에 이 옵션 없이도 무음으로 뜨게 만드는 다른 방법(예: 안드로이드 알림 채널을 앱 시작 시 미리 무음으로 생성해두고 OS가 그 채널을 재사용하게 하는 방식)을 검토.

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

## 인과관계 요약

```
현재 남은 실질적 문제:

버그1 (FCM 포그라운드 타이밍 gap)    → 간헐적 /location 1회 추가, 기능 영향 없음, 완전 방어 불가
버그2 (앱 재실행 폴링 복구)           → 로그인 전까지 폴링 없음, 로그인 후 즉시 복구되므로 영향 낮음
버그3 (백그라운드 30초 고정)          → 배터리 비효율, 기능 영향 없음, 후순위
버그9 (3초 중복 active 상단바 깜빡임) → 기능 영향 없음, 후순위
버그8 (포그라운드 GPS 중복 찌르기) → 배터리 낭비, 기능 영향 없음, 후순위
버그14 (추방/방 삭제 시 단계별 알람 미취소) → 낮음, FCM 연동 필요(그룹 참가자 동기화 인프라는 구축됨, 로컬 알람 취소 연동만 남음)
버그17 (자정~새벽 4시 날짜 경계)     → 정책 결정 후 수정, 현행 유지도 가능
버그21 (인증 없는 테스트 스케줄러 엔드포인트) → 의도적으로 남겨둠, 테스트 필요 없어지면 파일 삭제
버그22 (막차 첫 계산이 새벽 1~4시대에 걸리면 이미 지난 시각 그대로 응답) → 재현 조건 희귀해서 수정 시도했다가 롤백, 버그23과 맞물림
버그23 (GPS 획득/권한 실패 시 무알림) → 영향 중간, 미사용 앱 권한 자동 해제로 실제 발생 가능성 있음
버그24 (플라스크 4xx/5xx가 500으로 뭉개짐) → 버그22 조사 중 발견, 수정했다가 원래 목적과 무관해 롤백
버그25 (/location 폴링 4xx 시 무알림) → 버그22 조사 중 발견, 수정했다가 롤백(단 메시지 파싱 유틸은 시나리오 A용으로 유지)
버그27 (도착 예정 알림 커스텀 사운드 시스템 목록 등록) → 버그 아님. 프로토타입으로 검증까지 마쳤으나 우선순위 낮아 코드 롤백, 레시피만 기록해둠
버그28 (백그라운드 위치 추적 알림에 존재하지 않는 notificationChannelId 옵션) → 타입 정의상 없는 옵션 확인됨, 실제 무음 여부는 실기기 검증 필요
버그29 (READY interval 계산이 departureAlarmTime 임박 미반영) → 실기기 테스트로 발견, 최대 5분까지 DEPARTING 전환 지연 가능. 플라스크 수정 필요, 착수 전

해결된 버그(버그4~7, 10-A, 10-B, 11, 12, 13, 15, 16, 18, 19, 20, 26, 임시 interval 변경, Picker New Architecture 네이티브 크래시, 출발 알람 채널 사전 생성/단계별 커스텀 사운드, 배터리 최적화 상태 확인 네이티브 모듈, 단계별 알람 중복 발송/재발송 등)는 docs/history/resolved-bugs.md 참고.
```
