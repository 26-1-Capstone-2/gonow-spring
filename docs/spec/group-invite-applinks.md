# 그룹 초대 유니버설 링크 (Android App Links)

카톡 등으로 그룹 초대코드를 공유할 때, 순수 텍스트 복붙 대신 링크 탭 한 번으로 참여 화면까지 이어지게 하는 기능. 2026-08-19 도입. 범위는 "앱이 이미 설치된 유저"로 한정 — 미설치 유저 대응(플레이스토어 폴백, Deferred Deep Link)은 `docs/planning/feature-ideas.md` 아이디어 I로 분리.

## 전체 흐름

```
초대코드 공유 텍스트에 https://gonow-api.uk/join?code=XXXX 포함
                    │
        ┌───────────┴───────────┐
        │                       │
  App Links 인식 O          App Links 인식 X
  (Chrome, 문자, 슬랙,      (카카오톡·인스타그램처럼
   디스코드 등)              자체 인앱브라우저가 가로챔)
        │                       │
        ▼                       ▼
  안드로이드가 서버       인앱브라우저가 실제로
  요청 없이 바로           https://gonow-api.uk/join 요청
  GoNow 앱 실행                  │
        │                       ▼
        │                 InviteRedirectController가
        │                 join.html(브랜드 카드) 응답
        │                       │
        │                       ▼
        │                 페이지 로드 즉시 intent://로
        │                 gonow://join?code=XXXX 강제 호출
        │                       │
        └───────────┬───────────┘
                    ▼
        앱의 딥링크 리스너가 코드 수신
                    │
                    ▼
        daily-alarm 화면 → 그룹 참여 화면
        자동 오픈 + 초대코드 자동입력
        (최종 참여 확인은 사용자가 직접 버튼 눌러야 함)
```

## 백엔드 (`gonow`)

- **`.well-known/assetlinks.json`** (`src/main/resources/static/.well-known/assetlinks.json`, 정적 리소스): 안드로이드 Digital Asset Links 검증 파일. `package_name: com.hyeongwon.gonow` + release 서명 SHA256 지문(preview/production 빌드 프로필이 같은 키스토어를 공유해서 지문 하나만 등록됨, `eas credentials -p android`로 확인). RFC 8615 `/.well-known/` 표준 경로 + 구글 Digital Asset Links API가 하드코딩한 고정 파일명이라 경로 변경 불가.
- **`InviteRedirectController`** (`global/controller/InviteRedirectController.java`): `GET /join` → `forward:/join.html`로 정적 리소스 `join.html`을 포워딩. URL을 `/join.html` 대신 `/join`으로 깔끔하게 유지하려는 목적. **스프링 시큐리티는 내부 forward 디스패치에도 필터를 다시 걸기 때문에, forward 대상인 `/join.html`도 `SecurityConfig`에 함께 permitAll해야 한다** — 하나만 permitAll하면 나머지가 403이 나는 게 이 프로젝트에서 실제로 재현됐던 실수라 특히 기록.
- **`join.html`** (정적 리소스): 브랜드 카드 UI(로고, 안내문, "GoNow 앱으로 열기" 버튼) + 카톡/인스타그램 인앱브라우저 우회용 `intent://` 자동 실행 스크립트 + 카톡/문자 공유 시 링크 미리보기 카드가 뜨도록 하는 OG 메타 태그(`og:title`/`description`/`image`). `og:image`는 `/images/gonow_logo.png`(정적 리소스, 프론트 `assets/images/gonow_logo.png` 원본을 흰 배경 1000x1000 캔버스에 56% 크기로 중앙 배치해 어느 플랫폼이 잘라도 로고가 안 잘리게 재가공한 버전)를 가리킨다.
- **`SecurityConfig`**: `/join`, `/join.html`, `/.well-known/**`, `/images/**` permitAll — 전부 실제 데이터 변경이 없는 정적 콘텐츠라 인증 없이 공개해도 무방하다(그룹 참여라는 실제 액션은 여전히 `POST /api/appointments/join`이 JWT로 보호). 새 도메인/서비스 없이 기존 `gonow-api.uk`(스프링이 이미 서빙 중인 도메인)를 그대로 재사용했다.
- **`intent://` 카톡 우회 기법**: 카카오톡/인스타그램은 링크 탭 시 OS의 App Links 판정을 거치지 않고 자체 인앱 웹뷰로 먼저 가로챈다. `intent://join?code=XXXX#Intent;scheme=gonow;package=com.hyeongwon.gonow;end` 형태의 URI는 패키지명을 명시해서 앱을 강제로 지정 호출하는 방식이라 이 웹뷰들 안에서도 동작한다(토스/배달의민족 등이 쓰는 표준 우회 패턴). 자동 실행 후에도 인앱브라우저 탭 자체는 안 닫힌다(스크립트로 남의 탭을 못 닫는 브라우저 정책) — 그래서 방치된 텍스트 한 줄 대신 브랜드 카드 + 재시도 버튼으로 대체했다.

