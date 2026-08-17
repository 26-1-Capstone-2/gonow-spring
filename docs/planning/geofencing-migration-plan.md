# 지오펜싱 도입 계획 (READY/DEPARTING/NEARDEST 대체)

`feature-ideas.md`의 "아이디어 C — 실제 지오펜싱 도입"을 실제 착수 가능한 수준으로 구체화한 문서. 아직 구현 전(설계 단계)이라 `docs/planning/`에 둔다 — 실제 구현이 시작되면 `docs/spec/journey-state-machine.md`가 갱신 대상이 된다.

> **진행 상황(2026-08-13)**: **NEARDEST 단계는 구현 완료 + 실기기 검증 완료 + 백그라운드 신뢰성 문제까지 해결 완료.** 순수 지오펜싱(사용자가 명시적으로 "우선 하이브리드 생각하지 말고 순수 지오펜스만 도입하자"고 결정)으로 구현했고, 그 과정에서 FGS 생명주기 정책(아래 섹션)과 백그라운드 네트워크 타임아웃 신뢰성(아래 "지오펜스 콜백 신뢰성" 섹션) 두 가지 큰 문제를 발견해 둘 다 해결했다. 두 번째 문제(EXIT 처리가 백그라운드에서 25~120초씩 지연되던 것)의 근본 원인은 **지오펜스 자체의 문제가 아니라 JS `setTimeout` 기반 타임아웃이 백그라운드에서 신뢰할 수 없다는 리액트 네이티브 공통의 한계**였다 — 상세 진단 과정과 최종 해결책(`XMLHttpRequest.timeout` 네이티브 타임아웃)은 `docs/history/resolved-bugs.md`의 "2026-08-13" 항목에 전체 기록. **DEPARTING/READY에 착수하기 전 반드시 아래 "FGS 생명주기 정책"과 "지오펜스 콜백 신뢰성" 섹션을 먼저 읽을 것** — 상태별 난이도 분석 표의 결론이 두 섹션 내용으로 일부 뒤집힌다. 다만 이 타임아웃 교훈은 지오펜스 전용이 아니라 **모든 백그라운드 네트워크 호출에 공통 적용**되므로, DEPARTING/READY에서 새로 만드는 네트워크 호출도 처음부터 `XMLHttpRequest.timeout` 패턴을 쓸 것.
>
> **추가 진행(2026-08-13, 같은 날 이후 세션)**: 아래 "expo-location의 제약: FGS와 GPS 폴링을 분리할 수 없다"에 적힌 `LOCATION_FGS_ACTIVE_KEY` 기반 "승격" 워크어라운드를 완전히 대체하는 **독립 네이티브 모듈(`modules/foreground-service`, GoNow_Fronted)을 구현·배포 완료**했다 — 알림 표시만 전담하는 별도 Android Service를 새로 만들어서, FGS 시작/중지와 GPS 구독 시작/중지가 이제 완전히 독립된 함수(`startAlarmForegroundService`/`stopAlarmForegroundService`/`startGpsPolling`/`stopGpsPolling`)로 분리됐다. "구독은 있는데 FGS가 없으면 껐다가 다시 켠다"는 승격 로직 자체가 개념적으로 사라졌다. 같은 세션에서 실기기 검증 중 추가로 발견·수정한 버그 3건(NEARDEST 재진입 지연 재발 가능한 경쟁 조건, 위치 권한 미승인 시 FGS 시작으로 인한 앱 크래시, dev-client 환경에서 지오펜스 EXIT 처리 중 동적 import 실패로 인한 태스크 크래시)은 `docs/history/resolved-bugs.md`의 "2026-08-13" 항목에 추가 기록. 이 변화로 아래 "FGS 생명주기 정책" 섹션 중 `LOCATION_FGS_ACTIVE_KEY`/"승격" 관련 서술은 **구현 방식 기준으로는 낡았지만, 정책(언제 FGS를 켜고 끌지) 자체는 그대로 유효**하다 — 자세한 내용은 각 섹션의 갱신 노트 참고.

## 배경 — 왜 지금 이 얘기가 나왔는지

버그3/버그8(백그라운드 GPS interval 30초 고정 문제)을 고치는 과정에서, 근본적인 한계에 부딪혔다: **앱이 순수하게 백그라운드에만 머물러 있는 동안은, 서버가 새 interval을 알려줘도 OS에 등록된 GPS 폴링 주기 자체는 갱신되지 않는다.** 이걸 완전히 해결하려면 백그라운드 컨텍스트에서 위치추적 서비스를 재시작(`stop → start`)해야 하는데, 이 패턴은 이 프로젝트가 과거 실제로 겪었던 크래시(`ForegroundServiceDidNotStartInTimeException`, `docs/history/resolved-bugs.md` 참고)와 정확히 같은 위험을 안고 있다. 그리고 이 크래시는 JS `try/catch`로 막을 수 없는 네이티브 레벨 강제종료다.

이 문제를 조사하다가, **READY/DEPARTING/NEARDEST 세 상태는 애초에 "주기적으로 위치를 확인"할 필요가 없고 "특정 경계를 넘었는지"만 알면 된다**는 걸 재확인했다 — 즉 폴링 자체가 필요 없는 상태들이라, 지오펜싱으로 바꾸면 "백그라운드에서 주기를 못 좁힌다"는 문제 자체가 이 세 상태에서는 발생하지 않게 된다. 위험한 재시작 코드를 만들 필요 없이 문제의 상당 부분이 구조적으로 사라지는 셈이라, 이 방향으로 먼저 진행하기로 결정했다(2026-08-11).

## 범위

| 상태 | 현재 방식 | 전환 후 |
|---|---|---|
| READY | 폴링(최대 300초) | **구현 완료(2026-08-17) — 지오펜싱(앵커 500m EXIT 재센터링 + 목적지 100m ENTER) + 서버 시간 트리거(`DepartingTransitionScheduler`), 실기기 검증 전.** `readyGeofenceTask.ts`. 착수 전 발견해 먼저 고친 관련 버그: 반복 여정 앵커/출발시각 스테일 값(버그44, `docs/history/resolved-bugs.md`) |
| DEPARTING | 폴링 | **완료(2026-08-17) — 지오펜싱(앵커 300m EXIT + 목적지 100m ENTER), 실기기 검증 완료(개인/귀가/그룹) + 커밋 완료(`75aa11f`).** `departingGeofenceTask.ts` |
| MOVING | 폴링(30~120초, 상대적으로 짧음) | **완료(2026-08-17) — 폴링 유지 + 보조 지오펜스, 실기기 검증 완료(그룹) + 커밋 완료(`75aa11f`)** — 실시간 속도/방향/ETA 계산이 필요해 폴링을 지오펜싱으로 완전히 대체할 수는 없지만, 목적지 100m ENTER 지오펜스를 폴링과 병행 등록해 도착 감지를 더 빠르게 함. `movingGeofenceTask.ts` |
| NEARDEST | 폴링 | **완료(2026-08-12) — 순수 지오펜싱(EXIT), 백업 폴링 없음.** 아래 "NEARDEST 하이브리드 설계"는 착수 전 설계 검토였고, 실제로는 하이브리드 대신 순수 지오펜싱으로 구현하기로 사용자가 결정함(신뢰성 보강은 다음 단계로 명시적 보류) |

`GeoConstants`(스프링)의 기존 상수(`RECOMPUTE_THRESHOLD_METERS=500`, `DEPARTURE_THRESHOLD_METERS=300`, `ARRIVAL_THRESHOLD_METERS=100`)를 반경 값으로 그대로 재사용 — 새 숫자를 정할 필요 없음.

## 플랫폼 범위 — 사실상 안드로이드 전용

`journey-state-machine.md`에 이미 명시된 이유(§1, "Apple Developer 계정 없이 iOS 개발 시 지오펜싱 불가")로, 이 마이그레이션은 **안드로이드에서만** 지오펜싱을 쓰고 iOS는 기존 폴링을 그대로 유지해야 한다. 즉 프론트 코드에 `Platform.OS === 'android'` 분기가 필요하며, 이 분기 지점(어디서 "폴링 모드"와 "지오펜싱 모드"를 가를지)은 상태 하나를 실제로 구현하기 전에 먼저 확정해야 한다 — 나중에 상태를 하나씩 추가할 때마다 분기 지점을 고치는 것보다, 처음부터 "이 여정/참가자를 지금 폴링으로 볼지 지오펜싱으로 볼지" 판단하는 작은 디스패처를 하나 두는 편이 안전하다.

