package com.fixmap.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 관리자 접근 키 확인(MVP용 임시 방식). 로그인이 없어서 db.properties의 admin.key 하나를 관리자끼리 나눠 쓴다.
 * 키를 설정하지 않으면 관리자 화면과 API는 모두 막힌다.
 */
@Component
public class AdminAccess {

    private final byte[] key;

    public AdminAccess(@Value("${admin.key:}") String key) {
        this.key = key == null ? new byte[0] : key.trim().getBytes(StandardCharsets.UTF_8);
    }

    /** 키가 설정되어 있는지 */
    public boolean configured() {
        return key.length > 0;
    }

    /** 받은 값이 설정한 키와 같은지. 설정이 없으면 항상 false */
    public boolean matches(String candidate) {
        if (!configured() || candidate == null) {
            return false;
        }
        return MessageDigest.isEqual(key, candidate.trim().getBytes(StandardCharsets.UTF_8));
    }
}
