# FixMap

학교 배치도에 핀을 찍어 시설 고장·불편을 신고하고, 관리자가 처리 상황을 관리하는 학교 전용 웹 서비스입니다.

- 일반 사용자: `/FixMap/` (학교 지도, 신고, 신고 조회)
- 관리자: `/FixMap/admin` (신고 관리, 답글, 완료/보류 처리)

## 개발 환경

| 항목 | 버전 |
| --- | --- |
| Java | 21 |
| 서버 | Apache Tomcat **10.1** (Jakarta EE, `jakarta.servlet`). Tomcat 9 이하는 동작하지 않음 |
| 프로젝트 | Eclipse Dynamic Web Project (Dynamic Web Module 6.0) + Maven |
| 프레임워크 | Spring MVC 6 (Spring Boot 없음), Spring Data JPA, Hibernate |
| DB | Oracle (12c 이상) |

## 처음 세팅하기

1. 저장소를 클론하고 Eclipse에서 `File > Import > Existing Maven Projects`(또는 Existing Projects into Workspace)로 연다. 라이브러리는 Maven이 받는다.
2. `Window > Preferences > Server > Runtime Environments`에 Tomcat 10.1을 추가하고, 프로젝트의 Targeted Runtime으로 지정한다.
3. `src/main/resources/db.properties.example`을 같은 폴더에 `db.properties`로 복사하고 DB 접속 정보를 채운다. (커밋되지 않음)
4. 테이블은 JPA(`jpa.ddl-auto`)가 만든다. 현재는 엔티티가 없어서 테이블이 만들어지지 않는다. 초기 데이터는 `db/data.sql`을 Oracle에서 실행한다.
5. 프로젝트를 Tomcat에 올리고 `http://localhost:8080/FixMap/`에 접속한다.

## 폴더 구조

```
db/                         Oracle 초기 데이터와 예시 쿼리 (schema.sql은 JPA 전환 전 참고용)
pom.xml                     Maven 설정 (Spring, JPA, Oracle 드라이버 등)
src/main/java/com/fixmap/
  config/                   Spring 설정 (WebAppInitializer, RootConfig, WebConfig)
  controller/               화면(JSP)과 JSON API 컨트롤러
  service/                  업무 로직, 트랜잭션
  repository/               DB 접근 (Spring Data JPA)
  domain/                   JPA 엔티티
src/main/resources/         db.properties.example (DB·JPA 설정 예시)
src/main/webapp/
  WEB-INF/views/            JSP 화면 (추가 예정)
  static/                   css, js 등 정적 파일 (추가 예정)
```

패키지 이름은 설정에서 읽는 이름에 맞춘 임시 구성입니다. 팀에서 바꾸기로 하면 폴더와 `RootConfig`, `WebConfig`의 패키지 문자열을 함께 바꿉니다.

## 브랜치 규칙 (제안)

- `main`: 항상 실행되는 상태
- 기능 작업은 `feature/기능명` 브랜치에서 하고 PR로 합친다.
