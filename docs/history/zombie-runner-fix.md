# 좀비 Runner 버그 수정 가이드

## 증상
- 서버 로그에 같은 journeyId/appointmentId로 `/location`이 동시에 여러 개(6~8개) 찍힘
- 시간이 지날수록 동시 호출 수가 늘어남

## 원인

### 원인 1 — AppState `active` 중복 발화
안드로이드에서 `AppState active` 이벤트는 포그라운드 복귀뿐 아니라
권한 팝업 닫힘, 알림 상호작용, 화면 전환 등으로 **짧은 시간에 여러 번 발화**함.

현재 코드는 `active`마다 무조건 `startReadyAlarms()` 호출 →
READY 알람에 대해 `alarmService.start()` 반복 호출됨.

### 원인 2 — AlarmManager.start() race condition
`alarmService.start()`는 내부에서 `await runner.start()`로 오래 걸림
(AsyncStorage 읽기 + GPS 권한 + HTTP 왕복).

이 대기 시간 중에 같은 key로 `start()`가 또 들어오면,
이전 runner가 완전히 죽지 않고 **좀비로 남음**.

원인 1이 `start()`를 짧은 시간에 여러 번 부르면서 좀비가 누적됨.

---

## 수정 1 — `app/_layout.tsx`

**변경 사항 2가지:**
1. `useEffect` 내부를 `async init()`으로 감싸서 AsyncStorage 초기화에 `await` 추가
2. AppState `active` 이벤트 3초 디바운스 추가

```ts
useEffect(() => {
  const init = async () => {
    // await 필수 — 완료 전 startReadyAlarms 실행 방지
    await stopBackgroundLocationUpdates().catch(() => {});
    await AsyncStorage.setItem(ACTIVE_JOURNEYS_KEY, JSON.stringify([]));
    await AsyncStorage.setItem(ACTIVE_APPOINTMENTS_KEY, JSON.stringify([]));

    requestNotificationPermission();
    setupNotificationCategories();
    Notifications.registerTaskAsync(BACKGROUND_ALARM_TASK)
      .then(() => console.log('[BACKGROUND_ALARM_TASK] 등록 성공'))
      .catch((e) => console.log('[BACKGROUND_ALARM_TASK] 등록 실패:', e));
    Location.requestBackgroundPermissionsAsync().catch(() => {});

    const journeysApi = createJourneysApi();
    const appointmentsApi = createAppointmentsApi();
    const alarmsApi = createAlarmsApi();

    const today = new Date();
    const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;

    const startReadyAlarms = () => {
      alarmsApi.getAlarms(todayStr).then((res) => {
        (res.data ?? []).filter((a) => a.my_status === 'READY').forEach((a) => {
          if (a.alarm_type === 'GROUP' && a.appointment_id != null) {
            if (alarmService.isRunning(undefined, a.appointment_id)) return;
            alarmService.start({ alarmType: 'group', destination: a.dest_name, appointmentId: a.appointment_id });
          } else if (a.alarm_type === 'HOME' && a.journey_id != null) {
            if (alarmService.isRunning(a.journey_id)) return;
            alarmService.start({ alarmType: 'home', destination: a.dest_name, journeyId: a.journey_id });
          } else if (a.alarm_type === 'PERSONAL' && a.journey_id != null) {
            if (alarmService.isRunning(a.journey_id)) return;
            alarmService.start({ alarmType: 'personal', destination: a.dest_name, journeyId: a.journey_id });
          }
        });
      }).catch(() => {});
    };

    startReadyAlarms();

    // active 이벤트 3초 디바운스 — 짧은 시간 내 중복 발화 무시
    let lastForegroundAt = 0;
    const appStateSub = AppState.addEventListener('change', async (nextState) => {
      if (nextState === 'active') {
        const now = Date.now();
        if (now - lastForegroundAt < 3000) return;
        lastForegroundAt = now;
        startReadyAlarms();
      } else if (nextState === 'background') {
        await startBackgroundLocationUpdates().catch(() => {});
      }
    });

    // FCM, notifee 리스너 등 나머지 코드는 그대로 유지 ...

    return () => {
      fcmSub.remove();
      notifSub();
      appStateSub.remove();
    };
  };

  init();
}, []);
```

> ⚠️ `return () => { ... }` cleanup은 반드시 `init()` 밖 `useEffect` 레벨에 있어야 함.
> `init` 안에 넣으면 cleanup이 동작 안 함. 아래처럼 구조 잡을 것:
>
> ```ts
> useEffect(() => {
>   let cleanup = () => {};
>   const init = async () => {
>     // ...
>     cleanup = () => { fcmSub.remove(); notifSub(); appStateSub.remove(); };
>   };
>   init();
>   return () => cleanup();
> }, []);
> ```

---

## 수정 2 — `src/services/alarmService.ts`

`AlarmManager` 클래스에 `isRunning` 메서드 추가:

```ts
// AlarmManager 클래스 안에 추가
isRunning(journeyId?: number, appointmentId?: number): boolean {
  return this.runners.has(this.key(journeyId, appointmentId));
}
```

**위치**: `stopAll()` 메서드 아래에 추가하면 됨.

---

## 수정 후 기대 동작
- `active` 이벤트가 여러 번 와도 3초 내 중복은 무시
- 이미 폴링 중인 알람은 `isRunning` 체크로 재시작 안 함
- 좀비 runner 누적 차단 → 동시 `/location` 호출 1개로 정상화