## 백엔드 변경이 필요 없는 이유(리스크 평가의 핵심 근거)

`JourneyService.updateLocation()`/`ParticipantService.updateLocation()`의 상태별 switch-case는 좌표가 "타이머 폴링으로 왔는지 지오펜스 ENTER/EXIT 콜백으로 왔는지"를 전혀 구분하지 않는다 — 그냥 좌표 하나 받아서 거리 계산 후 분기할 뿐이다. 즉 지오펜싱 전환은 **프론트(Expo)가 언제 `PATCH /location`을 호출하느냐**만 바꾸는 문제이고, 백엔드 상태머신 코드는 원칙적으로 한 줄도 안 바꿔도 된다. 이게 아래 "상태별 난이도 분석"에서 상태마다 리스크가 갈리는 이유이기도 하다 — 상태별로 필요한 것은 결국 "프론트가 몇 개의 지오펜스를, 언제 재등록해야 하는가"의 차이일 뿐이다.

## 상태별 난이도 분석 및 착수 순서

아래 표는 "얼마나 흔한 상태인가"가 아니라 "지오펜싱으로 바꿀 때 새로 설계해야 할 게 얼마나 되는가" 기준으로 정리한 것이다. 착수 순서는 이 기준을 따른다.

| 순서 | 상태 | 경계 개수 | 재센터링 필요? | 순수 시간 트리거 섞여 있나? | 서버 로직 신규 필요? |
|---|---|---|---|---|---|
| 1 (가장 쉬움) | **NEARDEST** | 1개(목적지 100m, 고정) — 단, EXIT는 저빈도 백업 폴링 병행(아래 참고) | 없음 | 없음(시간 판단은 이미 서버 분기에 내장돼 있고 EXIT 콜백이 오면 그대로 전달만 하면 됨) | 없음 — P≥Q "고정" 분기, P<Q "READY 복귀" 분기 둘 다 기존 코드가 이미 처리 |
| 2 | **DEPARTING** | 2개(목적지 100m ENTER, 앵커 300m EXIT, 둘 다 고정) | 없음(앵커는 READY 때 값을 그대로 재사용, DEPARTING 안에서는 갱신 안 됨) | 없음 | 없음 — 두 전이 모두 "경계 이탈/진입 = 즉시 전이"라 지오펜스 콜백과 1:1 대응 |
| 3 (가장 어려움) | **READY** | 2개(목적지 100m + 앵커 500m)지만 앵커가 이동할 때마다 **재등록 필요** | 있음 — 500m 이탈마다 새 앵커 기준으로 지오펜스 재등록 | **있음** — `P >= Q` 순수 시간 트리거는 위치와 무관해서, 폴링이 없어지면 이걸 감시할 완전히 새로운 메커니즘이 별도로 필요 | **있음(2026-08-11 정정)** — `DepartingTransitionScheduler` 신규 필요(아래 "READY→DEPARTING 시간 트리거" 참고). 이전엔 "서버 동일"로 적었으나 틀린 판단이었음 |

**NEARDEST → DEPARTING → READY** 순으로 착수한다. 뒤로 갈수록 새로 설계해야 할 인프라(재센터링, 시간 트리거)가 늘어나므로, 서버 변경 없이 지오펜스 등록→콜백→기존 `/location` 재사용 패턴을 먼저 NEARDEST에서 검증하고, 그 경험(과 위 플랫폼 분기 디스패처)을 DEPARTING에 재사용한 뒤, 가장 복잡한 READY로 넘어간다.

- **NEARDEST가 쉬운 이유 보충**: 오히려 지오펜싱으로 바꾸면 대기 중 폴링주기를 계산하던 `GeoUtils.computeIntervalByTime()`이 통째로 필요 없어진다 — 순수 개선. NEARDEST 진입 자체는 여전히 READY의 폴링이 담당하므로(READY가 아직 폴링 기반인 동안은 안 건드림), EXIT로 READY에 복귀하는 순간에만 "이제 다시 폴링 모드로 전환"하는 디스패치가 필요하다.
- **DEPARTING이 쉬운 이유 보충**: 두 전이 모두 "그 경계를 벗어나거나 들어오면 끝"이라 상태 유지 로직도 따로 필요 없다(지오펜스 이벤트가 안 오면 자연스럽게 DEPARTING 유지 = 기존 "300m 미만이면 DEPARTING 유지" 의미와 정확히 일치).

## 왜 안전한가

`Location.startGeofencingAsync()`는 안드로이드가 "앱이 죽어있어도 대신 감시해주는" 용도로 공식 제공하는 메커니즘이라, 매번 새로 GPS를 찌르거나 위치추적 서비스를 재시작할 필요가 없다. 이미 이 프로젝트는 `expo-location`을 쓰고 있고, `app.json`에 필요한 권한(`ACCESS_BACKGROUND_LOCATION` 등)도 이미 선언돼 있어서 새 라이브러리나 권한 추가 없이 바로 시작 가능하다.

> **2026-08-12 정정**: "상단바 알림(FGS)을 계속 띄워둘 필요가 없다"는 원래 문장은 **틀린 판단이었음이 드러났다.** NEARDEST 하나만 보면 맞는 말이지만, DEPARTING 이후(특히 MOVING 진입) 상태는 여전히 FGS가 필요하고, 그 FGS를 백그라운드에서 뒤늦게 새로 켜는 건 안드로이드가 원천 차단한다(`ForegroundServiceStartNotAllowedException`). 자세한 내용과 최종 정책은 바로 아래 "FGS 생명주기 정책" 섹션 참고.

## FGS(포그라운드 서비스) 생명주기 정책 (2026-08-12 신규 — DEPARTING/READY 착수 전 필독)

NEARDEST를 실기기로 검증하는 과정에서, NEARDEST 하나만 봐서는 안 드러나지만 **DEPARTING/READY까지 지오펜싱을 확장하면 반드시 부딪히는** 근본적인 제약을 여러 개 발견했다. 이 섹션이 그 전체를 정리한 것 — 아래 "상태별 난이도 분석" 표의 결론(서버 변경 불필요, 신규 설계 최소)은 **폴링↔지오펜싱 전환 로직 자체에 한해서는** 여전히 유효하지만, FGS 생명주기는 별도로 이 섹션의 정책을 따라야 한다.

### 핵심 제약: 백그라운드에서 새 FGS를 못 켠다

안드로이드 12+(API 31+)부터, 앱이 백그라운드 상태일 때 코드에서 `startForegroundService()`를 부르면 `ForegroundServiceStartNotAllowedException`으로 즉시 크래시한다. 이건 JS `try/catch`로 못 막는 OS 레벨 제약이고, 언어(JS/Kotlin 무관)와도 무관하다. 즉 **"지오펜스 EXIT가 백그라운드에서 터지고, 그 순간 바로 FGS를 새로 켜야 하는" 시나리오는 구조적으로 성립 불가능**하다 — 이게 있으면 무조건 크래시하거나(시도하면), 애초에 시도를 안 하고 FGS 없이 넘어가야 한다(현재 구현 방식).

**예외적으로 허용되는 경우**(안드로이드 공식 문서에 명시된 배경 FGS 시작 예외 사유 중 이 프로젝트와 관련된 것):
- 앱이 포그라운드로 전환되는 시점(당연히 허용 — 지금 이 예외를 씀)
- 고우선순위(High Priority) FCM 메시지를 수신해 처리하는 순간 — **단, 이건 "일반 배경 시작 제한"의 예외 목록에만 있고, `ACCESS_BACKGROUND_LOCATION`처럼 while-in-use 권한이 필요한 서비스 타입(위치/카메라/마이크)에 적용되는 별도의(더 엄격한) 예외 목록에는 없다**([공식 문서](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start) 직접 인용 확인, 2026-08-12) — 그 목록엔 시스템 컴포넌트/앱 위젯/알림 상호작용/다른 앱의 PendingIntent/기기 정책 컨트롤러/VoiceInteractionService/`START_ACTIVITIES_FROM_BACKGROUND` 권한, 이렇게 7개만 있고 FCM은 없음.
- `AlarmManager.setExactAndAllowWhileIdle()`로 예약한 알람이 실제로 울리는 순간도 예외 사유로 알려져 있으나, 이 프로젝트에서 실측 검증한 적은 없음(미검증).

