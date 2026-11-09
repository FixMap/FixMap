# FixMap 작업 지침 (Codex, Claude 등 AI 도구 공통)

학교 배치도에 핀을 찍어 시설 고장을 신고하고 관리자가 처리하는 학교 전용 웹 서비스다.
이 파일은 팀원이 어떤 AI 도구를 써도 같은 규칙으로 작업하게 하기 위한 공통 지침이다.
Claude Code는 `CLAUDE.md`가 이 파일을 불러온다.

## 먼저 읽을 것: 정해진 것과 아직 아닌 것

AI 도구는 **정해지지 않은 것을 확정처럼 쓰거나 코드에 박지 않는다.** 모르겠으면 "미정"이라고 하고 팀원에게 묻는다.

| 구분 | 내용 |
| --- | --- |
| 수행 규칙 (바꿀 수 없음) | Eclipse Dynamic Web Project, Oracle, JSP |
| 팀이 정함 | Spring MVC 6(Boot 없이) + Maven + JPA(Hibernate, `ddl-auto`), 화면은 JSP + JSON API 혼합, Lombok 사용, MVP에 로그인 없음 |
| 팀이 정함 (Git) | `main` / `develop` / `feature/*`. `main`은 팀장 승인 필수 |
| **아직 미정** | URL 대부분(`docs/URL_PLAN.md`는 제안), 첨부 파일 저장 위치, 지도를 그리는 방식(SVG / DB 값으로 직접), Bootstrap 사용, `svg-pan-zoom`, 글꼴, PR 병합 방식(Merge / Squash), CSS 파일 구조 |

용어: Eclipse, Oracle, JSP 같은 제약은 "학교 규칙"이 아니라 **"수행 규칙"**(수행평가 규정)이다.

## 지켜야 할 제약

- **Eclipse Dynamic Web Project** 구조를 유지한다. Spring Boot는 쓰지 않는다.
- 화면은 **JSP**로 서버에서 렌더링하고, JS/CSS/HTML을 직접 작성한다. 지도, 핀, 폼 전송처럼 화면을 새로 그리지 않는 부분만 JSON API를 부른다.
- DB는 **Oracle**만 쓴다. MySQL 문법을 쓰지 않는다. 예약어를 피해 이름을 정했다: `building_floor`, `floor_level`, `place_type`, `report_comment`, `file_size`.
- Java 21, Tomcat 10.1 (`jakarta.*` 패키지. `javax.servlet`을 쓰지 않는다).
- 첨부 파일은 디스크(또는 S3)에 두고 DB(`report_file`)에는 메타데이터만 둔다. 저장 위치는 미정이다.

## 구조

- **구조는 Spring MVC 6(Boot 없이) + Maven + JPA다.** 설정은 반영되어 있다(`pom.xml`, `config/`). 컨트롤러는 Spring MVC(`@Controller`는 JSP 화면, `@RestController`는 JSON API), DB 접근은 JPA(Hibernate)로 한다.
- 패키지 `com.fixmap`: `config`, `controller`, `service`, `repository`(Spring Data JPA), `domain`(JPA 엔티티). 이 이름은 설정이 읽는 이름에 맞춘 임시 구성이고 팀이 바꿀 수 있다. 바꾸면 폴더와 `RootConfig`, `WebConfig`의 패키지 문자열을 함께 바꾼다.
- 아직 기능 코드(컨트롤러, 서비스, 엔티티)와 JSP, 정적 파일은 없다.
- JSP는 `WEB-INF/views/`, 정적 파일은 `static/`.
- 지도의 핀 좌표는 층 기준 0~1 비율로 저장한다.
- URL은 임의로 만들지 않는다. 제안은 `docs/URL_PLAN.md`에 있고, 확정되기 전까지 새 URL이 필요하면 팀에 먼저 묻는다.

## 작업 규칙

### Git

- Git 규칙은 `docs/GIT_CONVENTION.md`를 따른다. `main`, `develop`에 직접 push하지 않고, 기능마다 `feature/*` 브랜치에서 PR로 합친다.
- 커밋 메시지는 `종류: 내용` (feat, fix, style, refactor, docs, chore).
- 커밋 전에 `git status`로 올라갈 파일을 확인한다. `target/`, `build/`, `bin/`, `.class`, `.war`, `.jar`(의존성), IDE 설정, `db.properties`, 비밀번호, 개인 키는 커밋하지 않는다.
- 변경은 요청한 기능 범위로 작게 유지한다.

### Notion과 맞추기 (명세가 기준이다)

기능 명세는 Notion이 기준이다. 코드를 바꾸면 아래에 해당하는 Notion 페이지와 맞는지 확인하고, 달라지면 먼저 팀과 정한 뒤 Notion을 고친다.

