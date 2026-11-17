package com.fixmap.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 신고 첨부 파일 정보. 파일 자체는 디스크(upload.dir)에 두고 DB에는 정보만 둔다. */
@Entity
@Table(name = "report_file")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    /** upload.dir 기준 상대 경로 (예: 2026/11/{UUID}.jpg) */
    @Column(name = "stored_path", nullable = false, length = 255, unique = true)
    private String storedPath;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    /** 바이트 */
    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static ReportFile create(Long reportId, String originalName, String storedPath,
                                    String contentType, long fileSize) {
        ReportFile file = new ReportFile();
        file.reportId = reportId;
        file.originalName = originalName;
        file.storedPath = storedPath;
        file.contentType = contentType;
        file.fileSize = fileSize;
        return file;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
