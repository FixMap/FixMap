# WEB-INF/lib

Maven을 쓰지 않으므로 필요한 jar를 이 폴더에 직접 넣습니다. Eclipse는 이 폴더의 jar를 자동으로 빌드 경로에 추가합니다.

| jar | 용도 | 받는 곳 |
| --- | --- | --- |
| `ojdbc11.jar` | Oracle JDBC 드라이버 (Java 11 이상용) | https://www.oracle.com/database/technologies/appdev/jdbc-downloads.html 또는 Maven Central `com.oracle.database.jdbc:ojdbc11` |
| (선택) `jakarta.servlet.jsp.jstl-api-3.0.x.jar`, `jakarta.servlet.jsp.jstl-3.0.x.jar` | JSP에서 `<c:forEach>` 등 JSTL 사용 시 | Maven Central `jakarta.servlet.jsp.jstl` |

서블릿 API(`jakarta.servlet`)는 Tomcat 10.x가 제공하므로 넣지 않습니다.
