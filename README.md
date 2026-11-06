# FixMap

학교 배치도에 핀을 찍어 시설 고장·불편을 신고하고, 관리자가 처리 상황을 관리하는 학교 전용 웹 서비스입니다.

- 일반 사용자: `/FixMap/` (학교 지도, 신고, 신고 조회)
- 관리자: `/FixMap/admin` (신고 관리, 답글, 완료/보류 처리)

## 개발 환경

| 항목 | 버전 |
| --- | --- |
| Java | 21 |
| 서버 | Apache Tomcat **10.1** (Jakarta EE, `jakarta.servlet`). Tomcat 9 이하는 동작하지 않음 |
| 프로젝트 | Eclipse Dynamic Web Project (Dynamic Web Module 6.0) + **Maven** (m2e) |
| 프레임워크 | Spring MVC 6 (Boot 없음), JSP + JSTL, Spring JDBC(JdbcTemplate), HikariCP |
| DB | Oracle (12c 이상) |

라이브러리는 `pom.xml`에서 관리하며 jar 파일을 직접 넣지 않는다. Maven이 `WEB-INF/lib`에 자동 배포한다.

## 처음 세팅하기

1. 저장소를 클론하고 Eclipse에서 `File > Import > Maven > Existing Maven Projects`로 연다. (Eclipse IDE for Enterprise Java and Web Developers 필요)
2. `Window > Preferences > Server > Runtime Environments`에 Tomcat 10.1을 추가하고, 프로젝트 우클릭 > `Properties > Targeted Runtimes`에서 지정한다.
3. `pom.xml`을 바꾼 뒤에는 프로젝트 우클릭 > `Maven > Update Project` (Alt+F5)를 실행한다.
4. `src/main/resources/db.properties.example`을 같은 폴더에 `db.properties`로 복사하고 DB 접속 정보를 채운다. (커밋되지 않음)
5. Oracle에서 `db/schema.sql` → `db/data.sql` 순서로 실행한다.
6. 프로젝트를 Tomcat에 올리고 `http://localhost:8080/FixMap/`에 접속한다.

명령줄 빌드: `mvn package` → `target/FixMap.war`

## 폴더 구조

```
db/                         Oracle DDL, 테스트 데이터, 예시 쿼리
pom.xml                     Maven 의존성 (Spring, Jackson, ojdbc, JSTL 등)
src/main/java/com/fixmap/
  config/                   Spring 설정 (WebAppInitializer, RootConfig, WebConfig)
  controller/               화면 컨트롤러 (/, /admin) -> JSP 반환
  controller/api/           JSON API (@RestController) + 예외 처리
  service/                  업무 로직, 트랜잭션
  dao/                      DB 접근 (JdbcTemplate + SQL)
  dto/                      테이블과 대응하는 데이터 클래스
src/main/resources/         db.properties(.example)
src/main/webapp/
  WEB-INF/views/            JSP 화면 (직접 접근 불가, 컨트롤러가 반환)
  static/css, static/js     직접 작성하는 정적 파일
  static/lib/               내려받아 쓰는 프론트 라이브러리 (선택)
  static/maps/              층별 배치도 SVG (현재는 임시 도면)
```

## 프론트 라이브러리

React/Vue 같은 빌드 도구는 쓰지 않는다. JSP가 HTML을 만들고, 브라우저 JS는 `fetch`로 `/api/...`를 호출한다.

- **JSTL**: JSP 상단에 `taglib prefix="c" uri="jakarta.tags.core"` 선언 후 `c:forEach`, `c:if` 사용
- **Bootstrap 5 / Bootstrap Icons, svg-pan-zoom**: JSP의 head/body 끝에서 CDN 주소로 불러오거나, `static/lib/`에 내려받아 `${pageContext.request.contextPath}/static/lib/...`로 참조
- 공통 스타일은 `static/css/common.css`, 화면별 스크립트는 `static/js/`에 둔다.

## 지금 동작하는 것

- `GET /api/buildings` : 건물·층 목록과 배치도 경로
- `GET /api/reports/map?floor={층 id}` : 층의 신고 핀
- 메인 화면: 건물·층 탭, 배치도 표시, 신고 핀 표시, 클릭 위치를 0~1 비율 좌표로 계산
- 관리자 화면: 기본 레이아웃과 '사용자 사이트로 이동' 버튼

## 브랜치 규칙 (제안)

- `main`: 항상 실행되는 상태
- 기능 작업은 `feature/기능명` 브랜치에서 하고 PR로 합친다.
