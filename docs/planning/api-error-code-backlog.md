# API 에러 코드 도입 백로그

## 배경

`ApiResult`는 실패 응답을 `{success, message, data}`로만 내려준다. `GlobalExceptionHandler`가 예외 타입별로 HTTP 상태 코드는 분기하지만, **같은 상태 코드(400) 안에서 "정확히 어떤 실패인지"를 구분할 수 있는 값은 `message` 문자열뿐**이다.

`message`는 원래 사람이 읽는 문구라 언제든 자유롭게 바뀔 수 있어야 정상인데, 지금은 프론트 일부 코드가 이 문구를 그대로 분기 조건으로 쓰고 있다 — 실무에서 흔히 피하는 패턴(에러 코드 대신 표시 문구로 로직 분기)이다.

## 현재 이 패턴을 쓰는 곳

- `GoNow_Fronted/src/screens/auth/LeaveTimeSetupScreen.tsx`의 `EMAIL_VERIFICATION_REQUIRED_MESSAGE` — `MemberService.signUp()`이 던지는 `"이메일 인증을 먼저 완료해주세요."` 문구와 정확히 일치해야만, 이메일 인증 유효시간(10분) 만료 시 사용자를 `email-verify` 화면으로 자동 이동시키는 분기가 작동한다.

## 리스크

백엔드에서 이 메시지 문구를 조금이라도 수정하면(오타 수정, 말투 순화 등) 프론트의 문자열 비교가 조용히 실패한다. 예외가 터지지는 않고, 그냥 일반 실패 알럿(`else` 분기)으로 조용히 규격이 낮아질 뿐이라 — 배포 후에도 한동안 아무도 눈치 못 챌 수 있는 종류의 회귀다.

## 정석 해결책

응답에 안정적인 에러 코드 필드를 추가하고, 프론트는 `message`가 아니라 이 코드로 분기하도록 바꾼다.

```json
{ "success": false, "code": "EMAIL_VERIFICATION_REQUIRED", "message": "이메일 인증을 먼저 완료해주세요." }
```

### 예상 작업 범위 (지금 당장 하기엔 범위가 큼)

- `ApiResult`에 `code`(nullable) 필드 추가
- `GlobalExceptionHandler`가 처리하는 예외 타입 대부분이 지금 `IllegalArgumentException`/`IllegalStateException` 같은 범용 예외를 재사용 중 — 코드 필드를 채우려면 이 케이스들을 구분할 방법이 필요함(커스텀 예외 타입을 만들거나, 예외 생성 시 코드를 함께 실어 보내는 구조 등)
- 전체 도메인(`member`/`journey`/`appointment` 등)에 흩어진 `IllegalArgumentException`/`IllegalStateException` 호출부를 한 번에 어떻게 할지 방침 결정 필요 — 이번 이메일 인증 케이스 하나만 코드를 넣으면 나머지는 여전히 문자열 매칭에 의존하는 반쪽짜리 상태가 됨
- 프론트 쪽도 `getErrorMessage()`(현재는 message만 추출) 옆에 코드 추출용 헬퍼를 새로 만들고, 기존 문자열 매칭 분기들을 코드 매칭으로 교체

## 언제 재검토할지

- 지금처럼 "특정 실패 사유에 따라 프론트가 다르게 반응해야 하는" 케이스가 하나 더 생기면(즉 이 이메일 인증 케이스가 단발성이 아니라 반복되는 패턴으로 확인되면) 그때 전역 도입을 재검토한다.
- 그전까지는 `LeaveTimeSetupScreen.tsx`의 문자열 상수에 남긴 경고 주석으로 리스크를 명시해두고, 백엔드에서 해당 메시지를 수정할 일이 생기면 그 상수도 함께 고치는 것으로 방어한다.