**✅ 실기기 검증 완료(2026-08-12) — 가설 기각, 이 경로는 안 됨.** `FcmSender.sendData()`가 이미 `AndroidConfig.Priority.HIGH`로 보내고 있음을 확인한 뒤(스프링 코드로 확인 완료), `backgroundAlarmTask.ts`의 FCM 핸들러에서 `foregroundService` 옵션을 포함해 위치 구독 시작을 실험적으로 시도(try/catch로 안전하게 격리)하고, `firebase-admin`으로 동일한 페이로드(`{data: {journey_ids}, android: {priority: 'high'}}`)를 앱 완전 종료 상태의 테스트 기기에 직접 발송해서 검증했다. 결과:
```
Call to function 'ExpoLocation.startLocationUpdatesAsync' has been rejected.
→ Caused by: Couldn't start the foreground service. Foreground service cannot be started when the application is in the background
```
FCM 자체는 정상 수신됐지만(로그로 payload 확인됨), `foregroundService` 옵션을 포함한 시작 시도는 즉시 거부됐다 — 위 while-in-use 제한 분석과 정확히 일치하는 결과. **새벽 4시(또는 임의 시점) 백그라운드 FCM 웨이크업으로 위치추적용 FGS를 새로 켜는 방법은 없다고 최종 확정.** 실험 코드는 원래(FGS 없이 시작하는 폴백 전용) 상태로 롤백 완료. → 결론적으로 "FGS 생명주기 정책"에서 확정한 **"READY 진입 시점(대부분 포그라운드)부터 상시 유지"** 전략이 유일하게 남은 현실적인 답이다.

### expo-location의 제약: FGS와 GPS 폴링을 분리할 수 없다 — **해결 완료(2026-08-13, 독립 네이티브 모듈로 대체)**

`Location.startLocationUpdatesAsync(taskName, options)`는 `foregroundService` 옵션 유무로 FGS를 켤지 말지만 정할 수 있고, **"FGS는 켜되 GPS는 폴링 안 함"이라는 조합은 이 API로 표현이 안 된다.** FGS를 켜는 순간 그 안에 설정한 `timeInterval`대로 GPS도 같이 돈다. 그래서 "배터리 아끼려고 FGS만 잠깐 끈다"는 접근은 이 라이브러리 수준에서는 "FGS를 끄면 GPS 폴링도 같이 끊긴다"와 동의어였다.

또한 `Location.hasStartedLocationUpdatesAsync(taskName)`는 "그 이름의 구독이 켜져 있나"만 알려주고 **"FGS를 포함해서 켜져 있나"는 구분하지 못한다.** 그래서 한 번 FGS 없이(아래 "저품질 폴백" 경로로) 구독이 시작되면, 이후 `startBackgroundLocationUpdates()`가 이 API로 "이미 실행 중"이라 오판해서 **영원히 FGS로 승격을 시도조차 안 하는 버그**가 있음을 2026-08-12에 발견했다(NEARDEST의 EXIT→READY 폴백 경로, `nearDestGeofenceTask.ts`의 `fallbackToPolling()`에서 재현 가능). 처음엔 `LOCATION_FGS_ACTIVE_KEY`(AsyncStorage 플래그) + "구독은 있는데 플래그가 없으면 껐다가 FGS 포함해서 재시작(승격)"하는 방식으로 임시 봉합했다.

**2026-08-13(같은 날 이후 세션) 최종 해결**: 위 워크어라운드 자체를 없애고, 알림 표시만 전담하는 **독립 네이티브 모듈(`modules/foreground-service`, `battery-optimization`과 동일한 로컬 Expo 모듈 컨벤션)**을 새로 만들었다. `ForegroundAlarmService.kt`는 위치 콜백을 전혀 참조하지 않는 순수 `Service`이고, `ForegroundServiceModule.kt`가 `start()`/`stop()`만 노출한다. GPS 구독은 이제 항상 `foregroundService` 옵션 없이만 시작하므로(`startGpsPolling()`), `hasStartedLocationUpdatesAsync()` 하나로 실행 여부 판단이 충분해졌고 "승격" 개념 자체가 사라졌다. `startAlarmForegroundService()`/`stopAlarmForegroundService()`(FGS 전담)와 `startGpsPolling()`/`stopGpsPolling()`(GPS 전담)이 완전히 독립된 함수가 됐고, `maybeSyncGpsPolling()`이 `ACTIVE_JOURNEYS_KEY`/`ACTIVE_APPOINTMENTS_KEY` 기준으로 GPS 폴링 필요 여부만 별도로 재판단한다(전부 `backgroundLocationTask.ts`). 실기기로 "FGS 유지 + GPS 요청 0건" 조합이 실제로 성립함을 dumpsys로 검증 완료.

**부수 발견 — FGS 시작 시 위치 권한 미확인 크래시(2026-08-13)**: 새로 만든 `ForegroundAlarmService.kt`가 위치 권한 승인 여부를 확인 안 하고 무조건 `startForeground(..., FOREGROUND_SERVICE_TYPE_LOCATION)`을 호출하도록 돼 있어서, 신규 설치 기기에서 로그인 직후 기존 알람을 발견해 FGS를 켜려는 순간 `SecurityException`으로 앱 전체가 죽는 크래시가 있었다. `startAlarmForegroundService()`(JS)에 `Location.getForegroundPermissionsAsync()` 체크를 추가하고, `ForegroundAlarmService.kt`에도 `SecurityException` try/catch(방어용, 서비스만 조용히 중단)를 추가해 해결. 상세는 `docs/history/resolved-bugs.md`의 "2026-08-13" 항목 참고.

### 헤드리스 컨텍스트는 서로 완전히 분리돼 있다

`backgroundAlarmTask.ts`(FCM 웨이크업)와 `nearDestGeofenceTask.ts`(지오펜스 콜백), `backgroundLocationTask.ts`(위치 폴링)의 헤드리스 실행은 앱이 완전 종료된 상태에서 각자 **독립된 JS 컨텍스트**로 뜬다(모듈 레벨 변수가 서로 공유 안 됨, 매번 새로 초기화됨). 그래서 이 태스크들 사이에 뭔가 상태를 공유해야 하면 반드시 AsyncStorage(영속 저장소)를 거쳐야 한다 — `ACTIVE_JOURNEYS_KEY`/`ALARM_NAV_INFO_KEY` 등 기존 키들이 이미 이 원칙을 따르고 있고, 위 FGS 플래그도 같은 이유로 AsyncStorage가 필요하다.

### "승격 로직"만으로는 부족하다 — 언제 상시 유지로 가야 하는가

애초에 세운 전략은 "백그라운드에서는 FGS 없이 가볍게 버티다가, 유저가 앱을 다음에 열 때 FGS로 승격"이었다. 이건 **NEARDEST의 EXIT→READY 복귀**처럼 되돌아가는 상태가 그 자체로 시간에 안 급한 경우엔 합리적이다(READY는 이미 몇 시간이고 대기하는 상태라, 잠깐 저품질로 있어도 무방). 그런데 **"유저가 앱을 다음에 열 때"라는 전제 자체가 위험하다** — GoNow처럼 "이동 중" 시나리오가 핵심인 앱은 유저가 폰을 주머니에 넣거나 다른 앱(유튜브 등)을 보며 이동하는 게 정상적인 사용 패턴이라, **앱을 언제 다시 열지 보장이 전혀 없다.** 저품질 상태(FGS 없음)로 무기한 남으면 Doze로 인해 위치 갱신이 몇 분~수십 분 지연되거나 완전히 멈출 수 있다(안드로이드 표준 동작, 지오펜싱이 새로 만드는 리스크 아님).

