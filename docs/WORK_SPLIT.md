# 작업 분담과 충돌 방지 (제안, 팀 확정 전)

사람과 AI 에이전트(Codex, Claude 등)가 한 repo에서 동시에 작업해도 **같은 파일을 동시에 고치지 않게** 하는 것이 이 문서의 기준이다.
아래는 모두 제안이다. 팀이 정하면 "제안" 표시를 지우고 `AGENTS.md`에 옮긴다. 사람 이름과 담당 배정은 팀이 정한다(이 문서에는 적지 않는다).

## 1. 원칙 (우선순위 순)

1. **한 파일은 한 번에 한 작업자(사람 또는 에이전트)만 고친다.** 충돌은 합칠 때 푸는 것보다 처음부터 안 나게 하는 것이 싸다.
2. **영역으로 나눈다.** FE와 BE의 폴더를 나누고, 상대 영역은 고치지 않는다(2절).
3. **기능 단위로 쪼갠다.** 기능마다 파일이 겹치지 않게 이름을 나눈다(3절).
4. **계약을 먼저 적는다.** FE와 BE가 서로 기다리지 않도록 API 명세를 먼저 합의하고 병행한다(4절).
5. **여러 기능이 같이 쓰는 파일은 따로 관리한다.** 한 사람이 작은 PR로 먼저 합친다(5절).
6. **PR은 작게, 빨리 합친다.** 브랜치가 오래 살수록 충돌이 커진다(6절).

## 2. 영역 소유

| 영역 | 경로 | 소유 | 비고 |
| --- | --- | --- | --- |
| 화면 | `src/main/webapp/WEB-INF/views/` | FE | JSP |
| 정적 파일 | `src/main/webapp/static/` | FE | JS, CSS, 이미지 |
| API·로직 | `src/main/java/com/fixmap/controller`, `service`, `repository`, `domain` | BE | |
| DB | `db/` | BE | `schema.sql`, `data.sql`, `queries.sql` |
| 스프링 설정 | `src/main/java/com/fixmap/config/`, `src/main/resources/` | BE | 공유 파일(5절) |
| 프로젝트 설정 | `pom.xml`, `web.xml`, `.classpath`, `.settings/` | 공유(5절) | |
| 문서(명세) | Notion 기능 명세서, API 명세서, ERD, 디자인 | 바꾸는 쪽이 자기 영역 문서를 고친다 | 4절 |
| 문서(repo) | `docs/`, `README.md` | 바꾸는 쪽 | 같은 문서를 둘이 동시에 고치지 않는다 |
| 작업 기록 | `docs/worklog/` | 작업한 사람 또는 에이전트 | PR마다 새 파일 하나. 남의 기록은 고치지 않는다([규칙](worklog/README.md)) |
| 작업 규칙 | `AGENTS.md`, `CLAUDE.md`, `.github/CODEOWNERS` | 팀장 | 바꾸려면 팀에 먼저 말한다 |

- 상대 영역에 필요한 변경이 보이면 직접 고치지 말고, 무엇이 필요한지 PR 코멘트나 Notion에 남긴다.
- AI 에이전트는 **요청한 사람의 영역만** 고친다. 영역을 넘는 요청을 받으면 먼저 확인한다.

## 3. 기능 단위로 파일 나누기

기능(slice)은 화면과 기능 ID(REPAIR-n) 기준으로 셋이다. 파일 이름에 기능 이름을 넣어 서로 다른 파일을 쓰게 한다. 아래 이름은 제안이다.

