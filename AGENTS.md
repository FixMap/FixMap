# FixMap 작업 지침 (Codex, Claude 등 AI 도구 공통)

학교 배치도에 핀을 찍어 시설 고장을 신고하고 관리자가 처리하는 학교 전용 웹 서비스다.
이 파일은 팀원이 어떤 AI 도구를 써도 같은 규칙으로 작업하게 하기 위한 공통 지침이다.
Claude Code는 `CLAUDE.md`가 이 파일을 불러온다.

## 지켜야 할 제약

- **Eclipse Dynamic Web Project** 구조를 유지한다 (수행 규칙). Spring Boot는 쓰지 않는다. Boot 없이 Spring MVC만 쓴다.
- 화면은 **JSP**로 서버에서 렌더링하고 (수행 규칙), JS/CSS/HTML을 직접 작성한다. 화면과 API는 JSP + JSON API를 섞어 쓴다. 프론트 프레임워크나 목록에 없는 새 라이브러리는 팀과 정하기 전에 도입하지 않는다.
- DB는 **Oracle**만 쓴다 (수행 규칙). MySQL 문법을 쓰지 않는다. 예약어를 피해 이름을 정했다: `building_floor`, `floor_level`, `place_type`, `report_comment`, `file_size`.
- Java 21, Tomcat 10.1 (`jakarta.*` 패키지. `javax.servlet`을 쓰지 않는다).

## 구조

- URL 구조는 아직 정해지지 않았다. 사용자 화면과 관리자 화면을 URL로 나누는 안이 논의 중일 뿐이다. URL은 임의로 정하지 말고 팀과 먼저 정한다. MVP에는 로그인이 없다.
- 지도: 층별 SVG 배치도 위에 핀 좌표를 0~1 비율로 저장한다.
- 현재 패키지 `com.fixmap`: `controller`(화면, `api`는 JSON), `dao`, `dto`, `filter`, `util`. 서블릿 기반이다.
- **Spring MVC 6(Boot 없이) + Maven + JPA(Hibernate)를 쓰기로 정했다.** 사용할 라이브러리 목록은 Notion Backend Skill 페이지를 따른다.
- 위 내용은 정해졌지만 **코드에는 아직 반영되지 않았다**(현재 서블릿 기반). 적용은 별도 PR(`feature/spring-setup`)로 하며, 그 PR이 병합되기 전에는 구조를 바꾸지 않는다.
- JSP는 `WEB-INF/views/`, 정적 파일은 `static/`

## 작업 규칙

- Git 규칙은 `docs/GIT_CONVENTION.md`를 따른다. `main`, `develop`에 직접 push하지 않고, 기능마다 `feature/*` 브랜치에서 PR로 합친다.
- 사용자(팀원)가 요청하지 않은 PR 생성, push, 브랜치 삭제는 하지 않는다. 먼저 묻는다.
- 커밋 메시지는 `종류: 내용` (feat, fix, style, refactor, docs, chore).
- `db.properties`, 비밀번호, 개인 키를 커밋하지 않는다. `target/`도 커밋하지 않는다.
- 변경은 요청한 기능 범위로 작게 유지한다. 같이 쓰는 파일(`web.xml`, `common.css`, `.classpath` 등 프로젝트 설정)을 바꿀 때는 이유를 PR에 적는다.
- API를 바꾸면 Notion 기능 명세서와 맞는지 확인한다.

## 빌드와 실행

- Eclipse에서 Tomcat 10.1에 올려 `http://localhost:8080/FixMap/`로 확인한다.
- DB 스크립트는 `db/schema.sql` → `db/data.sql` 순서로 Oracle에서 실행한다.
