package com.fixmap.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.fixmap.domain.Category;
import com.fixmap.domain.Place;
import com.fixmap.domain.Report;
import com.fixmap.domain.ReportFile;
import com.fixmap.repository.BuildingFloorRepository;
import com.fixmap.repository.CategoryRepository;
import com.fixmap.repository.PlaceRepository;
import com.fixmap.repository.ReportFileRepository;
import com.fixmap.repository.ReportRepository;

import lombok.RequiredArgsConstructor;

/** 신고 작성(REPAIR-1), 첨부 파일(REPAIR-2), "이 층의 신고" 조회 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReportService {

    /** 신고 1건에 붙일 수 있는 첨부 파일 수 */
    public static final int MAX_FILES_PER_REPORT = 3;

    /** 받을 수 있는 파일 형식과 저장할 확장자. 형식 제한은 팀 미정이라 사진만 받는다. */
    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/gif", "gif",
            "image/webp", "webp");

    private final ReportRepository reportRepository;
    private final ReportFileRepository fileRepository;
    private final CategoryRepository categoryRepository;
    private final BuildingFloorRepository floorRepository;
    private final PlaceRepository placeRepository;
    private final ReportFileStorage storage;

    /** 고장 종류 목록(표시 순서) */
    public List<Category> findCategories() {
        return categoryRepository.findAllByOrderBySortOrderAsc();
    }

    /** 신고 등록. 처리 상태는 '접수'로 시작한다. 층·영역·종류가 없으면 400 */
    @Transactional
    public ReportView create(NewReport input) {
        if (!floorRepository.existsById(input.floorId())) {
            throw badRequest("층을 찾을 수 없습니다");
        }
        if (input.placeId() != null) {
            Place place = placeRepository.findById(input.placeId())
                    .orElseThrow(() -> badRequest("영역을 찾을 수 없습니다"));
            if (!place.getFloorId().equals(input.floorId())) {
                throw badRequest("영역이 선택한 층에 없습니다");
            }
        }
        if (!categoryRepository.existsById(input.categoryId())) {
            throw badRequest("고장 종류를 찾을 수 없습니다");
        }

        Report report = reportRepository.save(Report.create(
                input.floorId(), input.placeId(), input.categoryId(),
                input.title().strip(), input.content().strip(),
                ratio(input.posX()), ratio(input.posY())));
        return toViews(List.of(report)).get(0);
    }

    /** 층의 신고 목록(최신순). 첨부 파일 정보까지 담는다. 없는 층이면 404 */
    public List<ReportView> findByFloor(Long floorId) {
        if (!floorRepository.existsById(floorId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "층을 찾을 수 없습니다");
        }
        return toViews(reportRepository.findByFloorIdOrderByCreatedAtDescIdDesc(floorId));
    }

    /**
     * 신고에 첨부 파일을 붙인다. 이미 붙은 파일과 합쳐 3개까지, 사진 형식만 받는다.
     * DB 저장에 실패하면 이미 쓴 파일을 지운다.
     */
    @Transactional
    public List<ReportFile> addFiles(Long reportId, List<MultipartFile> files) {
        if (!reportRepository.existsById(reportId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "신고를 찾을 수 없습니다");
        }
        List<MultipartFile> uploads = files == null ? List.of()
                : files.stream().filter(f -> f != null && !f.isEmpty()).toList();
        if (uploads.isEmpty()) {
            throw badRequest("첨부할 파일을 선택해 주세요");
        }
        if (fileRepository.countByReportId(reportId) + uploads.size() > MAX_FILES_PER_REPORT) {
            throw badRequest("첨부 파일은 신고 1건에 " + MAX_FILES_PER_REPORT + "개까지 올릴 수 있습니다");
        }
        for (MultipartFile upload : uploads) {
            if (!ALLOWED_TYPES.containsKey(normalizeType(upload.getContentType()))) {
                throw badRequest("사진 파일(jpg, png, gif, webp)만 올릴 수 있습니다");
            }
        }

        List<String> written = new ArrayList<>();
        try {
            List<ReportFile> saved = new ArrayList<>();
            for (MultipartFile upload : uploads) {
                String type = normalizeType(upload.getContentType());
                String storedPath = storage.store(upload, ALLOWED_TYPES.get(type));
                written.add(storedPath);
                saved.add(fileRepository.save(ReportFile.create(
                        reportId, originalName(upload), storedPath, type, upload.getSize())));
            }
            fileRepository.flush();
            return saved;
        } catch (IOException | RuntimeException e) {
            written.forEach(storage::deleteQuietly);
            if (e instanceof ResponseStatusException rse) {
                throw rse;
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일을 저장하지 못했습니다", e);
        }
    }

    /** 내려받을 첨부 파일. 정보나 실제 파일이 없으면 404 */
    public StoredFile loadFile(Long fileId) {
        ReportFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다"));
        Path path = storage.resolve(file.getStoredPath());
        if (path == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다");
        }
        return new StoredFile(file, path);
    }

    /** 신고 목록에 종류 이름, 영역 이름, 첨부 파일을 붙인다(신고마다 따로 조회하지 않고 한 번에). */
    private List<ReportView> toViews(List<Report> reports) {
        if (reports.isEmpty()) {
            return List.of();
        }
        Map<Long, String> categoryNames = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
        List<Long> placeIds = reports.stream().map(Report::getPlaceId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> placeNames = placeRepository.findAllById(placeIds).stream()
                .collect(Collectors.toMap(Place::getId, Place::getName));
        Map<Long, List<ReportFile>> filesByReport = fileRepository
                .findByReportIdInOrderByIdAsc(reports.stream().map(Report::getId).toList()).stream()
                .collect(Collectors.groupingBy(ReportFile::getReportId));

        return reports.stream()
                .map(r -> new ReportView(r,
                        categoryNames.get(r.getCategoryId()),
                        r.getPlaceId() == null ? null : placeNames.get(r.getPlaceId()),
                        filesByReport.getOrDefault(r.getId(), List.of())))
                .toList();
    }

    /** 좌표를 DB 자리수(소수 4자리)에 맞춘다. */
    private static BigDecimal ratio(BigDecimal value) {
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    /** "image/jpeg; charset=..." → "image/jpeg" */
    private static String normalizeType(String contentType) {
        if (contentType == null) {
            return "";
        }
        int semicolon = contentType.indexOf(';');
        return (semicolon < 0 ? contentType : contentType.substring(0, semicolon)).strip().toLowerCase();
    }

    /** 브라우저가 경로까지 보내는 경우가 있어 파일 이름만 남기고 255자로 자른다. */
    private static String originalName(MultipartFile file) {
        String name = StringUtils.getFilename(StringUtils.cleanPath(
                Objects.requireNonNullElse(file.getOriginalFilename(), "")).replace('\\', '/'));
        if (!StringUtils.hasText(name)) {
            name = "file";
        }
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    /** 신고 등록에 필요한 값. 필수 여부와 길이 검사는 컨트롤러의 요청 객체가 먼저 한다. */
    public record NewReport(Long floorId, Long placeId, Long categoryId, String title, String content,
                            BigDecimal posX, BigDecimal posY) {
    }

    public record ReportView(Report report, String categoryName, String placeName, List<ReportFile> files) {
    }

    public record StoredFile(ReportFile file, Path path) {
    }
}
