# GPS 동적 폴링 / 지오펜스 안정화 (프론트, 2026-08-14)

> 이 문서는 GoNow_Fronted 저장소의 프론트엔드 변경사항을 다룬다. 문서 관리는 정책상 이 백엔드 저장소(`gonow`)에서 통합한다.

## 배경

2026-08-13에 NEARDEST 상태를 폴링 방식에서 지오펜스(`Location.startGeofencingAsync`) 방식으로 전환하는 작업을 했다. 이후 실기기 재검증 과정에서 다음 3대 불변식을 기준으로 하루 종일 실측 테스트와 수정을 반복했다.

1. **지오펜스 구간과 GPS 폴링 구간이 겹치지 않을 것** — NEARDEST(지오펜스 감시 중)인 alarm에 대해 `/location` 실제 호출이 중복으로 나가면 안 됨
2. **GPS 동적 폴링이 포그라운드/백그라운드 간 끊김 없이 동기화될 것** — 서버가 준 interval이 드라이버 전환과 무관하게 하나의 연속된 흐름으로 유지돼야 함
3. **FGS(포그라운드 서비스) + GPS 폴링이 잘 분리될 것**

이 문서는 그 과정에서 발견하고 고친 버그들과, 최종적으로 "코드로는 더 손댈 수 없다"고 결론 내린 지점(OS 지오펜싱 API 자체의 한계)을 정리한다.

## 아키텍처 요약 (프론트, `GoNow_Fronted`)

- **포그라운드 드라이버**: `alarmService.ts`의 `AlarmRunner` — JS `setTimeout` 체인 + `Location.getCurrentPositionAsync()` 단발 요청
- **백그라운드 드라이버**: `backgroundLocationTask.ts`의 헤드리스 태스크 — `Location.startLocationUpdatesAsync()` 네이티브 구독
- **NEARDEST 지오펜스**: `nearDestGeofenceTask.ts` — `Location.startGeofencingAsync()`로 목적지 100m 반경 등록, EXIT 시 `fallbackToPolling()`으로 폴링 복귀
- 두 폴링 드라이버는 항상 **정확히 하나만** 활성화되도록 `maybeSyncGpsPolling()`(AppState 기준 분기)이 조정하고, NEARDEST 진입 시 둘 다 멈추고 지오펜스로 감시가 넘어간다.
- `ACTIVE_JOURNEYS_KEY`/`ACTIVE_APPOINTMENTS_KEY`(AsyncStorage)가 "지금 폴링 대상인가"를 판단하는 단일 진실 공급원이다.

## 발견하고 고친 버그들

### 1. NEARDEST 진입 시 `lastCallTimes` 삭제로 인한 중복 호출

먼저 NEARDEST를 감지한 드라이버가 `lastCallTimes[key]`를 곧바로 지우면, 늦게 도착한 다른 드라이버가 "호출 기록 없음"으로 오판해 같은 alarm에 또 실제 호출을 만들었다. → NEARDEST(및 ARRIVED) 진입 시 삭제하지 않도록 변경(`ACTIVE_*_KEY`에서 이미 빠져서 아무도 안 읽으므로 무해).

### 2. `resumeIfDue()`가 신뢰할 수 없는 로컬 상태(`this.status`)로 NEARDEST를 판단

포그라운드 러너의 `this.status`는 자기 자신이 직접 `poll()` 응답을 처리해야만 갱신된다. NEARDEST를 백그라운드가 먼저 감지하면(이동 중엔 흔함) 포그라운드 러너의 `this.status`는 안 바뀌어서, 포그라운드 복귀 시 지오펜스 전담 구간인데도 실제 폴링을 재개해버렸다. → `isKeyActivelyTracked(key)`를 추가해 `ACTIVE_*_KEY` 멤버십 기준으로 판단하도록 교체.

### 3. AppState 디바운스가 반대 방향 전환을 놓쳐서 낡은 타이머가 살아남음

`_layout.tsx`의 "3초 내 같은 방향 이벤트 중복 무시" 디바운스가 `background→active→background`처럼 빠른 왕복 전환에서 두 번째 `background`를 놓쳤다(그 사이 진짜 `active`가 끼어 있었는데도). `pauseAll()`이 안 불려서 직전에 예약된 타이머가 안 지워진 채 남았다가 NEARDEST 진입 이후까지 살아남아 뒤늦게 `poll()`을 직접 발동시켰다. → `active` 처리 시 `lastBackgroundAt = 0`, `background` 처리 시 `lastForegroundAt = 0`으로 서로 리셋.

