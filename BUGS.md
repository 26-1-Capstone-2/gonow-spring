# GPS 폴링 버그 목록

마지막 업데이트: 2026-07-31

해결된 버그는 `docs/history/resolved-bugs.md` 참고.

---

## 미수정 버그 목록

> 중요도 높은 순으로 정렬

---

### 🔴 버그19 — 막차 모드, 자정~새벽4시 사이 첫 계산 시 날짜가 하루 밀림 [영향 높음, 원인 확정·수정 방향 미확정]

→ 아래 버그19 상세 참고 (스프링+플라스크 양쪽 관련, 수정 방식 두 가지 검토 중)

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

## 미수정 버그 상세

---

### 🔴 버그19 — 막차 모드, 자정~새벽4시 사이 첫 계산 시 날짜가 하루 밀림 [영향 높음]

**관련 저장소**: `gonow`(스프링) + `gonow-flask`(플라스크) 둘 다 관련

**파일**:
- 스프링: `src/main/java/com/timemate/gonow/domain/journey/service/JourneyService.java`의 `callFlaskAndUpdate()`
- 플라스크: `CounterClockEngine/gps_api/routes/alarm.py:230-231` (`_compute_alarm`의 `is_last_mode` 분기), `CounterClockEngine/gps_api/core/transit_route.py:285` (`find_last_train_departure`)

**증상**: 막차 모드 귀가 여정이 READY 상태가 되고 나서 자정~새벽4시 사이에 첫 GPS 위치가 들어와 플라스크를 처음 호출하면, 추천 도착 시각이 실제보다 하루 밀려서 나옴. 예: 6/11 밤에 여정을 만들어 6/12 새벽 1시 도착을 기대했는데, 자정 넘겨서(예: 6/12 00:30) 첫 계산이 이뤄지면 6/13 새벽 1시로 계산됨.

**원인**:
- 막차 모드는 생성 시점에 목표 시각(`target_time`)을 모르기 때문에, 여정이 READY 상태가 되고 첫 GPS 위치가 들어와 `callFlaskAndUpdate()`가 처음 호출될 때 `journey.getTargetTime()`은 아직 `null`이고, 이 값이 그대로 플라스크 요청의 `target_time`으로 전달됨
- 플라스크(`alarm.py:231`) `search_ref = target_time if target_time is not None else _now_kst()` — `target_time`이 null이면 **플라스크 자신의 서버 시계**(`_now_kst()`, 날짜+시각 전부 포함)로 대체
- `find_last_train_departure()`(`transit_route.py:285`) `base_date = base_dt.date()` — `search_ref`의 **날짜 부분만** 뽑아서 그 날 23시~다음날 1시를 막차 탐색 범위로 고정
- 이 전체 과정에 "자정~새벽4시는 전날 밤의 연장으로 친다"는 보정이 전혀 없어서, 자정 넘겨서 첫 계산이 이뤄지면 탐색 기준 날짜가 하루 밀려버림
- 참고: 이 프로젝트에는 이미 같은 개념(서비스데이 보정, `scheduler.day-boundary-hour=4`)이 스프링의 `ArrivedTransitionService`에 구현돼 있음 — 거긴 정상 적용돼 있고, 이 경로(막차 첫 계산)에만 빠져 있던 것

**검토했으나 이 버그와 무관한 것으로 확인됨** (같이 헷갈리기 쉬워서 기록):
- 데드라인 모드 / 개인 여정 / 그룹 알람: `target_time`이 항상 명시적으로 주어지고, 플라스크의 "탐색" 로직(`is_last_mode` 분기) 자체를 안 타므로 이 버그와 무관. 목표 시각이 새벽 시간대(예: 새벽 2시)여도 문제없음.
- `JourneyService.resolveInitialStatus()` / `AppointmentService.resolveInitialStatus()`(여정·약속 생성 시 SCHEDULED/READY 결정): 겉보기엔 비슷한 "자정 보정 누락"처럼 보이지만, `plan_date` 필드에 `@FutureOrPresent` 검증이 걸려 있어 이 비교 시점엔 항상 유효한 값만 들어옴 → 별도 수정 불필요 (한 차례 상세 검토 후 기각).