**최종 확정 정책(2026-08-12, 구현·배포 완료)**: 처음엔 "READY부터 상시 유지, NEARDEST만 끔"으로 갔었는데, 이것도 구멍이 있었다 — READY 진입 자체가 새벽 4시에 백그라운드에서 일어나면(반복 여정 등) 그 시작점에서 또 똑같은 "백그라운드에서 FGS 못 켬" 벽에 부딪힌다. 그래서 한 단계 더 단순화했다: **FGS는 "이 계정에 알람이 하나라도 있으면" 켜고, "알람이 완전히 없어지면"만 끈다 — NEARDEST도 더 이상 안 끈다.**

| 상태 | FGS | 근거 |
|---|---|---|
| SCHEDULED | **켜짐(알람 생성 시점부터 상시 유지)** | 알람을 만드는 행위 자체가 무조건 포그라운드라, 이 순간에 켜두면 이후 모든 상태 전환(READY/DEPARTING/MOVING)이 "이미 켜진 걸 계속 쓰는" 것뿐이라 백그라운드에서 새로 켜야 하는 상황 자체가 아예 안 생김 |
| READY / DEPARTING / MOVING / NEARDEST | 켜짐(유지) | 위와 동일 — SCHEDULED부터 이어짐. NEARDEST도 예외 없이 유지(예전엔 여기서만 껐는데, 그러면 EXIT→READY 복귀나 반복 여정의 다음 회차 진입 시점에 다시 못 켤 위험이 배터리 이득보다 커서 포기함) |
| ARRIVED(비반복 알람) | 꺼짐(다른 알람이 없다면) | 완전히 끝났으므로 |
| ARRIVED(반복 알람) | **켜짐이어야 함(미구현, 아래 참고)** | |

**구현**: `AlarmManager.hasActivePolling()`이 이제 그냥 `this.runners.size > 0`(상태 무관). 배경 위치추적 틱(`backgroundLocationTask.ts`)도 "폴링 대상 목록이 비었나"가 아니라 `ALARM_NAV_INFO_KEY`(NEARDEST로 넘어가도 안 지워지는, "알람이 존재하긴 하는지"의 근거)를 기준으로 FGS를 끌지 판단하도록 같이 고쳤다.

**⚠️ 남은 구멍 — 반복 알람(미구현, 다음으로 미룸)**: ARRIVED 도달 시 `handlePersonalStatus`/`handleGroupStatus`가 반복 여부와 무관하게 무조건 `this.stop()`을 호출해서 runner가 사라진다. 그런데 반복 여정은 서버가 다음 해당 요일 새벽 4시에 이 여정을 다시 READY로 되돌리는데(CLAUDE.md: "반복 여정... `ARRIVED` 포함 `READY` 전환"), 이 runner가 마지막 하나였다면 그 사이 FGS가 꺼져 있다가 다음 새벽 4시 전환 때 또 똑같이 "백그라운드에서 FGS 못 켬" 문제를 겪는다 — **매 회차마다 반복될 수 있음.** 고치려면 `AlarmRunner`의 생명주기 자체를 손봐야 한다(ARRIVED에서도 반복 여정이면 runner를 완전히 안 지우고 "다음 회차 대기" 상태로 남기는 로직 필요, `AlarmTarget`에 반복 여부/`repeatDays` 전달도 추가해야 함) — 범위가 있는 별도 작업이라 의도적으로 미룸.

**FGS 승격 버그도 같이 수정 완료(2026-08-12)**: `Location.hasStartedLocationUpdatesAsync()`가 FGS 포함 여부를 구분 못 해서 한 번 FGS 없이 시작되면 영원히 승격 안 되던 버그 — `LOCATION_FGS_ACTIVE_KEY`(AsyncStorage)로 FGS 포함 여부를 직접 추적하고, "구독은 있는데 FGS가 없으면" 껐다가 다시 FGS 포함해서 켜도록 `startBackgroundLocationUpdates()`를 고쳤다. 이제 알람이 하나라도 있는 한 FGS는 거의 안 꺼지지만, 위 반복 알람 문제처럼 "잠깐이라도 알람이 0개가 됐다가 백그라운드에서 다시 생기는" 경로가 있는 한 이 안전망은 여전히 유효하다.

**배터리 트레이드오프**: 알람이 존재하는 내내(SCHEDULED 며칠 전부터) FGS+GPS 구독이 켜져 있다는 뜻이라 배터리를 어느 정도 희생한다. FGS 배지 자체의 비용은 낮고(안드로이드가 이 프로세스를 캐시 우선순위에서 보호해줄 뿐), 실제 배터리를 먹는 건 거기 딸려오는 GPS 폴링 주기다 — `timeInterval`을 SCHEDULED/READY처럼 급하지 않은 대기 구간에서는 훨씬 길게(예: 몇 시간) 잡아서 GPS 비용만 낮추는 절충이 가능하다(미구현, 다음 단계 검토 대상).

## 지오펜스 콜백 신뢰성 — 지연/유실 가능성과 안전장치

안드로이드 지오펜싱은 배터리 절약을 위해 ENTER/EXIT 콜백을 즉시 안 주고 지연시킬 수 있다(Doze 모드/앱 대기 최적화). 앱의 배터리 최적화를 "제한 없음"으로 설정하면 표준 안드로이드의 지연은 크게 완화되지만, 일부 제조사(샤오미/화웨이/삼성 등)의 자체 배터리 관리자는 이 설정과 별개로 백그라운드 앱을 더 강하게 제약할 수 있어 완전한 보장은 아니다(`dontkillmyapp.com` 등 업계에 널리 알려진 문제). 극단적인 경우 콜백이 아예 안 올 수도 있다.

이건 지오펜싱이 새로 만드는 리스크가 아니라 지금 폴링/FCM 방식에도 이미 있던 백그라운드 실행 제약(동일한 OS 정책 아래 있음)이라, 지오펜싱 도입으로 더 나빠지는 게 아니라 오히려 배터리를 덜 써서 OS가 이 앱을 덜 의심하게 되는 효과가 있다. 다만 콜백 유실의 실질적 파급 효과는 상태마다 다르므로, 아래처럼 상태별로 따로 판단한다.

**✅ 실기기 검증 완료, 원인 규명 + 해결 완료(2026-08-13)**: NEARDEST EXIT 처리 중 좌표 확보→`/location` 호출→응답 구간이 백그라운드에서 25~120초씩 불규칙하게 지연됐다가 앱을 포그라운드로 전환하는 순간에야 처리되는 문제를 실측으로 반복 확인했다. 배터리 최적화/App Standby Bucket(`EXEMPTED`)/Doze 화이트리스트/JobScheduler 우선순위(`setExpedited`) 등 여러 가설을 실측으로 하나씩 기각한 끝에, **최종 원인은 지오펜스가 아니라 `patchLocation()`이 쓰던 JS `setTimeout` 기반 타임아웃(`fetch()`+`AbortController`)이 백그라운드에서 신뢰할 수 없었다는 것**으로 확정됐다 — 같은 시간대에 GPS 네이티브 호출이나 다른 헤드리스 태스크는 계속 정상 실행돼서 JS 엔진 자체는 안 얼어 있었는데, 오직 JS 타이머 콜백만 제때 전달이 안 됐다. `XMLHttpRequest.timeout`(JS 타이머를 안 거치고 OkHttp `callTimeout()`으로 직결되는 네이티브 레벨 타임아웃)으로 교체해 해결 — 재검증 결과 모든 EXIT 처리가 193~476ms로 일관되게 빨라짐(재시도 로직도 발동할 필요가 없었음). 전체 진단 과정(기각된 가설 목록, 코드 근거, 검증 로그)과 최종 구현 코드는 `docs/history/resolved-bugs.md`의 "2026-08-13" 항목에 상세 기록. **이 타임아웃 교훈은 지오펜스 전용이 아니라 모든 백그라운드 네트워크 호출에 공통 적용되므로, DEPARTING/READY에서 새로 만드는 네트워크 호출도 처음부터 이 패턴을 쓸 것.**

