# ODsay MCP 서버 활용 가이드 (개발용 도구)

Claude Code에서 ODsay(대중교통 경로 API)를 직접 호출해서 원본 응답을 바로 확인할 수 있는 MCP 서버. **개발자 디버깅 편의 도구이며, GoNow 앱 자체의 런타임과는 무관하다** — 실제 앱은 플라스크(`transit_route.py`)가 ODsay REST API를 직접 호출하고, 이 MCP는 그와 별개로 개발 중 Claude와 대화하면서 ODsay 응답을 빠르게 조회하기 위한 것.

## 왜 유용한가

카카오맵 딥링크([kakao-map-deeplink-spec.md](kakao-map-deeplink-spec.md) 참고)는 화면을 렌더링하는 앱/웹이라 매번 사람이 직접 열어서 눈으로 확인해야 했다. ODsay는 순수 JSON API라서, MCP로 연결해두면 Claude가 직접 호출해서 원본 응답을 바로 분석할 수 있다 — 스크린샷 왕복 없이 "이 출발지-목적지 조합에서 ODsay가 뭘 주는지" 같은 걸 즉시 확인 가능.

## 등록 방법 (Claude Code, local scope)

Claude Desktop 안내(`npx mcp-remote` 브릿지 방식)와 달리, Claude Code는 원격 HTTP MCP 서버를 네이티브로 지원해서 더 간단하게 등록된다.

API 키는 비밀값이므로 채팅에 붙여넣지 말고, 아래 명령어의 `{API_KEY}` 부분만 실제 키로 바꿔서 `!` 접두사로 직접 실행:

```
claude mcp add --transport http odsay-mcp https://mcp.odsay.com/mcp --header "ARO-API-Key: {API_KEY}"
```

- `-s local`(기본값, 생략 가능)로 등록 — 이 프로젝트 로컬 설정에만 저장되고 git에는 안 올라감.
- **`project` 범위로 등록하면 안 됨** — GoNow 저장소는 대회 출품용 공개(Public) 저장소라, `project` 범위는 `.mcp.json`에 저장돼 커밋될 수 있고 그러면 API 키가 그대로 깃허브에 공개됨. 팀원과 공유하고 싶으면 설정 자체가 아니라 "이 명령어를 각자 본인 키로 실행하라"는 안내만 공유할 것.
- 등록 후 Claude Code 세션 재시작 필요할 수 있음.
- `/mcp`로 `odsay-mcp` 연결 상태(connected/authenticated) 확인 가능.

## 사용 시 주의 — API 호출수 공유

MCP로 조회할 때마다 등록한 API 키의 실제 호출수가 차감된다(무료 Basic 플랜 기준 일 1,000회). 이 키는 실제 앱(플라스크)이 쓰는 프로덕션 키와 무관한, 별도 개발용 키를 쓰는 걸 권장 — 같은 키를 쓰면 디버깅용 조회가 앱의 실제 호출 한도를 갉아먹는다.

## 제공 도구 (15개)

경로 탐색부터 역/노선/시간표 조회까지 ODsay의 주요 기능을 커버:

- `find_route` — 대중교통 경로 탐색 (플라스크 `transit_route.py`가 호출하는 `searchPubTransPathT`와 대응)
- `search_station`, `get_bus_station_info`, `get_subway_station_info` — 정류장/역 정보 조회
- `search_bus_route`, `get_bus_lane_detail` — 버스 노선 조회
- `get_subway_transfer_info`, `search_subway_schedule` — 지하철 환승/시간표
- `search_city_code`, `search_express_bus_terminals`, `search_intercity_bus_terminals`, `search_inter_bus_schedule` — 시외/고속버스 관련
- `search_train_terminals`, `search_train_service_time` — 기차 관련
- `search_air_service_time` — 항공 관련

GoNow 개발과 직접 관련 있는 건 `find_route`(플라스크 로직과 동일한 대중교통 경로 탐색)와 `search_station`/`get_subway_transfer_info` 정도 — 나머지는 ODsay가 제공하는 전체 기능 중 일부라 참고용.
