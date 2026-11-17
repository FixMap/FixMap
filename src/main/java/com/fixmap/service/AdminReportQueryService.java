package com.fixmap.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fixmap.domain.Building;
import com.fixmap.domain.BuildingFloor;
import com.fixmap.domain.Category;
import com.fixmap.domain.Place;
import com.fixmap.domain.Report;
import com.fixmap.repository.BuildingFloorRepository;
import com.fixmap.repository.BuildingRepository;
import com.fixmap.repository.CategoryRepository;
import com.fixmap.repository.PlaceRepository;
import com.fixmap.repository.ReportCommentRepository;
import com.fixmap.repository.ReportFileRepository;
import com.fixmap.repository.ReportRepository;

import lombok.RequiredArgsConstructor;

/**
 * 관리자 화면(/admin)의 목록·요약·상세 조회.
 * 신고 수가 많지 않은 학교용이라 신고를 모두 읽어 메모리에서 거르고 정렬한다.
 */
@Service
@RequiredArgsConstructor
public class AdminReportQueryService {

    public static final int PAGE_SIZE = 10;

    private static final DateTimeFormatter LIST_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter API_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    /** 상태순 정렬: 접수 → 보류 → 완료 */
    private static final List<String> STATUS_ORDER =
            List.of(Report.STATUS_RECEIVED, Report.STATUS_HOLD, Report.STATUS_DONE);

    private final ReportRepository reportRepository;
    private final CategoryRepository categoryRepository;
    private final BuildingRepository buildingRepository;
    private final BuildingFloorRepository floorRepository;
    private final PlaceRepository placeRepository;
    private final ReportFileRepository fileRepository;
    private final ReportCommentRepository commentRepository;

    /** /admin 쿼리 문자열. 값이 없거나 올바르지 않으면 걸러내지 않는다. */
    public record ListQuery(String q, String status, Long building, boolean unread, String sort, int page) {
    }

    public record Row(Long id, String title, String category, String building, String floor, String place,
                      String status, boolean read, String createdAt) {
    }

    public record BuildingCount(Long id, String name, int count) {
    }

    /** 요약 타일. 걸러내기와 상관없이 전체 신고 기준이다. */
    public record Summary(int total, int unread, Map<String, Integer> byStatus, List<BuildingCount> byBuilding) {
    }

    public record ListResult(List<Row> reports, Summary summary, int page, int totalPages, int totalCount) {
    }

    public record FileInfo(Long id, String name, String contentType, long size, String url) {
    }

    public record CommentInfo(Long id, String content, String createdAt) {
    }

    public record Detail(Long id, String title, String content, Long categoryId, String category, Long floorId,
                         String building, String floor, Long placeId, String place, BigDecimal posX,
                         BigDecimal posY, String status, boolean read, String createdAt,
                         List<FileInfo> files, List<CommentInfo> comments) {
    }

    @Transactional(readOnly = true)
    public ListResult list(ListQuery query) {
        Lookup lookup = new Lookup();
        List<Report> all = reportRepository.findAll();

        Summary summary = summarize(all, lookup);

        String keyword = query.q() == null ? "" : query.q().trim().toLowerCase(Locale.ROOT);
        List<Report> filtered = all.stream()
                .filter(r -> !isStatus(query.status()) || r.getStatus().equals(query.status()))
                .filter(r -> query.building() == null || query.building().equals(lookup.buildingId(r)))
                .filter(r -> !query.unread() || !r.isRead())
                .filter(r -> keyword.isEmpty() || lookup.searchText(r).contains(keyword))
                .sorted(comparator(query.sort()))
                .toList();

        int totalPages = Math.max(1, (filtered.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.min(Math.max(query.page(), 1), totalPages);
        List<Row> rows = filtered.stream()
                .skip((long) (page - 1) * PAGE_SIZE)
                .limit(PAGE_SIZE)
                .map(r -> new Row(r.getId(), r.getTitle(), lookup.categoryName(r), lookup.buildingName(r),
                        lookup.floorName(r), lookup.placeName(r), r.getStatus(), r.isRead(),
                        r.getCreatedAt().format(LIST_TIME)))
                .toList();
        return new ListResult(rows, summary, page, totalPages, filtered.size());
    }

    /** 관리자 상세. 열면 읽음으로 바꾼다. 신고가 없으면 404. */
    @Transactional
    public Detail detail(Long id) {
        if (!reportRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "신고를 찾을 수 없습니다");
        }
        reportRepository.markRead(id);
        return load(id);
    }

    /** 사용자 사이트의 신고 상세(메인 오른쪽 영역). 관리자 상세와 같은 내용이고 읽음 처리는 하지 않는다. */
    @Transactional(readOnly = true)
    public Detail view(Long id) {
        return load(id);
    }

    private Detail load(Long id) {
        Report r = reportRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "신고를 찾을 수 없습니다"));

