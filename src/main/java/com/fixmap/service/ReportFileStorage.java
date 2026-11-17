package com.fixmap.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 첨부 파일을 디스크에 저장하고 읽는다. 저장 위치는 팀 미정이라 db.properties의 upload.dir로 바꿀 수 있게 했다.
 * 파일 이름은 UUID로 바꿔 저장하고(원래 이름은 DB에만 둔다), 경로는 upload.dir 기준 상대 경로로 돌려준다.
 */
@Component
public class ReportFileStorage {

    private final Path root;

    public ReportFileStorage(@Value("${upload.dir:${user.home}/fixmap-uploads}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /** 파일을 {연}/{월}/{UUID}.{확장자}로 저장하고 상대 경로를 돌려준다. */
    public String store(MultipartFile file, String extension) throws IOException {
        LocalDate today = LocalDate.now();
        String relative = String.format("%d/%02d/%s.%s",
                today.getYear(), today.getMonthValue(), UUID.randomUUID(), extension);
        Path target = root.resolve(relative);
        Files.createDirectories(target.getParent());
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return relative;
    }

    /** 저장된 파일의 실제 경로. upload.dir 밖을 가리키거나 파일이 없으면 null */
    public Path resolve(String storedPath) {
        String relative = storedPath.startsWith("/") ? storedPath.substring(1) : storedPath;
        Path path = root.resolve(relative).normalize();
        if (!path.startsWith(root) || !Files.isRegularFile(path)) {
            return null;
        }
        return path;
    }

    /** 저장 도중 실패했을 때 이미 쓴 파일을 지운다. 지우지 못해도 예외를 던지지 않는다. */
    public void deleteQuietly(String storedPath) {
        try {
            Files.deleteIfExists(root.resolve(storedPath).normalize());
        } catch (IOException ignored) {
            // 남은 파일은 DB에 정보가 없어 화면에 나오지 않는다.
        }
    }
}
