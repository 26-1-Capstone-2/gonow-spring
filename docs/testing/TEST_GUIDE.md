# GoNow 백그라운드/FCM 테스트 가이드

마지막 업데이트: 2026-06-04

---

## 커밋 이력

| 커밋 | 브랜치 | 내용 |
|------|--------|------|
| `b6c98ed` | fix | GPS 폴링 좀비 버그 전면 수정 및 포그라운드/백그라운드 폴링 안정화 |

---

## 1. Metro(터미널) 기본 원칙

`npx expo start`는 JS 번들을 폰에 전달하는 웹 서버 역할만 한다.
**앱이 종료/재실행/크래시 나도 터미널은 건드리지 않는다.**

| 상황 | 터미널 행동 |
|------|-----------|
| 앱 크래시 | 아무것도 하지 않음 |
| 앱 스와이프 킬 후 재실행 | 아무것도 하지 않음 |
| 코드 수정 후 반영 | `r` 입력 (JS만 재시작) |
| 포트 점유 오류 등 이상 발생 | 터미널만 종료 후 재시작 |

---

## 2. `r` 리로드의 부작용 — 반드시 알아야 함

`r` (또는 폰 흔들기 → Reload)는 **JS 엔진(Hermes)만 재시작**한다.
네이티브 레이어는 재시작하지 않는다.

**결과:**
- JS의 `alarmService.runners` → 초기화됨 (size = 0)
- OS의 `startLocationUpdatesAsync` (백그라운드 위치추적) → **살아있음**
- 상단바 "GoNow 알람 실행 중" 알림 → **그대로 떠있음**

이 상태에서 테스트하면 JS는 폴링 안 하는데 상단바 알림은 뜨고, 백그라운드 위치추적은 30초마다 발화하는 **좀비 상태**가 된다.

---

## 3. 상황별 올바른 테스트 방법

### ① 코드 수정 후 FCM/백그라운드 테스트

```
코드 수정
  → 터미널 r (JS 리로드)
  → 폰에서 앱 스와이프 킬 (완전 종료)
  → 상단바 알림 사라졌는지 확인
    ├─ 사라짐 → 앱 아이콘으로 재실행 → 테스트 시작
    └─ 아직 떠있음 → 폰 재부팅 → 앱 실행 → 테스트 시작
```

> **주의**: 스와이프 킬만으로 OS 레벨 위치추적이 안 꺼질 수 있다.
> 상단바 알림이 남아있으면 반드시 폰을 재부팅해야 깨끗한 초기 상태.

### ② 앱 크래시 후 복구 테스트 (버그2)

```
앱 크래시 발생
  → 앱 아이콘으로 재실행
  → _layout.tsx init()이 AsyncStorage 초기화함 (이전 세션 ID 삭제)
  → 로그인 화면이면 로그인 → startReadyAlarms() 재호출 → READY 알람 복구
```

> **주의**: 크래시 후 재실행 시 AsyncStorage에 이전 데이터가 남아 복구되는 게 아니다.
> `init()`에서 ACTIVE_JOURNEYS_KEY, ACTIVE_APPOINTMENTS_KEY를 빈 배열로 초기화한다.
> 복구는 서버 `getAlarms` API 재조회로 이루어진다.

### ③ 백그라운드 FCM Data 수신 테스트 (새벽 4시 시뮬레이션)

```
앱 완전 종료 또는 백그라운드 상태로 두기
  → 서버에서 FCM Data 전송 (gps-sync.http 또는 scheduler.http)
  → Metro 로그 확인 불가 (백그라운드 로그는 서버 로그로 확인)
  → 서버 로그에서 /location 호출 확인
  → 앱 포그라운드로 복귀
  → Metro에 [AppState] active 로그 + [포그라운드] /location 로그 확인
```

### ④ 포그라운드 ↔ 백그라운드 전환 테스트

```
앱 실행 + 알람 READY 상태 확인
  → Metro에 [alarmService.start] 로그 확인
  → Metro에 [startBackgroundLocationUpdates] 완료 로그 확인
  → 홈 버튼으로 백그라운드 전환
    → Metro 로그 끊김 (JS 일시정지)
    → 서버 로그에서 30초 간격 /location 확인 (백그라운드 태스크)
  → 앱 다시 포그라운드로 복귀
    → Metro에 [AppState] active 로그
    → Metro에 [stopBackgroundLocationUpdates] 로그
    → Metro에 [포그라운드] /location 로그 재개
```

---

## 4. Metro 로그로 체크할 항목

정상 흐름에서 반드시 보여야 하는 로그 순서:

### 앱 시작 ~ 알람 폴링 시작
```
[BACKGROUND_ALARM_TASK] 등록 성공
[AppState] 상태 변경: active            ← 앱 최초 실행
[alarmService.start] 시작 — type:... id:...
[alarmService.handOff] 백그라운드 인계 — key:...
[startBackgroundLocationUpdates] 시작
[startBackgroundLocationUpdates] 완료 — 상단바 알림 표시됨
[alarmService.start] 완료 — AsyncStorage 등록 완료, 폴링 시작
[포그라운드] /location 호출 — journeyId:N (lat, lng) interval:30s
[포그라운드] /location 응답 — journeyId:N status:READY interval:120
[포그라운드] interval 갱신 — journeyId:N 30s → 120s
```