## 프론트 (`GoNow_Fronted`)

- **`app.json` `android.intentFilters`**: `gonow-api.uk` 도메인 + `pathPrefix: "/join"`, `autoVerify: true`로 Android App Links 등록. **네이티브 매니페스트를 바꾸는 설정이라 `eas update`(JS OTA)로는 반영 안 되고, 새 네이티브 빌드(`eas build` 또는 로컬 `gradlew assembleRelease`)가 필요**하다.
- **`src/utils/inviteDeepLink.ts`**: `https://gonow-api.uk/join?code=...` 형태의 URL에서만 코드를 파싱(`extractInviteCodeFromUrl`). 로그인 전(콜드스타트)에 링크를 받았을 수도 있어 `AsyncStorage`에 코드를 임시 저장(`setPendingInviteCode`)해뒀다가, `daily-alarm` 화면 마운트 시 소비(`consumePendingInviteCode`)한다.
- **`app/_layout.tsx`**: `Linking.getInitialURL()` + `Linking.addEventListener('url', ...)`로 위 https 형태의 URL만 처리해서 코드 저장 후 `router.push('/daily-alarm')`.
- **`app/join.tsx`**: `gonow://join?code=...` 커스텀 스킴(위 `intent://` 우회 경로로 들어오는 경우) 전용 라우트. **이 파일이 왜 별도로 필요한지가 이 기능에서 가장 헷갈리기 쉬운 지점**: expo-router는 `app.json`의 `scheme: "gonow"`를 자체적으로 인식해서, 이 URL을 받으면 **자기 자신의 파일 기반 라우팅으로도 매칭을 시도**한다. `app/join.tsx`가 없으면 "일치하는 라우트 없음"으로 판단해 `+not-found`("Unmatched Route") 화면을 먼저 보여준 뒤, `_layout.tsx`의 수동 리스너가 뒤늦게 `daily-alarm`으로 옮기는 경쟁 상태가 발생해서, 뒤로가기 시 그 `+not-found` 화면이 히스토리에 유령처럼 남았다(실제로 재현·수정됨). `app/join.tsx`가 실제 매칭 라우트가 되어 즉시 `router.replace('/daily-alarm')`하면서 이 문제가 해결된다. (반대로 https 형태는 expo-router가 인식하는 prefix가 아니라서 이 파일과 무관하게 `_layout.tsx`의 수동 리스너만 처리한다 — 두 리스너가 서로 다른 URL 형태를 나눠 맡는 구조.)
- **`app/daily-alarm.tsx`**: 마운트 시 `consumePendingInviteCode()`로 저장된 코드를 읽어 `GroupAlarmSheet`를 `initialMode: 'join'` + `initialInviteCode`로 자동 오픈.
- **`GroupAlarmSheet.tsx`**: `initialInviteCode` prop으로 `view` 상태를 바로 `'join'`으로, `inviteCode` 상태를 코드값으로 초기화 — 기존에 있던 "초대코드 수동 입력" 화면(`이동수단` 선택 + 참여 확인 버튼)을 그대로 재사용하므로, 코드가 채워진 채로 사용자가 이동수단만 고르고 확인 버튼을 누르면 된다.
- **공유 텍스트** (`GroupAlarmSheet.tsx`/`GroupAllAlarmSheet.tsx`의 `shareInviteCode()`): 초대코드만 있던 기존 문구에 `https://gonow-api.uk/join?code=...` 링크를 포함하도록 변경. 코드만 단독 공유하던 `copyInviteCode()`/"복사" 버튼은 링크 공유와 기능이 겹쳐서 삭제(2026-08-19) — 초대코드 자체는 화면에 텍스트로 계속 표시됨.

## 검증 방법

- App Links 인증 상태: `adb shell pm get-app-links com.hyeongwon.gonow` → `gonow-api.uk: verified` 확인.
- 직접 실행 테스트: `adb shell am start -a android.intent.action.VIEW -d "https://gonow-api.uk/join?code=테스트코드" com.hyeongwon.gonow`.
- 카톡 인앱브라우저 경로는 반드시 실제 카톡 채팅으로 링크를 보내서 탭해봐야 재현된다(다른 브라우저로 강제로 열면 App Links 경로를 타서 인앱브라우저 문제를 놓친다).

## 알려진 이슈 이력

- **Google Safe Browsing "위험한 사이트" 오탐**: 새로 등록한 `.uk` 도메인 + `join.html`의 자동 리다이렉트·"앱으로 열기" 버튼 패턴이 피싱 사이트의 전형적인 패턴과 유사하게 탐지된 것으로 추정. Google Search Console에 도메인 소유권 인증(Cloudflare DNS TXT 레코드) 후 "안전한 페이지" 오탐 신고 제출 완료(2026-08-19) — Search Console "보안 문제" 메뉴에서는 "감지된 문제 없음"으로 확인됨. 코드/배포와 무관한 순수 검토 대기 상태.
