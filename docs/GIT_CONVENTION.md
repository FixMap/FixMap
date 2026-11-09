# Git Convention

한 repo에서 FE(JSP, JS, CSS)와 BE(Controller, Service, Repository)를 함께 작업한다.
기능 하나를 JSP부터 DB까지 한 브랜치에서 끝까지 동작시키는 것이 원칙이다.

## 브랜치

| 브랜치 | 역할 | 규칙 |
| --- | --- | --- |
| `main` | 시연, 제출용 | 직접 push 금지. `develop`에서 PR로만 합친다 (팀장 담당) |
| `develop` | 개발 통합 | 직접 push 금지. `feature/*`, `fix/*`에서 PR로만 합친다 |
| `feature/기능명` | 기능 개발 | `develop`에서 만들고 `develop`으로 PR |
| `fix/내용` | 버그 수정 | `feature`와 동일 |

브랜치 이름은 영문 소문자와 하이픈: `feature/map-pin`, `feature/report-create`, `fix/floor-tab-pin`

## 작업 흐름

1. `git switch develop && git pull`
2. `git switch -c feature/map-pin`
3. 작게 자주 커밋한다.
4. push 전에 최신 develop을 합쳐 충돌을 먼저 해결한다: `git merge develop`
5. `develop`으로 PR을 올린다. 승인은 필수가 아니지만, 팀원이 한 번씩 훑어보고 병합하는 것을 권장한다.
6. 병합한 브랜치는 삭제한다.

## 커밋 메시지

`종류: 내용` (한국어 가능, 한 줄 50자 안팎)

| 종류 | 의미 |
| --- | --- |
| `feat` | 기능 추가 |
| `fix` | 버그 수정 |
| `style` | CSS, 화면 모양 |
| `refactor` | 동작 변화 없는 정리 |
| `docs` | 문서 |
| `chore` | 설정, 빌드 |

예: `feat: 지도 핀 등록 API`

## PR

- 제목은 커밋 메시지 형식으로 쓴다.
- 본문에 한 일, 확인한 방법(화면 캡처 등), 관련 기능 ID(`REPAIR-n`)를 적는다.
- 변경 요청은 같은 브랜치에 추가 커밋으로 고친다.

## FE/BE 충돌 줄이기

- API 규약(URL, 요청 필드, 응답 JSON)을 먼저 Notion 기능 명세서에 정한다.
- 주 영역과 하는 일, 하지 않는 일은 [AGENTS.md의 "FE / BE 작업 구분"](../AGENTS.md)을 따른다. 상대 영역의 파일은 직접 고치지 않고 알린다.
- 같이 건드리는 파일(`pom.xml`, `web.xml`, `.classpath` 등 프로젝트 설정)은 수정 전에 팀에 알리고 작은 PR로 바로 합친다.

## 하지 않는 것

- `main`, `develop`에 직접 push, `push --force`
- `db.properties`, 비밀번호, 개인 키 커밋
- `target/`, IDE 설정 커밋
