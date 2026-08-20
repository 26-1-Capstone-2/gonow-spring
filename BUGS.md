# GPS 폴링 버그 목록

마지막 업데이트: 2026-08-20

해결된 버그는 `docs/history/resolved-bugs.md` 참고.

---

## 미수정 버그 목록

> 중요도 높은 순으로 정렬

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

### 🟢 버그47 — 막차 생성 시점(새벽 01~04시) 차단 메시지 UX 개선 아이디어 [버그 아님, 아이디어 문서로 이동]

→ 버그 아님(`validateLastTrainNotAlreadyMissed`는 정상 동작 중). 개선 아이디어는 `docs/planning/feature-ideas.md`의 "아이디어 H" 참고.

---

## 미수정 버그 상세

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

## 인과관계 요약

```
현재 남은 실질적 문제:

버그21 (인증 없는 테스트 스케줄러 엔드포인트) → 의도적으로 남겨둠, 테스트 필요 없어지면 파일 삭제
버그22 (막차 첫 계산이 새벽 1~4시대에 걸리면 이미 지난 시각 그대로 응답) → 재현 조건 희귀해서 수정 시도했다가 롤백, 버그23과 맞물림
버그23 (GPS 획득/권한 실패 시 무알림) → 영향 중간, 미사용 앱 권한 자동 해제로 실제 발생 가능성 있음
버그25 (/location 폴링 4xx 시 무알림) → 버그22 조사 중 발견, 수정했다가 롤백(단 메시지 파싱 유틸은 시나리오 A용으로 유지)

해결된 버그(버그1, 2, 3, 4~7, 8, 9, 10-A, 10-B, 11, 12, 13, 14, 15, 16, 17(재검토 결과 버그 아님으로 종결), 18, 19, 20, 24, 26, 27(커스텀 사운드를 시스템 소리 목록에 노출하는 방식 대신, 모든 알림 타입을 앱 내 소리/진동/무음 토글로 전환하는 방향으로 재설계해 최종 해결 — MediaStore 등록용 네이티브 모듈은 더 이상 필요 없어져 삭제), 28, 29(안드로이드 전용 서비스 기준 지오펜싱 마이그레이션으로 해소), 30, 34(별도 수정 없이 버그40으로 실질 해소), 35, 36, 37, 38, 39, 40(카카오모빌리티 future API 2-pass 도입), 41(플라스크는 막차 시각 미확정+700m 미만이면 값을 지어내지 않고 null 응답, 스프링은 `Journey.isLastTrainTimeConfirmed()` 가드로 NEARDEST/DEPARTING 전이 보류, 반복 막차 여정은 새벽 리셋 시 target_time도 함께 리셋 — 3개 수정 세트로 해결), 42(코드 롤백으로 해당없음 처리), 43(READY 분기 플라스크 중복 호출 가드 추가), 44(반복 여정 앵커/출발시각 스테일 값 리셋), 45(반복 알람 ARRIVED 이후 FGS 미유지, 4개 경로 파킹 처리), 46(`resumeIfDue()` 낡은 status로 포그라운드 재폴링 조용히 스킵되던 지오펜싱 마이그레이션 회귀), 48(DRIVING 모드 출발지-목적지 5m 이내 시 카카오모빌리티 거부로 알람 즉시 소멸, 100m 거리 사전 체크 추가), 임시 interval 변경, Picker New Architecture 네이티브 크래시, 출발 알람 채널 사전 생성/단계별 커스텀 사운드, 배터리 최적화 상태 확인 네이티브 모듈, 단계별 알람 중복 발송/재발송, 지오펜싱 마이그레이션(READY/DEPARTING/MOVING/NEARDEST 4개 상태, 2026-08-17 완료) 등)는 docs/history/resolved-bugs.md 참고. (버그3/8은 2026-08-10 한 차례 해결 후 2026-08-11 롤백, 2026-08-14 재구현으로 최종 해결됨)
```
