package com.fixmap.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 클래스패스의 db.properties를 한 번 읽어 두고 값을 꺼내 쓴다.
 * db.properties는 src/main/java/db.properties.example을 복사해서 만든다.
 */
public final class AppConfig {

    private static final Properties PROPS = load();

    private AppConfig() {
    }

    private static Properties load() {
        Properties props = new Properties();
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new IllegalStateException(
                        "db.properties가 없습니다. src/main/java/db.properties.example을 복사해서 만드세요.");
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("db.properties를 읽지 못했습니다.", e);
        }
        return props;
    }

    public static String get(String key) {
        String value = PROPS.getProperty(key);
        if (value == null) {
            throw new IllegalStateException("db.properties에 " + key + " 값이 없습니다.");
        }
        return value.trim();
    }
}
