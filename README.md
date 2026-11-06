# FixMap

학교 배치도에 핀을 찍어 시설 고장·불편을 신고하고, 관리자가 처리 상황을 관리하는 학교 전용 웹 서비스입니다.

- 일반 사용자: `/FixMap/` (학교 지도, 신고, 신고 조회)
- 관리자: `/FixMap/admin` (신고 관리, 답글, 완료/보류 처리)

## 개발 환경

| 항목 | 버전 |
| --- | --- |
| Java | 21 |
| 서버 | Apache Tomcat **10.1** (Jakarta EE, `jakarta.servlet`). Tomcat 9 이하는 동작하지 않음 |
| 프로젝트 | Eclipse Dynamic Web Project (Dynamic Web Module 5.0) |
| DB | Oracle (12c 이상) |

## 처음 세팅하기

1. 저장소를 클론하고 Eclipse에서 `File > Import > Existing Projects into Workspace`로 연다.
2. `Window > Preferences > Server > Runtime Environments`에 Tomcat 10.1을 추가하고, 프로젝트의 Targeted Runtime으로 지정한다.
3. `src/main/webapp/WEB-INF/lib`에 `ojdbc11.jar`를 넣는다. ([lib 안내](src/main/webapp/WEB-INF/lib/README.md))
4. `src/main/java/db.properties.example`을 같은 폴더에 `db.properties`로 복사하고 DB 접속 정보를 채운다. (커밋되지 않음)
5. Oracle에서 `db/schema.sql` → `db/data.sql` 순서로 실행한다.
6. 프로젝트를 Tomcat에 올리고 `http://localhost:8080/FixMap/`에 접속한다.

## 폴더 구조

```
db/                         Oracle DDL, 테스트 데이터, 예시 쿼리
src/main/java/
  com/fixmap/
    controller/             화면 서블릿 (/, /admin)
    controller/api/         JSON API 서블릿 (/api/...)
    dao/                    DB 접근 (SQL)
    dto/                    테이블과 1:1로 대응하는 데이터 클래스
    filter/                 공통 필터 (UTF-8 인코딩)
    util/                   DB 연결, 설정, JSON 도구
  db.properties.example     DB·업로드 설정 예시
src/main/webapp/
  WEB-INF/views/            JSP 화면 (직접 접근 불가, 서블릿이 forward)
  WEB-INF/lib/              jar 라이브러리
  static/css, static/js     정적 파일
  static/maps/              층별 배치도 SVG (현재는 임시 도면)
```

## 지금 동작하는 것

- `GET /api/buildings` : 건물·층 목록과 배치도 경로
- `GET /api/reports/map?floor={층 id}` : 층의 신고 핀
- 메인 화면: 건물·층 탭, 배치도 표시, 신고 핀 표시, 클릭 위치를 0~1 비율 좌표로 계산
- 관리자 화면: 기본 레이아웃과 '사용자 사이트로 이동' 버튼

## 브랜치 규칙 (제안)

- `main`: 항상 실행되는 상태
- 기능 작업은 `feature/기능명` 브랜치에서 하고 PR로 합친다.