        Lookup lookup = new Lookup();
        List<FileInfo> files = fileRepository.findByReportIdOrderByIdAsc(id).stream()
                .map(f -> new FileInfo(f.getId(), f.getOriginalName(), f.getContentType(), f.getFileSize(),
                        "/api/files/" + f.getId()))
                .toList();
        List<CommentInfo> comments = commentRepository.findByReportIdOrderByIdAsc(id).stream()
                .map(c -> new CommentInfo(c.getId(), c.getContent(), format(c.getCreatedAt())))
                .toList();
        return new Detail(r.getId(), r.getTitle(), r.getContent(), r.getCategoryId(), lookup.categoryName(r),
                r.getFloorId(), lookup.buildingName(r), lookup.floorName(r), r.getPlaceId(), lookup.placeName(r),
                r.getPosX(), r.getPosY(), r.getStatus(), r.isRead(), format(r.getCreatedAt()), files, comments);
    }

    private Summary summarize(List<Report> all, Lookup lookup) {
        Map<String, Integer> byStatus = new LinkedHashMap<>();
        STATUS_ORDER.forEach(s -> byStatus.put(s, 0));
        all.forEach(r -> byStatus.merge(r.getStatus(), 1, Integer::sum));

        Map<Long, Integer> counts = new LinkedHashMap<>();
        all.forEach(r -> counts.merge(lookup.buildingId(r), 1, Integer::sum));
        List<BuildingCount> byBuilding = lookup.buildings.values().stream()
                .sorted(Comparator.comparingInt(Building::getSortOrder))
                .map(b -> new BuildingCount(b.getId(), b.getName(), counts.getOrDefault(b.getId(), 0)))
                .toList();

        int unread = (int) all.stream().filter(r -> !r.isRead()).count();
        return new Summary(all.size(), unread, byStatus, byBuilding);
    }

    private static Comparator<Report> comparator(String sort) {
        Comparator<Report> latest = Comparator.comparing(Report::getCreatedAt).reversed()
                .thenComparing(Comparator.comparing(Report::getId).reversed());
        if ("status".equals(sort)) {
            return Comparator.comparingInt((Report r) -> STATUS_ORDER.indexOf(r.getStatus())).thenComparing(latest);
        }
        return latest;
    }

    private static boolean isStatus(String status) {
        return status != null && STATUS_ORDER.contains(status);
    }

    private static String format(LocalDateTime time) {
        return time.format(API_TIME);
    }

    /** 이름 조회용 표. 건물·층·영역·종류는 개수가 적어서 한 번에 읽는다. */
    private final class Lookup {
        final Map<Long, Building> buildings = buildingRepository.findAll().stream()
                .collect(Collectors.toMap(Building::getId, Function.identity()));
        final Map<Long, BuildingFloor> floors = floorRepository.findAll().stream()
                .collect(Collectors.toMap(BuildingFloor::getId, Function.identity()));
        final Map<Long, Place> places = placeRepository.findAll().stream()
                .collect(Collectors.toMap(Place::getId, Function.identity()));
        final Map<Long, Category> categories = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));

        Long buildingId(Report r) {
            BuildingFloor f = floors.get(r.getFloorId());
            return f == null ? null : f.getBuildingId();
        }

        String buildingName(Report r) {
            Building b = buildings.get(buildingId(r));
            return b == null ? "" : b.getName();
        }

        String floorName(Report r) {
            BuildingFloor f = floors.get(r.getFloorId());
            return f == null ? "" : f.getName();
        }

        String placeName(Report r) {
            Place p = r.getPlaceId() == null ? null : places.get(r.getPlaceId());
            return p == null ? "" : p.getName();
        }

        String categoryName(Report r) {
            Category c = categories.get(r.getCategoryId());
            return c == null ? "" : c.getName();
        }

        /** 키워드 검색 대상: 제목, 내용, 위치(건물·층·영역) */
        String searchText(Report r) {
            List<String> parts = new ArrayList<>(List.of(r.getTitle(), r.getContent(), buildingName(r),
                    floorName(r), placeName(r)));
            return String.join("\n", parts).toLowerCase(Locale.ROOT);
        }
    }
}