**⚠️ 실기기로 신규 발견(2026-08-16) — 같은 지오펜스 콜백이 중복 전달될 수 있음, 처리 로직은 반드시 key 단위로 직렬화할 것**: DEPARTING(Phase 1, 출발지 300m EXIT + 목적지 100m ENTER) 실기기 테스트 중, 앵커 EXIT와 목적지 ENTER가 35ms 안에 동시 발화했는데 그중 **앵커 EXIT가 안드로이드에 의해 같은 key로 2번 중복 전달**됐다(같은 시간대에 EXIT×2 + ENTER×1, 총 3개 콜백 발화). 안드로이드 지오펜싱은 위 지연/유실 문제와 마찬가지로 "정확히 1번만 오는 신호"를 보장하지 않는다 — 지연/유실뿐 아니라 **중복**도 이미 알려진 OS 레벨 특성이라 앱이 방어해야 한다. 당시 `departingGeofenceTask.ts`엔 key별 처리 중 여부를 확인하는 가드가 없어서, 3개 콜백이 각자 독립적으로 GPS 재확보 + `/location` 호출 + 디버그 알림 발송까지 진행했다(서버 호출 3배 낭비, 디버그 알림도 3개 중복 발송 — 최종 상태는 셋 다 동일하게 수렴해서 데이터 오염이나 크래시는 없었음, `exitDepartingGeofenceMode()`의 idempotency 덕분).

**해결(같은 날 적용)**: `departingGeofenceTask.ts`에 key별 처리 락(`withKeyLock`)을 추가 — 지오펜스 콜백이 오면 먼저 "이 key가 아직 등록돼 있는가"(=아직 아무도 처리 안 함)부터 확인하고, 아니면(이미 다른 콜백이 먼저 처리해서 등록이 지워졌으면) GPS/서버 호출 없이 조용히 skip한다. 같은 key로 들어온 여러 콜백은 큐에 줄 세워 하나씩만 실제 처리한다.

**✅ NEARDEST(`nearDestGeofenceTask.ts`)에도 적용 완료(2026-08-17)**: DEPARTING/MOVING/READY와 동일한 `withKeyLock` 패턴을 소급 적용해, 4개 지오펜스 태스크 전부 같은 동시성 방어 구조를 쓴다.

**READY(Phase 3)는 이 문제에 더 취약할 것으로 예상됨**: 위 "새로 설계해야 하는 것들 2"(NEARDEST의 "스쳐지나간 케이스")에 이미 적어뒀듯, READY 지오펜싱이 도입되면 목적지 100m 지오펜스 하나에 **ENTER와 EXIT를 동시에 등록**해야 한다 — 지금 DEPARTING처럼 "서로 다른 두 원이 각각 한 방향만" 감시하는 것보다, "같은 원의 경계선에서 ENTER/EXIT가 GPS 흔들림만으로 번갈아 뜨는" 경우의 수가 더 많아진다. READY 착수 시 이 key-lock 패턴은 선택이 아니라 필수로 처음부터 넣을 것.

## NEARDEST 하이브리드 설계 — 순수 지오펜싱이 아닌 이유 (착수 전 검토, 실제로는 채택 안 됨)

> **2026-08-12 갱신**: 아래는 착수 전에 검토했던 설계 논리이고, 실제 구현 시점에 사용자가 "우선 하이브리드 생각하지 말고 순수 지오펜스만 도입하자"고 명시적으로 결정해서 **백업 폴링 없이 순수 지오펜싱으로 구현·검증 완료**됐다. 아래 리스크 분석 자체는 여전히 유효한 참고 자료(다음 단계에서 하이브리드를 재검토할 때 근거로 재사용 가능)라 남겨둔다.


**EXIT 콜백을 놓치면 지각으로 이어질 수 있다.** NEARDEST 진입 시점에 서버는 `departureAlarmTime`을 "이미 목적지 근처에 도착했다"는 전제(이동 시간 ≈0)로 재계산해서 저장한다(`callFlaskAndUpdate`가 `isNearDest` 조건에도 걸림). 이 값을 기준으로 프론트는 이미 단계별 로컬 알림을 예약해둔다. 만약 사용자가 그 후 실제로 100m 밖으로 나갔는데(예: 근처 카페) EXIT 콜백이 안 잡히면, 서버는 계속 NEARDEST로 알고 있고 `departureAlarmTime`도 "이미 도착했다"는 낡은 전제 그대로 남는다 — 실제로는 다시 이동해서 돌아와야 하는데 그 이동 시간이 전혀 반영이 안 된 채로 있는 것. `ArrivedTransitionScheduler`(targetTime 초과 시 자동 ARRIVED)는 이 문제를 해결 못 한다 — 이건 사후 뒷정리일 뿐, 사전에 사용자에게 제때 나가라고 경고하는 기능이 아니기 때문이다.

**그래서 NEARDEST의 EXIT는 순수 지오펜싱이 아니라 하이브리드로 간다**: 지오펜스 EXIT를 주력으로 쓰되, 5~10분 정도의 낮은 빈도 백업 폴링을 병행한다. 지오펜스가 정상 동작하면 즉시 반응(배터리 이득 그대로), 콜백을 놓쳐도 최악의 경우 "무한정 갇힘"이 아니라 "최대 5~10분 지연"으로 리스크가 유계(bounded)해진다.

**반대로 ENTER(READY→NEARDEST)를 놓치는 건 하이브리드가 필요 없다 — 훨씬 낮은 리스크.** READY 상태의 앵커 500m 재계산 로직이 실좌표 기반으로 계속 `departureAlarmTime`을 정확하게 유지해주기 때문에(목적지에 실제로 도착해 있으면 그 재계산도 이동 시간 ≈0으로 정확하게 나옴), ENTER를 놓쳐도 알람 시각 자체가 틀어지지 않는다. 놓쳤을 때 실질적으로 빠지는 건 "도착 확인해주세요" 로컬 알림(`sendArrivalCheckAlarm`)과 `/arrive` 버튼 노출 정도의 UX 손해뿐 — 지각 위험은 없다. 그래서 ENTER는 순수 지오펜싱으로 두고, EXIT만 백업 폴링을 병행한다.

## READY→DEPARTING 시간 트리거 — `DepartingTransitionScheduler` 신설

> **구현 완료(2026-08-17)** — 아래 설계 그대로 구현됨(`DepartingTransitionScheduler`/`DepartingTransitionService`, `JourneyRepository`/`ParticipantRepository`에 `findIdsReadyOverdue`/`bulkUpdateToDeparting`/토큰 매핑 메서드 신설). Participant는 `departureAlarmTime`이 참가자별 독립 계산값이라(약속 단위 `targetTime` 공유값을 쓰는 NEARDEST/지각 정리 쿼리와 달리) 참가자 ID 단위로 개별 판정하도록 구현 — 약속 단위로 묶으면 다른 참가자의 알람 시각까지 잘못 전환시킬 위험이 있었음.

`P >= Q`(현재 시각이 `departureAlarmTime` 도달)는 위치와 무관한 순수 시간 조건이라 지오펜스 이벤트로는 원천적으로 못 잡는다(사용자가 안 움직이면 이벤트 자체가 없음). 이 트리거는 클라이언트가 정시에 스스로를 깨우는 방식(안드로이드 `AlarmManager` 등 네이티브 모듈 필요) 대신, **서버 스케줄러로 해결한다** — 이미 `ArrivedTransitionScheduler`가 똑같은 성격의 일(순수 시간 조건으로 상태 벌크 전환)을 하고 있어서 그 패턴을 그대로 복제하면 된다.

**설계**:
- 매 1분 실행(`ArrivedTransitionScheduler`와 동일 주기)
- `READY` 상태 + `departureAlarmTime <= now()`인 Journey/Participant를 조회
- **GPS 좌표 없이** 상태만 `DEPARTING`으로 벌크 업데이트 — DEPARTING은 원래 새 앵커를 안 만들고 READY 앵커를 그대로 재사용하는 구조라(`journey-state-machine.md` §②), 새 좌표 없이도 전환 자체는 정확함
- 전환된 유저들에게 FCM Data 발송(예: `sync_event: departing_transition`) → 앱이 신호를 받으면 READY용 지오펜스(500m/100m)를 내리고 DEPARTING용 지오펜스(300m/100m)를 등록. 앵커 좌표는 서버가 새로 안 보내도 됨 — 클라이언트가 READY 진입 시 자신이 직접 찍었던 좌표를 로컬에 이미 갖고 있음