### 4. `poll()` 자체에 방어선 부재

3번으로 근본 원인은 막았지만, 낡은 타이머가 `resumeIfDue()`를 안 거치고 곧장 `poll()`을 부르는 경로는 못 막는다. → `poll()` 진입 시에도 `isKeyActivelyTracked()`로 최종 확인. 단 `AlarmRunner.start()`가 러너를 막 생성하고 거는 최초 1회 호출은 예외(`skipActiveCheck`) — 이 호출은 낡은 타이머가 만들 수 없는 호출이라 체크가 불필요하고, 오히려 아래 5번의 원인이 됐다.

### 5. 콜드 스타트 직후 `addActiveId()`와 `poll()`의 경쟁 → 되돌린 시도

`AlarmManager.start()`가 `addActiveId()`를 fire-and-forget으로 던지고 바로 `poll()`을 부르면, 저장이 안 끝난 시점에 4번 체크가 "활성 추적 대상 아님"으로 오판했다.

- **1차 시도(실패)**: `addActiveId()`를 `await`로 바꿔 순서를 보장하려 했으나, 콜드 스타트 직후(특히 `expo-updates` JS 리로드 직후) 이 `await`가 영영 안 풀리는 정지 현상을 새로 만들었다 — 더 심한 회귀.
- **최종 수정**: `addActiveId()`는 fire-and-forget으로 되돌리고, 4번의 `skipActiveCheck`로 최초 poll() 호출이 체크를 건너뛰게 함 — `await` 자체가 불필요해져 정지 위험도 사라짐.

**교훈**: 경쟁 조건을 막으려고 `await`를 추가하는 게 항상 안전한 건 아니다. 체크를 건너뛰어도 안전한 케이스인지 먼저 따져보는 게 순서를 강제하는 것보다 나을 수 있다.

### 6. 콜드 스타트 시 살아남은 러너가 영원히 안 깨어남 (가장 근본적인 버그)

안드로이드가 FGS가 떠 있는 앱을 스와이프해도 프로세스가 안 죽는 경우가 있다. 이때 앱을 다시 열면 JS는 재시작("Running main"이 다시 찍힘)되지만 `AlarmManager.runners` 같은 모듈 상태가 살아남는 경우가 실기기 로그로 확인됐다. `_layout.tsx`의 "이미 active 상태" 초기화 분기(AppState 리스너가 전환을 못 잡는 콜드 스타트 예외 경로)가 `startReadyAlarms()` + `syncForegroundService()`만 부르고 **`alarmService.resumeAll()`을 안 불렀다** — 정상 `'active'` 리스너 분기는 부르는데 이 예외 경로만 빠뜨렸던 것. `doStartReadyAlarms()`는 `isRunning()`이 true라 `start()`를 다시 안 부르니, 살아남은 러너를 깨울 방법이 전혀 없었다.

→ 이 초기화 분기에도 `resumeAll()` 추가. 진짜 콜드 스타트라 runners가 비어있으면 no-op이라 안전. 로그로 `resumeAll()` 발동 → `resumeIfDue()` → 실제 `/location` 호출까지 정상 이어지는 것 확인.

## 코드 리뷰로 찾아서 정리한 것 (버그 아님)

`backgroundLocationTask.ts`의 `clearDesiredInterval()` — 2026-08-13에 만들었다가 2026-08-14 재검토 때 호출 지점을 이미 제거해서 함수 정의만 죽은 채로 남아있었다. 프로젝트 전체 그레핑으로 호출부 없음을 확인 후 제거.

## EXIT 지오펜스 — 코드 버그가 아니라 OS 플랫폼 한계

실외 테스트에서 EXIT 콜백(`eventType: 2`)이 "빨리 올 때도, 몇십 분씩 안 올 때도, 아예 유실될 때도" 있었다. 조사 결과:

