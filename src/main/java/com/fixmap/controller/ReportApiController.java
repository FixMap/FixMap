package com.fixmap.controller;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fixmap.domain.Report;
import com.fixmap.domain.ReportFile;
import com.fixmap.service.ReportService;
import com.fixmap.service.ReportService.NewReport;
import com.fixmap.service.ReportService.ReportView;
import com.fixmap.service.ReportService.StoredFile;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

/**
 * 신고 작성(REPAIR-1), 첨부 파일(REPAIR-2) JSON API. "이 층의 신고" 목록은 MapApiController의 /api/reports/map이 맡는다.
 * 주소는 docs/URL_PLAN.md를 따른다. 요청·응답 형식은 이 PR의 제안이며 Notion API 명세서에 옮겨 확정한다.
 */
@RestController
@RequiredArgsConstructor
public class ReportApiController {

    private final ReportService reportService;

    /** GET /api/categories : 고장 종류 목록 */
    @GetMapping("/api/categories")
    public List<CategoryResponse> categories() {
        return reportService.findCategories().stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName()))
                .toList();
    }

    /** POST /api/reports : 신고 등록(JSON). 필수 항목이 비면 400, 성공하면 201과 등록된 신고 */
    @PostMapping(path = "/api/reports", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse create(@Validated @RequestBody CreateReportRequest request) {
        ReportView created = reportService.create(new NewReport(
                request.floorId(), request.placeId(), request.categoryId(),
                request.title(), request.content(), request.posX(), request.posY()));
        return ReportResponse.of(created);
    }

    /** POST /api/reports/{id}/files : 첨부 파일 업로드(multipart, 필드 이름 files). 신고 1건에 3개까지 */
    @PostMapping(path = "/api/reports/{id}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public List<FileResponse> upload(@PathVariable("id") Long reportId,
                                     @RequestPart("files") List<MultipartFile> files) {
        return reportService.addFiles(reportId, files).stream().map(FileResponse::of).toList();
    }

    /** GET /api/files/{id} : 첨부 파일 보기(사진은 브라우저에서 바로 열린다). 없으면 404 */
    @GetMapping("/api/files/{id}")
    public ResponseEntity<Resource> download(@PathVariable("id") Long fileId) {
        StoredFile stored = reportService.loadFile(fileId);
        ReportFile file = stored.file();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .contentLength(file.getFileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(file.getOriginalName(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(stored.path()));
    }

    /** 신고 등록 요청. 신고 위치는 층(floorId)과 핀 좌표(posX, posY, 0~1)이고 영역(placeId)은 복도 등이면 생략한다. */
    public record CreateReportRequest(
            @NotNull(message = "층을 선택해 주세요") Long floorId,
            Long placeId,
            @NotNull(message = "고장 종류를 선택해 주세요") Long categoryId,
            @NotBlank(message = "제목을 입력해 주세요")
            @Size(max = 100, message = "제목은 100자까지 입력할 수 있습니다") String title,
            @NotBlank(message = "내용을 입력해 주세요")
            @Size(max = 4000, message = "내용은 4000자까지 입력할 수 있습니다") String content,
            @NotNull(message = "지도에서 위치를 선택해 주세요")
            @DecimalMin(value = "0", message = "위치 좌표가 올바르지 않습니다")
            @DecimalMax(value = "1", message = "위치 좌표가 올바르지 않습니다") BigDecimal posX,
            @NotNull(message = "지도에서 위치를 선택해 주세요")
            @DecimalMin(value = "0", message = "위치 좌표가 올바르지 않습니다")
            @DecimalMax(value = "1", message = "위치 좌표가 올바르지 않습니다") BigDecimal posY) {
    }

    public record CategoryResponse(Long id, String name) {
    }

    /** 신고 1건: 작성한 항목 전부 + 처리 상태 + 첨부 파일 */
    public record ReportResponse(Long id, Long floorId, Long placeId, String placeName,
                                 Long categoryId, String categoryName, String title, String content,
                                 BigDecimal posX, BigDecimal posY, String status,
                                 LocalDateTime createdAt, List<FileResponse> files) {

        static ReportResponse of(ReportView view) {
            Report r = view.report();
            return new ReportResponse(r.getId(), r.getFloorId(), r.getPlaceId(), view.placeName(),
                    r.getCategoryId(), view.categoryName(), r.getTitle(), r.getContent(),
                    r.getPosX(), r.getPosY(), r.getStatus(), r.getCreatedAt(),
                    view.files().stream().map(FileResponse::of).toList());
        }
    }

    /** 첨부 파일 정보. url로 파일을 연다(컨텍스트 경로 /FixMap은 화면에서 붙인다). */
    public record FileResponse(Long id, String name, String contentType, long size, String url) {

        static FileResponse of(ReportFile f) {
            return new FileResponse(f.getId(), f.getOriginalName(), f.getContentType(), f.getFileSize(),
                    "/api/files/" + f.getId());
        }
    }
}