### 상태 전이
```
[alarmService] 상태전이 READY → DEPARTING — journeyId:N
[alarmService] 단계별 알람 스케줄 시작 — journeyId:N preparationTime:10분
[alarmService] 단계별 알람 2~4단계 등록 완료
```

### 백그라운드 전환 후 복귀
```
[AppState] 상태 변경: background
[AppState] 상태 변경: active
[AppState] active → stopBackgroundLocationUpdates 호출
[stopBackgroundLocationUpdates] 완료 — 상단바 알림 제거됨
[AppState] active → startReadyAlarms 호출
[startBackgroundLocationUpdates] 이미 실행 중 — skip   ← isRunning 체크
  또는
[startBackgroundLocationUpdates] 시작                   ← 재시작 필요했던 경우
[포그라운드] /location 호출 재개
```

### 이상 징후 — 이게 보이면 버그
```
[AlarmManager.start] 기존 runner 교체    ← 중복 start (좀비 가능성)
[AlarmManager.start] runners 총 N개      ← N > 1이면 좀비 누적 중
[포그라운드] GPS 위치 획득 실패          ← GPS 권한 or 하드웨어 문제
[포그라운드] /location 호출 실패         ← 서버 연결 문제
[백그라운드] 서버 HTTP 4xx → ID 제거    ← 삭제된 알람 정리 (정상)
[백그라운드] 네트워크 오류 → 다음 주기 재시도  ← 간헐적이면 무시, 반복되면 네트워크 문제
[FCM] 수신 — data keys: {}              ← FCM은 왔는데 data가 비어있음
```

---

## 5. 백그라운드 로그 확인법

백그라운드 상태에서는 Metro 로그가 출력되지 않는다.
백그라운드 폴링 확인은 **서버 로그**로만 가능하다.

```bash
# EC2 서버 로그 실시간 확인
# (배포된 서버에서 /location 호출 타임스탬프 확인)
```

또는 `adb logcat`으로 Android 네이티브 로그 직접 확인:
```bash
adb logcat | grep -E "BackgroundLocation|location"
```

---

## 6. 테스트 완료 현황 (2026-06-04)

| 테스트 항목 | 결과 | 비고 |
|------------|------|------|
| 앱 시작 시 초기 상태 (좀비 없음) | ✅ | |
| 포그라운드 폴링 (/location 주기적 호출) | ✅ | |
| 포그라운드 → 백그라운드 전환 시 폴링 유지 | ✅ | 30초마다 백그라운드 태스크 발화 확인 |
| 백그라운드 → 포그라운드 복귀 시 인계 | ✅ | alarmService 포그라운드 폴링 재개 확인 |
| 빠른 백그라운드↔포그라운드 반복 | ✅ | |
| 알람 스위치 OFF → 폴링 중단 | ✅ | 개인 여정 |
| 알람 스위치 ON → 폴링 재개 | ✅ | 개인 여정 |
| 앱 재실행 시 OFF 상태 유지 | ✅ | is_active 체크 추가 |
| 알람 삭제 → 폴링 중단 | ✅ | 개인 여정 |
| 그룹 알람 삭제 → 모든 폴링 중단 + 상단바 알림 제거 | ✅ | |
| FCM Data 수신 → 폴링 시작 (포그라운드) | ✅ | journeyId=146, appointmentId=138 |
| FCM Data 수신 후 백그라운드 바톤 터치 | ✅ | 포그라운드↔백그라운드 전환 정상 |
| FCM Data 수신 → 폴링 시작 (백그라운드) | ✅ | 첫 /location 1회 호출 확인. 이후 지속 폴링은 Foreground Service 없어 Android가 제한 — 사용자가 앱 열면 정상화 |
| 앱 재실행 후 로그인 → READY 알람 즉시 폴링 시작 | ✅ | LoginScreen.tsx에서 직접 getAlarms 호출, startBackgroundLocationUpdates 1번만 완료 |
| DEPARTING → 출발 알람 발송 | ✅ | 단계별 알람 정상 수신 확인 |
| 방장 날짜 수정(내일) → 양쪽 폴링 중단 | ✅ | 폰A 직접 중단, 태블릿B FCM Data 수신 후 중단 |
| 방장 날짜 수정(오늘) → 양쪽 폴링 재시작 | ✅ | 폰A 직접 재시작, 태블릿B FCM Data 수신 후 재시작 |
| NEARDEST → 도착 확인 알람 + 확인 버튼 → ARRIVED | ✅ | 알람 수신 + 확인 버튼 후 추적 대상에서 제거 확인 |

---

## 7. 테스트 전 체크리스트

- [ ] 상단바에 "GoNow 알람 실행 중" 알림 없는지 확인 (없어야 깨끗한 상태)
- [ ] Metro 터미널 살아있는지 확인
- [ ] 서버(Spring + Flask) 실행 중인지 확인
- [ ] 테스트용 알람이 오늘 날짜 + READY 상태인지 확인
- [ ] 폰 배터리 최적화에서 GoNow 앱 제외됐는지 확인 (설정 → 배터리 → 앱별 최적화)