- EXIT 이벤트는 OS(Fused Location Provider)가 배터리 최적화 차원에서 자체 판단해 던져주는 블랙박스다. 앱이 직접 거리를 재지 않는다.
- 실측 중 한 번은 EXIT이 아예 안 와서 사용자가 알람을 수동 삭제했는데, 그 삭제 처리 로그가 우연히 "EXIT이 늦게 처리된 것"처럼 보인 적이 있었다 — 로그 정밀 대조로 "EXIT 콜백 자체가 안 왔다"는 걸 확인. 같은 세션 안에서도 첫 EXIT은 4분 만에 정상 발화, 두 번째는 아예 무응답일 정도로 변동성이 크다.
- Android `GeofencingRequest.Builder.setNotificationResponsiveness(ms)`를 검토했으나, 이건 "빠르게" 스위치가 아니라 "이 정도 지연까지는 허용하니 배터리를 아껴달라"는 best-effort 힌트다. 공식 가이드도 5~10분을 권장해 오히려 반대 방향이라 적용하지 않기로 결론.
- 앱에서 조절 가능한 지오펜스 파라미터(반경, notifyOnEnter/Exit)는 이미 다 쓰고 있고, 그 이상은 OS 영역이라 코드로 개선할 방법이 없다.
- EXIT 관련 디버그 알림에 처리 시점(포그라운드/백그라운드)을 노출해, 적어도 "어느 상태에서 처리됐는지"는 adb 없이 바로 확인 가능하게 해둠.

## 남겨둔 임시 코드 (의도적)

`alarmService.ts`/`backgroundLocationTask.ts`의 `DEBUG_FORCE_INTERVAL_SEC = 15`는 서버가 준 interval(최대 300초)을 무시하고 15초로 강제 고정하는 테스트 전용 코드다. 오픈소스 대회 발표까지 시간이 많이 남았고 지오펜스 관련 기능이 아직 완전히 마무리된 상태가 아니라, 당분간 빠른 테스트 사이클 유지를 위해 의도적으로 원복하지 않기로 했다(2026-08-14 결정). 나중에 원복할 땐 `null`로 바꾸면 된다.

## 앞으로의 작업 방침 — 로그를 항상 남길 것

로그가 촘촘히 깔려 있으면 재현 한 번으로 근본 원인을 확정할 수 있지만, 로그가 없으면 정황 증거로 추론해야 해서 오판 위험이 커진다. 실제로 6번 버그(콜드 스타트 정지)는 "그 지점 이후로 로그가 아예 안 찍힌다"는 사실 하나로 원인을 좁힐 수 있었다.

앞으로 프론트 새 코드(특히 비동기 흐름, 상태 전이, 여러 드라이버/타이머가 얽히는 로직)를 작성할 때는 다음 지점에 로그를 남긴다:

- 함수/분기 진입점과 각 분기(성공/스킵/실패)
- 상태 전이가 일어나는 지점(예: `SCHEDULED → READY`)
- 여러 드라이버가 공유 자원(AsyncStorage 등)을 다루는 지점 — 어느 쪽이 언제 썼는지
- 타이머 예약/취소 지점
- 기존 스타일(`[모듈명] 상황 — 세부정보`)을 그대로 따를 것

실외 테스트처럼 adb 접근이 번거로운 상황을 위해, 사용자가 반복 확인해야 하는 정보는 `sendDebugNotification()`으로 알림에도 남긴다 — 단, 이미 다른 알림에 있는 정보를 또 다른 알림으로 쪼개서 중복시키지 않도록 주의.

## 테스트 방법 메모

- `adb logcat -c`로 버퍼를 비우고 재현 후 `adb logcat -d -v time > 파일`로 덤프, `ReactNativeJS` 태그로 필터링해서 분석.
- "백그라운드"와 "앱 종료(스와이프)"는 FGS가 떠 있는 한 코드 관점에서 완전히 동일하다(`AppState`는 애초에 이 둘을 구분하는 값이 없음) — 실측 시 굳이 둘 다 나눠 테스트할 필요 없음.
- 목적지 100m 이내로 알람을 생성하면 생성 직후 UI 블립(바텀시트 닫힘 등) 때문에 백그라운드/포그라운드 중 어느 쪽이 NEARDEST를 먼저 감지할지가 매번 달라진다 — 두 경우 다 정상 동작하는지 확인하려면 여러 번 반복하는 수밖에 없다.
