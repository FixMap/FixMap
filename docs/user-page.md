# 유저 페이지

`main`의 Java 21, Tomcat 10.1, Oracle, `com.fixmap` 패키지와 DB 접속 방식을 따릅니다. 유저 화면은 기존 구현을 유지하고 공통 테이블·서블릿·DAO에 연결했습니다.

## 실행

1. `src/main/java/db.properties.example`을 `db.properties`로 복사하고 Oracle 접속 정보와 `upload.dir` 절대 경로를 설정합니다. 접속 설정과 실제 업로드 파일은 커밋하지 않습니다.
2. 새 DB는 `db/schema.sql` → `db/user-page.sql` 순서로 실행합니다. 이미 초기화된 DB는 `user-page.sql`만 실행합니다. `schema.sql`은 기존 테이블을 삭제하므로 데이터가 있는 DB에 재실행하지 않습니다.
3. JDK 21에서 실행합니다. Maven 래퍼는 공식 배포 파일을 내려받고 SHA-512를 검증합니다.

```powershell
.\mvnw.cmd compile exec:java
```

로컬 주소는 <http://localhost:8090/>입니다. 포트 변경은 `$env:PORT = '8088'`, 종료는 `Ctrl+C`입니다. 외부 Tomcat에는 `target/FixMap.war`를 배포하고 <http://localhost:8080/FixMap/>으로 접속합니다. 컨텍스트 경로를 포함해 링크·API·사진 경로를 계산합니다.

Eclipse 방식도 유지합니다. `WEB-INF/lib`에 ojdbc11과 Gson 2.13.2를 넣고 기존 Dynamic Web Project 설정으로 실행합니다. Maven WAR에는 런타임 의존성이 자동 포함됩니다.

## 기능

- 학교 지도: 건물·층 선택, 공식 도면 표시, 클릭·키보드 위치 선택, 핀 이동·취소, 확대/축소, 상태별 신고 핀과 요약.
- 신고 작성: 제목(100자), 내용(4,000자), DB에 등록된 카테고리, 위치 미리보기, 사진 드래그 앤드 드롭·미리보기·첨부 취소, 등록 중 중복 클릭 차단.
- 사진: JPG/JPEG·PNG, 최대 3장, 장당 5MB, 최대 2,500만 화소. 서버에서도 확장자·실제 형식·손상·해상도를 확인합니다.
- 신고 조회: 제목·내용·건물·층·시설명 검색, 최신순·오래된순·접수→보류→완료 정렬, 상태·건물·층·카테고리·기간 필터 조합, 12건 단위 페이지 이동, 상세와 사진 다운로드.

사용자 요청에 따라 답글·관리자 기능은 추가하지 않았습니다. `main`에 있던 관리자 컨트롤러·JSP·공통 CSS와 기존 건물/지도 API는 유지합니다. 로그인 없이 전체 신고를 조회하는 MVP 방식입니다.

## main과의 통합

- `DBUtil`, `AppConfig`, Oracle 스키마를 재사용합니다. 앱 실행 시 테이블을 만들거나 삭제하지 않습니다.
- `ReportDao.insert(Connection, Report)`를 추가해 기존 신고 등록·시설 영역 판별과 사진 메타데이터 저장이 같은 트랜잭션을 사용합니다. 기존 `insert(Report)`와 지도 조회 API의 계약은 유지합니다.
- 신고는 `report`, 사진 정보는 `report_file`에 저장합니다. 원본은 `upload.dir` 아래 UUID 파일명으로 저장합니다. 실패 시 요청의 DB 변경을 롤백하고 작성한 파일을 정리합니다.
- 상태는 공통 코드 `RECEIVED`·`DONE`·`HOLD`로 저장하고 화면에서 접수·완료·보류로 표시합니다.
- 이전 H2 파일 `data/fixmap.mv.db`는 삭제하거나 자동 변환하지 않습니다. 운영 저장소는 Oracle입니다. 이전 로컬 테스트 데이터 이관은 포함하지 않았습니다.
- 유저 CSS·JavaScript는 `static/user`, 배치도는 `static/maps`에 둡니다. 유저 보안 필터는 해당 화면/API에만 적용합니다.