**수정 방향 — 두 가지 검토, 아직 미결정**:

1. **꼼수 (스프링만 수정)**: `callFlaskAndUpdate()`에서 `journey.getTargetTime()`이 `null`일 때, 그대로 보내지 말고 "새벽4시 이전이면 전날로 보정한 현재 시각"을 계산해서 그 자리에 채워 보낸다 (`ArrivedTransitionService`가 쓰는 `day-boundary-hour` 설정 재사용). 플라스크는 전혀 수정하지 않는다.
   - 장점: 스프링 한 파일만 수정, 빠름
   - 단점: `target_time` 필드가 "확정된 값"과 "검색 기준 힌트"라는 두 가지 뜻을 몰래 겸하게 됨. **현재 플라스크 코드 기준으로는 안전함을 확인**(이 필드는 `is_last_mode` 분기에서 `search_ref` 계산에만 쓰이고 이후 로직 어디에도 재사용되지 않음) — 다만 나중에 플라스크가 "target_time이 있으면 이미 확정된 값이니 재검색 생략" 식으로 최적화되면 조용히 깨질 수 있는 잠재 위험이 있음

2. **정석 (스프링+플라스크 둘 다 수정)**: `FlaskJourneyRequest`(스프링)에 `search_anchor` 필드를 신설해 위와 동일한 보정값을 항상 채워 보낸다. 플라스크는 `alarm.py:231`을 `search_ref = target_time if target_time is not None else search_anchor`로 한 줄만 변경(`_now_kst()` 자기 시계 참조 제거). `target_time`은 원래 뜻("확정된 목표 도착 시각") 그대로 유지되어 미래 리스크가 없음.
   - 장점: 필드 의미가 명확해지고 위 미래 리스크가 사라짐
   - 단점: 플라스크도 같이 수정해야 함 (다만 두 저장소 합쳐 20줄 이내로 작업량 자체는 크지 않음)

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

### 🟡 버그26 — `backgroundLocationTask.ts`의 미정의 함수(`removeStagingKey`) 호출로 그룹 약속 4xx 시 크래시 [영향 낮음]

**파일**: `src/tasks/backgroundLocationTask.ts` (그룹 약속 `/location` 4xx 처리 분기)

**증상**: `await removeStagingKey(key);`를 호출하는데 이 함수가 프로젝트 어디에도 정의/임포트되어 있지 않음. 이 분기가 실행되면(그룹 약속이 4xx를 받으면) `ReferenceError`가 발생해 이후 정리 로직(`delete lastCallTimes[key]` 등)이 실행되지 않음.

**원인 추정**: 같은 파일에 이미 임포트되어 있는 `cancelStagedAlarms(key)`(예약된 단계별 로컬 알림 취소, `alarmService.ts`의 `cancelRemainingStages()`에서도 동일 목적으로 사용)를 호출하려던 자리가 오타/리팩토링 과정에서 존재하지 않는 이름으로 남은 것으로 보임. 버그22 조사 중 발견해서 `cancelStagedAlarms(key)`로 고쳤다가, 원래 목적과 무관한 범위라 다른 변경들과 함께 되돌림(이 문제 자체는 이번 조사와 완전히 무관한, 그 전부터 있던 버그).

**수정 방향**: `await removeStagingKey(key);` → `await cancelStagedAlarms(key);` 한 줄 교체.

---

## 인과관계 요약

```
현재 남은 실질적 문제:

버그19 (막차 모드 자정~새벽4시 날짜 밀림) → 원인 확정, 수정 방식(꼼수 vs 정석) 결정 대기 중, 스프링+플라스크 둘 다 관련
버그18 (DEPARTING 단계별 알람 중복)  → 임계 구간 두 세트 울림, 수정 필요

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
버그26 (backgroundLocationTask.ts 미정의 함수 호출 크래시) → 이번 조사와 무관한 기존 버그, 고쳤다가 다른 변경들과 함께 롤백

해결된 버그(버그4~7, 10-A, 10-B, 11, 12, 13, 15, 16, 20, 임시 interval 변경 등)는 docs/history/resolved-bugs.md 참고.
```
