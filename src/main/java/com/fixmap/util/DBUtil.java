package com.fixmap.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Oracle 연결을 만든다.
 * WEB-INF/lib에 ojdbc11.jar가 있어야 한다.
 */
public final class DBUtil {

    static {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Oracle JDBC 드라이버가 없습니다. WEB-INF/lib에 ojdbc11.jar를 넣으세요.", e);
        }
    }

    private DBUtil() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                AppConfig.get("db.url"),
                AppConfig.get("db.user"),
                AppConfig.get("db.password"));
    }
}