**왜 폴링형 스케줄러가 `TaskScheduler.schedule(Instant)` 방식보다 나은가**: 후자(정확한 시각에 1회 실행 예약)는 더 "정밀"해 보이지만, `departureAlarmTime`이 READY 동안 앵커 500m 재계산마다 여러 번 바뀌므로 값이 바뀔 때마다 기존 예약을 취소하고 새로 거는 로직이 별도로 필요하고, 서버 재시작 시 인메모리 예약이 통째로 날아가서 부팅 시 DB 재스캔 복구 로직까지 얹어야 한다. 반면 폴링은 매 사이클 DB의 현재 값을 다시 읽으므로 값 변경·재시작 둘 다 신경 쓸 필요 없이 자동으로 self-healing된다. 1분 오차는 실사용에 무해함(로컬 알림이 이미 정확한 시각에 별도로 울리므로, 이 스케줄러는 서버 DB 상태/지오펜스 동기화용일 뿐).

**비용 비교(왜 이 트레이드가 유리한가)**: 지금 방식(클라이언트 30초 GPS 폴링)은 사용자 수만큼 분산되는 배터리 비용이고, `High Accuracy` GPS는 모바일에서 가장 배터리를 많이 먹는 축이라 READY 상태가 몇 시간 이어지면 그만큼 계속 든다. 새 스케줄러는 인덱스 걸린 컬럼에 대한 쿼리 한 번을 사용자 수와 무관하게(거의) 고정 비용으로 서버가 1분마다 부담하는 것 — 분산된 배터리 비용을 중앙화된 아주 작은 서버 비용으로 바꾸는 거래라 압도적으로 유리하다.

## 지오펜스 등록 시점의 일반 원칙 — 등록 직전 GPS 1회 확보

지오펜스를 등록하는 순간 이미 그 경계 안/밖에 있으면, 그 방향의 이벤트(예: 이미 안에 있는데 ENTER)가 다시 발생하지 않는다(버그41과 동일 클래스의 문제). 이를 피하려면 **지오펜스를 등록/재등록하는 모든 순간, 그 직전에 실제 위치를 한 번 확인한 뒤 결과에 맞는 지오펜스를 등록**해야 한다.

- **READY 진입 시점**: 이미 이 원칙대로 설계돼 있다(§1 "앵커 확정 문제" 참고) — 좌표 확보 → `/location` 호출 → 응답으로 실제 상태(READY 유지 vs 즉시 NEARDEST) 확인 → 그에 맞는 지오펜스 등록.
- **NEARDEST↔READY 전환**: 지오펜스 이벤트 자체가 `/location` 호출을 유발하므로 좌표가 자연스럽게 딸려온다 — 별도 조치 불필요.
- **READY→DEPARTING(시간 트리거)**: 위 `DepartingTransitionScheduler`가 발동하는 순간엔 최신 GPS가 자연스럽게 딸려오지 않는다. 다만 DEPARTING은 새 앵커를 안 만들고 READY 앵커를 그대로 쓰므로, 등록 시점에 "이미 300m 밖에 나가 있는" 경우만 주의하면 됨(READY 동안 500m를 안 넘었다면 애초에 300m 안에 있을 수밖에 없어 실제로는 문제되지 않음).

**참고 — 지금 코드는 이 원칙을 부분적으로만 따르고 있음**: `alarmService.ts`(포그라운드)의 `poll()`은 이미 시작 시 `Location.getCurrentPositionAsync()`로 단발 확보 후 `/location`을 호출하지만, 그 후 `scheduleNextPoll()`로 계속 폴링을 이어간다. `backgroundAlarmTask.ts`(새벽4시 FCM)도 `startLocationUpdatesAsync`(반복 폴링 등록)를 호출하는데 첫 틱이 결과적으로 `/location`을 부르긴 하지만 이후에도 폴링이 안 멈춘다. 즉 "최초 1회 확보" 자체는 이미 있지만 "그걸로 끝내고 지오펜스로 전환"하는 부분이 없다 — 지오펜싱 도입 시 이 두 파일의 흐름을 "1회 확보 → 결과 확인 → 지오펜스 등록 → 폴링 중단"으로 명시적으로 바꿔야 한다.

## 새로 설계해야 하는 것들 (그냥 갈아끼우면 안 되는 부분)

### 1. 앵커 확정 문제
지오펜스를 등록하려면 중심 좌표가 있어야 하는데, 새벽 4시 READY 전환 직후엔 아직 GPS를 한 번도 안 찍은 상태다(현재도 동일 — `docs/spec/journey-state-machine.md`의 "최초 좌표 수신 시 → 앵커 저장"). 그러므로 흐름은:

```
새벽 4시 FCM 웨이크업 → GPS 1회 획득(현재처럼 폴링 시작이 아니라 단발성 fix) → 그 지점을 앵커로 저장(기존과 동일)
→ 그 지점 기준 500m 반경 지오펜스 등록 → 대기(폴링 없음)
```

`backgroundAlarmTask.ts`가 하는 일이 "GPS 폴링 시작"에서 "위치 1회 확인 + 지오펜스 등록"으로 바뀐다. 오늘 이 파일에 넣었던 interval 동적 계산 로직(`getMinDesiredIntervalMs()`)은 이 설계에서 쓸 데가 없어져서 롤백했다(아래 "2단계 롤백" 참고).

> **구현 시 단순화(2026-08-17)**: 위 "GPS 1회 획득 → 지오펜스 등록, 폴링 없이"를 `backgroundAlarmTask.ts`에 직접 넣는 대신, **기존 폴링 응답 처리 지점(`alarmService.ts`/`backgroundLocationTask.ts`)에 READY 분기를 추가하는 더 단순한 방식으로 구현했다** — DEPARTING/MOVING/NEARDEST와 완전히 동일한 패턴. `backgroundAlarmTask.ts`(4시 FCM 웨이크업)는 그대로 `startGpsPolling()`을 부르고, 그 첫 네이티브 GPS 틱(기존 로직 그대로, 최대 수십 초 이내)이 `/location`을 호출해 READY를 확인하는 순간 지오펜스로 전환된다. 이상적인 설계(즉시 단발 fix)보다 최초 등록이 한 폴링 주기(보통 30초 이하)만큼 늦지만, 코드 변경 범위가 훨씬 작고 이미 검증된 패턴을 재사용해 리스크가 낮다. 필요시 나중에 진짜 "1회 확보" 방식으로 최적화 가능.

### 2. NEARDEST의 "스쳐지나간 케이스" 처리
`journey-state-machine.md` §③에 이미 있는 로직: NEARDEST 상태에서 100m를 다시 벗어나고 `P < Q`(아직 출발 알람 전)면 READY로 복귀해야 한다. 지오펜스는 ENTER/EXIT 둘 다 등록 가능하므로, 100m 지오펜스에 EXIT 이벤트도 같이 등록해서 이 케이스를 커버해야 한다(ENTER만 등록하면 이 반전 로직이 빠짐). **EXIT를 놓쳤을 때의 리스크와 하이브리드(저빈도 백업 폴링) 설계는 위 "NEARDEST 하이브리드 설계" 참고.**

> **구현 완료(2026-08-17)**: READY 자신의 목적지 100m 지오펜스는 ENTER만 등록한다(EXIT 불필요) — NEARDEST 진입이 확정되는 즉시 `exitReadyGeofenceMode()` + 기존 `nearDestGeofenceTask.ts`의 `enterNearDestGeofenceMode()`로 핸드오프하고, "스쳐지나간 케이스" EXIT는 이미 구현·검증된 NEARDEST 자신의 지오펜스(변경 없음)가 그대로 처리한다. 신규 코드 최소화 목표대로 재사용됨.

### 3. 버그41과의 상호작용 — 구현 구조상 자동 해소됨
버그41(막차모드 귀가 여정, 목적지 700m 이내에서 새벽4시 최초 계산 시 오작동, `BUGS.md` 참고)의 기존 수정 설계는 "폴링 기반 `/location` 호출"을 전제로 짜여 있다. 지오펜싱 도입 시 "GPS 1회 획득 → 앵커 확정" 단계에서 이미 목적지 500m/100m 이내라면 지오펜스를 등록하는 의미가 없어지므로(이미 안에 있으니 EXIT 이벤트가 다시 안 옴), 이 케이스를 지오펜싱 설계 단계에서 함께 다시 짜야 한다(위 "지오펜스 등록 시점의 일반 원칙" 참고).