| 바꾼 것 | 확인하거나 고칠 Notion |
| --- | --- |
| **API** (URL, 요청 필드, 응답 JSON) | 기능 명세서 (FE와 BE가 같은 규약을 쓰도록 구현 전에 먼저 적는다) |
| **화면** (페이지 구성, 버튼, 입력 항목, 동작 순서) | 기능 명세서와 와이어프레임, Frontend 페이지 |
| **DB** (테이블, 컬럼) | ERD 설계 (Oracle 이름 기준) |
| **라이브러리 추가·변경** (`pom.xml`, JS/CSS 라이브러리) | Backend / Frontend 페이지의 Skill 하위 페이지 (사용할 것만 적는다) |
| 팀에서 논의한 결정, 비교한 선택지 | 기술 논의 사항 페이지 |

### 라이브러리와 공유 파일

- Skill 페이지 목록에 없는 라이브러리는 팀과 정한 뒤에 추가하고, 추가한 이유와 함께 Skill 페이지에 적는다.
- 같이 쓰는 파일(`pom.xml`, `web.xml`, `.classpath` 등 프로젝트 설정)을 바꿀 때는 이유를 PR에 적고 작은 PR로 먼저 합친다. CSS 파일 구조는 아직 미정이다.

### FE / BE 작업 구분

한 repo에서 같이 작업하므로 맡은 영역을 나눈다. 상대 영역의 파일은 직접 고치지 않고 그 팀원에게 알리거나 PR에 요청을 남긴다.

| | FE (프론트) | BE (백엔드) |
| --- | --- | --- |
| 폴더 | `src/main/webapp/WEB-INF/views/`, `src/main/webapp/static/` | `src/main/java/com/fixmap/`(`config`, `controller`, `service`, `repository`, `domain`), `src/main/resources/`, `db/` |
| 하는 일 | JSP 화면(레이아웃, JSTL), JS(지도·핀 그리기, API 호출, 폼 전송), CSS, 이미지 등 정적 파일, 화면 입력 확인(사용자 편의용) | JSON API(`@RestController`) 구현, 업무 로직과 트랜잭션, 엔티티와 리포지토리, DB 스크립트, 서버 쪽 입력 검증, 파일 저장, 페이지 컨트롤러(`@Controller`: URL을 JSP 이름에 연결하고 화면에 넘길 데이터를 담는 얇은 코드) |
| 하지 않는 일 | Java, SQL, DB 구조, `pom.xml` | JSP, JS, CSS |
| Notion | 화면 변경은 기능 명세서, 와이어프레임, Frontend 페이지 | API와 DB 변경은 기능 명세서, ERD 설계, Backend 페이지 |

- **API 규약**(URL, 요청 필드, 응답 JSON)은 구현 전에 Notion 기능 명세서에 먼저 적는다. BE는 그 명세대로 구현하고, FE는 명세만 보고 호출한다. 응답 형식을 바꾸려면 두 쪽이 먼저 합의한다.
- **같이 건드리는 파일**(`pom.xml`, `web.xml`, `.classpath` 등 프로젝트 설정)은 수정 전에 팀에 알리고 이유를 적은 작은 PR로 먼저 합친다.
- AI 도구는 요청한 팀원의 영역만 고친다. 상대 영역에 필요한 변경이 보이면 고치지 말고 무엇이 필요한지 알린다.

### AI 도구 작업 방식

- 사용자(팀원)가 요청하지 않은 PR 생성, push, 브랜치 삭제는 하지 않는다. 먼저 묻는다.
- 요청받은 곳만 고친다. Notion을 고치라는 요청에 코드나 PR을 건드리지 않고, 코드를 고치라는 요청에 Notion을 임의로 고치지 않는다.
- 팀과 합의하지 않은 내용을 문서나 코드 주석에 확정처럼 쓰지 않는다.
- 실수를 발견하면 숨기지 말고 무엇이 틀렸는지 알리고 고친다.

## 빌드와 실행

- Eclipse에서 Maven 프로젝트로 가져와 Tomcat 10.1에 올려 `http://localhost:8080/FixMap/`로 확인한다. 처음 세팅은 `README.md`를 따른다.
- 테이블은 JPA(`jpa.ddl-auto`)가 엔티티를 보고 만들고, 초기 데이터는 `db/data.sql`을 Oracle에서 실행한다. `db/schema.sql`은 엔티티의 이름과 컬럼을 맞추는 설계 기준이다. 아직 엔티티가 없어서 지금은 `schema.sql`로 테이블을 만들어야 `data.sql`을 실행할 수 있다.