## 배치도

[학교 공식 교내 배치도](https://gsm.jge.hs.kr/sub/page.php?page_code=info_14)의 [원본 PDF](https://gsm.jge.hs.kr/data_files/doc/pdf_view/web/map.pdf)를 렌더링해 사용했습니다. 원본은 `docs/gsm-campus-original.pdf`입니다.

| 코드 | 위치 | 도면 |
| --- | --- | --- |
| CAMPUS | 교내 전체 | 1쪽 |
| MAIN | 본관·실습동 1~4층 | 2쪽 |
| GEUMBONG | 금봉관 1~4층 | 3쪽 |
| DONGHAENG | 동행관 1~5층 | 4쪽 |

`db/user-page.sql`은 건물·층과 카테고리를 중복 없이 추가합니다. `main` 임시 도면에 연결된 기존 신고·교실 좌표를 바꾸지 않으며, 해당 데이터를 넣어 둔 DB에는 기존 건물 탭도 표시됩니다. 공식 도면과 임시 도면은 서로 다른 층 ID를 사용하므로 좌표가 섞이지 않습니다. 도면 교체 시 기존 핀·시설 영역 좌표와의 대응을 확인해야 합니다.

좌표는 도면의 0~1 비율입니다. 건물·층과 지도 경로는 DB에서 읽습니다.

## API

| 메서드 | 경로 | 용도 |
| --- | --- | --- |
| GET | `/api/meta` | DB 건물·층·카테고리, 표시 상태, 세션 CSRF 토큰 |
| GET | `/api/pins?building=MAIN&floor=2` | 건물 코드·층 번호 기준 상세 핀 |
| GET | `/api/reports` | 신고 목록 |
| GET | `/api/reports/{id}` | 신고 상세·첨부 목록 |
| POST | `/api/reports` | 신고·사진 등록 |
| GET | `/attachments/{id}` | 사진 표시 |
| GET | `/attachments/{id}?download=1` | 사진 다운로드 |

기존 `/api/buildings`, `/api/reports/map?floor={층ID}`는 `main`의 응답 형식을 유지합니다. 유저 등록도 같은 `report.floor_id`를 사용하므로 기존 지도 API에서 조회됩니다.

목록 파라미터는 `q`, `status`, `building`, `floor`, `category`, `from`, `to`, `sort`, `page`입니다. `status`는 한글 표시 값, `building`은 DB 코드, 날짜는 `YYYY-MM-DD`, `sort`는 `newest`·`oldest`·`status`입니다.

등록은 `multipart/form-data`로 `title`, `content`, `category`, `building`, `floor`, `x`, `y`, 선택 항목인 여러 `files`를 보냅니다. 같은 세션의 `/api/meta`에서 받은 `csrfToken`을 `X-CSRF-Token` 헤더에 넣습니다. 성공은 `201 {"id":...}`, 실패는 `4xx/5xx {"error":"..."}`입니다.

## 검증

```powershell
.\mvnw.cmd package
```

통합 테스트는 `main`의 테이블·제약 DDL과 `db/user-page.sql`을 H2 Oracle 호환 모드에서 실행합니다. 실제 Tomcat HTTP 요청으로 JSP·신고 등록·공통 지도 API·검색·첨부 다운로드·잘못된 입력·롤백을 확인합니다. 테스트 설정과 파일은 `target` 아래에만 만듭니다. H2는 테스트 전용이며 운영 WAR에 포함하지 않습니다.

실제 Oracle 인스턴스의 실행과 Eclipse 배포, 이번 통합 후 브라우저 업로드 UI 재검증은 별도로 필요합니다.