> **구현 결과 재검토(2026-08-17)**: 실제 구현 구조상 이 우려가 발생하지 않는다 — READY 지오펜스(앵커 500m·목적지 100m)는 항상 **직전에 확인한 `/location` 응답이 이미 READY임을 전제로만** 등록된다(NEARDEST/DEPARTING이면 그 상태로 먼저 핸드오프되고 READY 지오펜스 등록 자체가 안 일어남). 즉 "이미 100m 이내인데 100m ENTER 지오펜스를 등록"하는 상황 자체가 코드 구조상 생길 수 없다 — 등록 시점의 좌표가 바로 그 판정에 쓰인 좌표이기 때문. 버그41 자체(막차 탐색 창 관련 로직)는 이 지오펜싱 작업과 무관하게 여전히 별도 사안으로 남아있음 — `BUGS.md` 확인.

### 4. 버그29(P>=Q 시간 기반 체크) — `DepartingTransitionScheduler`로 해결 설계 확정
READY→DEPARTING 전환 조건 중 하나(`P >= Q`, 즉 출발 알람 시각 도달)는 순수 시간 기반이라 위치 이동과 무관하다. 이 트리거의 구체적인 해결 설계는 위 "READY→DEPARTING 시간 트리거" 섹션에 확정해뒀다(신규 서버 스케줄러 + FCM Data, 별도 클라이언트 네이티브 모듈 불필요).

### 5. 버그34는 최종적으로 착수 불필요로 확정, 버그40은 구현·검증까지 완료
자가용(DRIVING) 모드가 "정지 상태에서 재계산 안 됨" 문제(버그34)는, 재검토 결과 **기존 폴링 방식에서도 이미 발생하지 않던 문제였다**(재계산 트리거가 "500m 이동"이지 "시간 경과"가 아니므로, 가만히 있으면 폴링 빈도와 무관하게 재계산 자체가 없었음). 그리고 버그40(2-pass future API)이 실측 6개 시나리오 전부 완료(절대 오차 평균 ~6.6%, 최악 -13.8%, 리드타임 길어져도 오차 안 커짐) 후 `gonow-flask`에 실제 구현·EC2 검증까지 끝났다(상세 근거는 `docs/history/resolved-bugs.md`의 "버그40"/"버그34" 항목 참고). 지오펜싱 도입으로 재확인 빈도가 줄어드는 것 자체가 큰 리스크가 아니라고 최종 확정 — 버그34는 착수하지 않는다.

## 버그3/버그8 수정(1단계+2단계) 전면 롤백 사유

버그3/버그8 수정을 `backgroundLocationTask.ts`(1단계, 앱을 연 적 있는 상태에서 백그라운드 전환)와 `backgroundAlarmTask.ts`(2단계, 새벽4시 FCM 헤드리스 웨이크업) 양쪽에 적용하고 1단계는 실기기 검증까지 마쳤으나, 2026-08-11에 **둘 다 전면 롤백**함. 사유 두 가지:

1. **2단계**: 지오펜싱 도입 시 이 파일이 하는 일 자체가 "폴링 시작"에서 "위치 1회 확인 + 지오펜스 등록"으로 바뀔 예정이라 이 수정은 무의미해질 것으로 판단.
2. **1단계(재검토로 새로 발견)**: 기존 30초 고정 덕분에, 백그라운드에서도 의도치 않게 항상 자주(30초) 확인되고 있었던 게 버그29(`departureAlarmTime` 임박 반영 못 함) 증상을 완화해주고 있었음. "서버가 알려준 실제 값(최대 300초)"으로 바꾸면 배터리는 아끼지만 백그라운드에서 상태 전환 감지가 최대 300초까지 늦어질 수 있어 버그29의 영향 범위가 오히려 넓어짐 — 배터리 효율보다 반응 정확도를 우선하기로 하고 전면 롤백.

결과적으로 `backgroundLocationTask.ts`/`alarmService.ts`/`backgroundAlarmTask.ts` 전부 원래(수정 전) 상태로 복원됨. 검증까지 마쳤던 설계(`getMinDesiredIntervalMs()`/`applyNewInterval()`)는 **MOVING 상태 전용으로 범위를 좁혀서** 지오펜싱 구현과 함께 재도입하는 게 목표 — MOVING은 interval이 원래 짧게(30~120초) 계산돼서 위 2번 트레이드오프의 심각도가 낮음. 상세 구현 내용은 `docs/history/resolved-bugs.md`의 "버그3/버그8" 항목 참고.

## 오늘 테스트하며 얻은 교훈 (지오펜싱 구현 시 재활용)

- **`adb shell am force-stop`으로 테스트하지 말 것** — 앱을 "정지 상태"로 만들어서 FCM 자체를 차단해버림. 일반 스와이프로 종료해야 정상적인 "앱 죽음" 상태를 재현할 수 있음.
- **dev client는 백그라운드/헤드리스 테스트에 불리하다(2026-08-13 실기기로 확정, 더 이상 가설 아님)** — dev-client는 일부 JS 모듈을 앱 시작 시 전부 들고 있는 게 아니라 필요할 때 PC의 Metro 서버(`127.0.0.1:8081`, USB `adb reverse`로 터널링)에서 그때그때 받아온다. USB를 뽑고 밖에서 테스트하면 이 터널이 끊겨서, `nearDestGeofenceTask.ts`의 `fallbackToPolling()`이 쓰는 동적 import(`await import('@/src/services/alarmService')`)가 `LoadBundleFromServerRequestError`로 실패하고 이 예외가 안 잡혀서 `NEARDEST-GEOFENCE-TASK` 전체가 죽는 게 실기기로 재현됐다. **처음부터 preview/production APK(`eas build --profile preview` 또는 `expo run:android --variant release`로 로컬 빌드)로 테스트할 것** — 이런 빌드는 모든 JS가 빌드 시점에 정적으로 번들링돼 있어 이 실패 자체가 발생하지 않는다. 동적 import 자체도 실패 시 태스크를 죽이지 않고 백그라운드 폴링 경로로 폴백하도록 방어 코드를 추가해뒀다(`docs/history/resolved-bugs.md`의 "2026-08-13" 항목 참고).
- **기기 배터리 최적화 설정이 결과에 영향을 줄 수 있음** — 테스트 기기에서 GoNow 앱의 배터리 최적화 예외를 미리 켜두고 테스트할 것.
- **`adb logcat -c`로 버퍼를 비우고 새로 캡처해야 "방금 무슨 일이 있었는지"를 정확히 볼 수 있음** — 누적된 로그를 재활용하면 시점이 헷갈림.

## 구현 순서 제안

착수 순서와 그 근거는 위 "상태별 난이도 분석 및 착수 순서" 표를 따른다 — **NEARDEST → DEPARTING → READY** (빈도·심각도가 아니라 신규 설계 필요량 기준으로 재확정, 2026-08-11).

1. **NEARDEST** — 서버 변경 없음, 재센터링 없음, 시간 트리거도 기존 서버 분기가 이미 처리. **완료(2026-08-12), 실기기 검증까지 마침.** FGS 생명주기 정책의 승격 로직 버그도 독립 네이티브 모듈 도입으로 완전히 해소됐다(2026-08-13, 위 "expo-location의 제약" 섹션 참고) — DEPARTING 착수 전 별도로 더 고칠 게 없음.
2. **DEPARTING** — 경계 2개 다 고정, 재센터링 없음. NEARDEST에서 검증한 "지오펜스 등록→콜백→기존 `/location` 재사용" 패턴을 그대로 재사용. **완료(2026-08-17), 실기기 검증(개인/귀가/그룹) + 커밋 완료(`75aa11f`).** 계획엔 없었으나 검증 중 MOVING 보조 지오펜스(폴링과 병행)도 같은 세션에서 함께 구현·검증·커밋됨 — 위 "범위" 표 참고.
3. **READY** — 앵커 재센터링 + `P >= Q` 시간 트리거용 신규 인프라가 필요해 가장 마지막. **구현 완료(2026-08-17)** — 서버 `DepartingTransitionScheduler`(신규) + 프론트 `readyGeofenceTask.ts`(신규, 앵커 500m EXIT마다 재센터링 + 목적지 100m ENTER로 NEARDEST 핸드오프) + `backgroundAlarmTask.ts`(`sync_event: departing_transition` 수신 시 캐시된 앵커로 DEPARTING 지오펜스 핸드오프). 실기기 검증 전. 플랫폼 분기는 별도 디스패처 모듈 없이 각 진입점(`alarmService.ts`/`backgroundLocationTask.ts`)에 `Platform.OS === 'android'` 인라인 체크로 처리(아래 "미결정 사항" 참고 — Phase 1/2엔 아직 이 체크가 없음, 알려진 격차).
4. 각 단계마다 **preview APK로 실기기 검증 후 다음 단계** — 한 번에 여러 상태를 다 바꾸고 나서 테스트하면 오늘처럼 원인 특정이 어려워짐.