| 기능 | 기능 ID | FE 파일 | BE 파일 |
| --- | --- | --- | --- |
| 메인 지도 | REPAIR-7 | `views/main.jsp`, `static/js/map.js`, `static/css/main.css` | `MapApiController`, `MapService`, `Building*`, `Place*`, `*Repository` (PR #11) |
| 신고 작성 | REPAIR-1, 2, 8 | `static/js/report-form.js`, `static/css/report-form.css` (메인 화면 안의 영역이라 `main.jsp`에서 불러온다) | `ReportApiController`, `ReportService`, 파일 업로드 |
| 관리자 | REPAIR-3, 4, 5, 10 | `views/admin.jsp`, `static/js/admin.js`, `static/css/admin.css` | `AdminReportApiController`, `AdminReportService` |

- JS·CSS는 기능마다 파일을 나눈다. 한 파일(`app.js`, `style.css`)에 모두 넣으면 두 기능이 같은 파일을 고치게 된다.
- `main.jsp`는 메인 지도와 신고 작성이 함께 쓰는 화면이라 한 사람이 맡는다. 신고 작성 영역은 `main.jsp` 안에 자리만 잡고, 내용은 `report-form.js`가 그린다(또는 JSP 조각으로 나눈다. 방식은 FE가 정한다).
- BE도 기능마다 Controller와 Service를 나눈다. 한 Controller에 모든 API를 넣지 않는다.
- URL은 `docs/URL_PLAN.md`에 있는 것만 쓴다. 새 URL은 그 문서와 API 명세서를 먼저 고친다.

## 4. 계약 먼저 (FE와 BE가 서로 기다리지 않게)

- 계약은 **API 명세서**(Notion)의 행이다. URL, 요청, 응답, 오류가 적혀 있어야 구현을 시작한다. "미정(제안)" 행은 요청·응답을 정한 뒤에 구현한다.
- 순서: ① 계약(명세) → ② BE API와 FE 화면을 **병행**(FE는 명세의 예시 JSON으로 화면을 먼저 만든다) → ③ 둘을 연결해 확인.
- 계약을 바꾸려면 코드보다 **명세를 먼저** 고치고, 바뀐 것을 상대에게 알린다. 응답 필드를 몰래 바꾸지 않는다.
- DB 변경(컬럼, 상태 값)은 `ERD 설계` → `db/schema.sql` → 엔티티 순으로 한 사람이 한 PR에서 처리한다.

## 5. 충돌이 잘 나는 공유 파일

여러 기능이 건드리는 파일이다. **한 번에 한 PR만** 고치고, 고치기 전에 팀에 알린다. 이 파일을 바꾸는 PR은 다른 변경과 섞지 않는다.

| 파일 | 왜 충돌하나 | 규칙 |
| --- | --- | --- |
| `pom.xml`, `web.xml`, `.classpath`, `.settings/` | 라이브러리·설정을 여러 사람이 추가 | 이유를 적은 작은 PR로 먼저 합친다 |
| `config/RootConfig`, `WebConfig` | 설정 한 곳에 몰림 | BE 한 사람이 관리 |
| `domain/Report.java`, `repository/ReportRepository` | 지도, 신고 작성, 관리자가 모두 쓴다. 지금은 지도용 컬럼만 매핑되어 있다(PR #11) | 컬럼 추가는 따로 한 PR로 먼저 합치고, 기능 PR은 그 뒤에 시작한다 |
| `db/schema.sql`, `db/data.sql` | 한 파일에 모든 테이블·시드 | DB 변경은 한 사람이 순서대로 |
| `static/css/common.css` | 모든 화면이 공통으로 사용 | FE 한 사람이 관리. 화면별 스타일은 기능별 CSS에 둔다 |
| `AGENTS.md`, `docs/URL_PLAN.md`, `README.md` | 모두가 참고하고 고침 | 한 번에 한 PR. 같은 줄을 동시에 고치지 않는다 |
| Notion API 명세서, 기능 명세서 | 같은 행을 둘이 고치면 덮어쓴다 | 고치기 전에 최신으로 열고, 한 행은 한 사람이 고친다 |

## 6. 브랜치와 PR

- 브랜치는 `develop`에서 만든다. 기능 하나를 FE 브랜치와 BE 브랜치로 **나눠** 병행한다. 이름에 영역을 넣는다: `feature/fe-main-map`, `feature/be-map-api`.
- **한 브랜치는 한 영역만 고친다.** FE 파일과 BE 파일을 한 PR에 섞지 않는다. (섞으면 리뷰와 충돌 해결이 어려워진다.)
- **한 에이전트(또는 사람)는 한 브랜치에서 한 기능만** 한다. 하나를 끝내 PR을 올리고 나서 다음으로 간다.
- 작업을 시작하기 전에 `git fetch`로 최신 `develop`을 확인하고, 같은 기능을 누가 하고 있는지 본다(아래).
- 작게 나눠 하루 안에 PR을 올린다. 오래 걸리면 중간 PR로 쪼갠다.
- push 전에 최신 `develop`을 합친다. 충돌이 **자기 영역 파일**이면 직접 풀고, **상대 영역 파일**이면 풀지 말고 상대에게 알린다.
- 합치는 순서: 계약·DB·공유 파일 PR → BE API → FE 화면.
- 에이전트는 요청받지 않은 PR 생성, push, 브랜치 삭제를 하지 않는다(`AGENTS.md`).

### 누가 무엇을 하는지 보이게 하기

- 작업을 시작할 때 Notion 기능 목록에서 해당 기능의 **담당자**를 적고 **상태**를 "진행 중"으로 바꾼다. 끝나면 "완료".
- 에이전트가 같은 기능을 이미 누가 "진행 중"으로 잡았으면 시작하지 말고 사람에게 확인한다.

## 7. 충돌을 줄이는 작은 습관

- 포맷만 바꾸는 수정(들여쓰기, import 정렬)을 기능 PR에 섞지 않는다. 다른 사람의 줄까지 바꿔 충돌을 만든다.
- 자기 기능과 상관없는 파일의 이름 변경, 이동, 삭제를 하지 않는다.
- 같은 파일의 같은 줄을 고치는 일이 생기면 시작 전에 알린다.
- 파일을 새로 만들 때는 기능 이름이 들어간 이름을 쓴다(3절).

## 아직 정하지 않은 것

1. 위 파일 이름(`main.css`, `report-form.js` 등)과 신고 작성 영역을 JSP 조각으로 나눌지.
2. `.github/CODEOWNERS`를 영역별로 나눠 상대 영역 PR에 자동으로 리뷰를 요청할지(지금은 모두 팀장).
3. FE가 쓸 예시 JSON(mock)을 repo에 둘지, 어디에 둘지.
4. 기능 단위 브랜치 이름 규칙(`feature/fe-*`, `feature/be-*`)을 `docs/GIT_CONVENTION.md`의 규칙으로 확정할지. 지금 Git 규칙은 "기능 하나를 JSP부터 DB까지 한 브랜치에서 끝까지" 하는 것이 원칙이라, 영역별 브랜치와 다르다. 팀이 정한 뒤에 한쪽으로 맞춘다.