## 미결정 사항 (착수 시점에 다시 논의)

- ~~플랫폼 분기 디스패처의 구체적 설계~~ → **결정 및 전체 적용 완료(2026-08-17)** — 별도 디스패처 모듈 없이 `alarmService.ts`/`backgroundLocationTask.ts`의 상태 분기 진입점마다 `Platform.OS === 'android'` 인라인 체크로 처리(iOS는 자동으로 기존 폴링 분기로 빠짐). 처음엔 READY에만 있었으나, NEARDEST/DEPARTING/MOVING에도 동일하게 소급 적용해 4개 상태 전부 통일됨 — 지금은 안드로이드만 실사용 중이라 동작 변화는 없고, iOS 빌드 착수 시 안전망 역할.
- 버그41을 지오펜싱 이전에 독립적으로 먼저 고칠지, 지오펜싱과 함께 재설계할지
- ~~NEARDEST 하이브리드의 백업 폴링 정확한 주기값~~ → 하이브리드 자체를 채택 안 함(위 "NEARDEST 하이브리드 설계" 갱신 참고), 더 이상 미결정 아님
- ~~`docs/spec/journey-state-machine.md`의 "GPS는 지오펜싱 대신 주기적 폴링 방식을 사용한다"는 서술(10행)~~ → **갱신 완료(2026-08-13)** — NEARDEST/안드로이드 예외를 명시하도록 수정.
- ~~FGS 승격 로직 버그 수정~~ → **구현·배포 완료(2026-08-12), 이후 독립 네이티브 모듈로 재대체(2026-08-13)** — 처음엔 `LOCATION_FGS_ACTIVE_KEY`로 봉합했다가, 같은 날 이후 세션에서 `modules/foreground-service` 독립 모듈 도입으로 "승격" 개념 자체를 없앴다. 위 "expo-location의 제약" 섹션 참고.
- ~~(신규, 2026-08-13) NEARDEST 재진입 시 도착 확인 알림이 최대 5분 지연~~ → **원인 규명 + 해결 완료(2026-08-13)** — `DESIRED_INTERVALS_KEY`(서버가 지시한 폴링 주기)가 EXIT 시점에 안 지워지던 버그, 이어서 같은 날 코드 리뷰에서 여러 헤드리스 태스크가 이 키를 잠금 없이 동시에 건드릴 수 있는 경쟁 조건까지 추가로 발견해 `withIntervalsLock`으로 보강. `docs/history/resolved-bugs.md` "2026-08-13" 항목 참고.
- ~~FCM 고우선순위 예외로 헤드리스 FGS 시작 검증~~ → **실기기 검증 완료, 가설 기각(2026-08-12)** — 위 "FGS 생명주기 정책 > 핵심 제약" 섹션에 결과 기록.
- **(신규, 2026-08-12) 반복 알람이 ARRIVED를 지나도 FGS를 유지하도록 `AlarmRunner` 생명주기 재설계** — 현재 최우선 미구현 TODO. 위 "FGS 생명주기 정책" 섹션의 "남은 구멍 — 반복 알람" 참고.
- **(신규) SCHEDULED/READY 등 급하지 않은 대기 구간의 GPS 폴링 주기 최적화** — FGS는 상시 유지하되 `timeInterval`을 대기 상태에 맞게 늘려서(예: 몇 시간) 배터리 비용 절감. 구체적 주기값은 실측 후 확정. "FGS는 알람 생성 시점부터 상시 유지"로 정책이 더 넓어졌으니 이 최적화의 중요도도 더 커짐.
- ~~백그라운드 `/location` 호출이 25~120초씩 불규칙 지연되는 문제~~ → **원인 규명 + 해결 완료(2026-08-13)** — `docs/history/resolved-bugs.md` "2026-08-13" 항목 참고. 더 이상 미결정 아님.
- **(신규, 2026-08-13) 지오펜스 등록/해제 stop→start 경쟁 상태(Bug 2)** — `GeofencingClient.addGeofences()`/`removeGeofences()`가 둘 다 fire-and-forget이라, 짧은 시간에 반복 등록/해제하면 순서가 꼬일 이론적 여지가 남아있음. 오늘 겪은 지연의 실제 원인은 아니었던 것으로 확정됐지만 근본 수정은 안 함 — 낮은 우선순위로 남겨둠.
- **(신규, 2026-08-13) `patches/` 디렉토리 도입** — `patch-package`로 `expo-location`(`GeofencingTaskConsumer.kt`, 등록 성공/실패 확인 추가)과 `expo-task-manager`(`TaskManagerUtils.java`, `setExpedited(true)`)에 네이티브 패치를 적용 중. `npm install` 시 `postinstall` 스크립트로 자동 재적용되지만, 두 패키지를 버전업할 때는 패치가 깨질 수 있으니 `patch-package` 경고를 반드시 확인할 것.
- **(신규, 2026-08-13) DEPARTING/READY 전환 후 `fallbackToPolling()`의 "최후 안전망" 재설계** — 지금 NEARDEST의 `fallbackToPolling()`은 좌표 확보나 `/location` 호출이 실패하면 READY/DEPARTING의 폴링 로직으로 무기한 되돌리는 방식인데, 이건 "READY/DEPARTING이 아직 폴링 기반"이라는 전제에 기대고 있다. DEPARTING/READY까지 지오펜싱으로 전환되면(MOVING만 폴링으로 남음) 이 전제가 깨지므로, 실패 시 "무기한 폴링 전환" 대신 **만료시각이 있는 짧은 재시도(예: 5분간 짧은 간격으로 재시도) → 성공하면 즉시 지오펜스 재등록으로 복귀, 끝까지 실패하면 기존 서버 스케줄러 안전망(목표시각+1시간 초과 시 자동 ARRIVED)에 위임**하는 방식으로 바꾸는 게 지금까지의 self-healing 설계 철학(`DepartingTransitionScheduler`가 정확한 1회 예약 대신 매분 폴링형을 택한 이유와 동일 논리)과 맞다. 다만 이건 좌표 확보와 서버 호출이 **동시에** 실패해야 발동하는, 실측상 매우 낮은 확률의 최후 방어선이라(EXIT 콜백 자체가 이미 위치 계산의 결과물이라 시스템 위치 캐시가 거의 항상 최신 상태) 지금 당장 구현할 필요는 없음 — DEPARTING/READY 착수 시점에 재논의. 참고로 재시도 루프는 헤드리스 JS 컨텍스트가 태스크 종료 시 사라지는 구조라(위 "헤드리스 컨텍스트는 서로 완전히 분리돼 있다" 참고) 단순 `setTimeout` 반복으로는 못 만들고, 결국 기존 폴링 태스크를 "만료시각이 찍힌 임시 모드"로 잠깐 재사용하는 형태가 될 가능성이 높음.

- ~~NEARDEST에도 key-lock 소급 적용할지~~ → **적용 완료(2026-08-17)** — 위 "지오펜스 콜백 신뢰성" 섹션 참고. 4개 지오펜스 태스크 전부 `withKeyLock`으로 통일됨.

> 버그29(P≥Q 시간 트리거)는 위 "READY→DEPARTING 시간 트리거" 섹션에서 `DepartingTransitionScheduler`로 설계 확정됨 — 더 이상 미결정 아님.
