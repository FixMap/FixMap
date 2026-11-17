package com.fixmap.service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

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

/** 신고 작성(REPAIR-1)과 첨부 파일(REPAIR-2). 신고와 첨부는 한 요청, 한 트랜잭션으로 저장한다. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReportService {

    /** 신고 1건에 붙일 수 있는 첨부 파일 수 */
    public static final int MAX_FILES_PER_REPORT = 5;

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

    /**
     * 신고 등록. 처리 상태는 '접수'로 시작하고, 영역(place)은 핀 좌표로 서버가 정한다.
     * 첨부 파일을 먼저 모두 검사한 뒤 저장하고, 중간에 실패하면 DB는 되돌리고 이미 쓴 파일은 지운다.
     */
    @Transactional
    public ReportView create(NewReport input, List<MultipartFile> files) {
        if (!floorRepository.existsById(input.floorId())) {
            throw badRequest("층을 찾을 수 없습니다");
        }
        Category category = categoryRepository.findById(input.categoryId())
                .orElseThrow(() -> badRequest("고장 종류를 찾을 수 없습니다"));
        List<Upload> uploads = checkUploads(files);

        BigDecimal posX = ratio(input.posX());
        BigDecimal posY = ratio(input.posY());
        Place place = findPlaceAt(input.floorId(), posX, posY);

        Report report = reportRepository.save(Report.create(
                input.floorId(), place == null ? null : place.getId(), category.getId(),
                input.title().strip(), input.content().strip(), posX, posY));

        List<String> written = new ArrayList<>();
        try {
            List<ReportFile> saved = new ArrayList<>();
            for (Upload upload : uploads) {
                String storedPath = storage.store(upload.file(), upload.type().extension);
                written.add(storedPath);
                saved.add(fileRepository.save(ReportFile.create(report.getId(), originalName(upload.file()),
                        storedPath, upload.type().contentType, upload.file().getSize())));
            }
            fileRepository.flush();
            return new ReportView(report, category.getName(), place == null ? null : place.getName(), saved);
        } catch (IOException | RuntimeException e) {
            written.forEach(storage::deleteQuietly);
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

    /**
     * 핀이 들어간 영역. 여러 영역에 걸치면 가장 작은 영역(교실이 복도보다 먼저)을, 없으면 null.
     * 영역 경계 위의 점도 그 영역에 넣는다.
     */
    private Place findPlaceAt(Long floorId, BigDecimal x, BigDecimal y) {
        return placeRepository.findByFloorIdOrderByIdAsc(floorId).stream()
                .filter(p -> x.compareTo(p.getX()) >= 0 && x.compareTo(p.getX().add(p.getWidth())) <= 0
                        && y.compareTo(p.getY()) >= 0 && y.compareTo(p.getY().add(p.getHeight())) <= 0)
                .min(Comparator.comparing((Place p) -> p.getWidth().multiply(p.getHeight()))
                        .thenComparing(Place::getId))
                .orElse(null);
    }

    /** 빈 파일 칸은 버리고, 개수와 형식(파일 앞부분으로 판단)을 검사한다. 문제가 있으면 400 */
    private List<Upload> checkUploads(List<MultipartFile> files) {
        List<MultipartFile> nonEmpty = files == null ? List.of()
                : files.stream().filter(f -> f != null && !f.isEmpty()).toList();
        if (nonEmpty.size() > MAX_FILES_PER_REPORT) {
            throw badRequest("첨부 파일은 " + MAX_FILES_PER_REPORT + "개까지 올릴 수 있습니다");
        }
        List<Upload> uploads = new ArrayList<>();
        for (MultipartFile file : nonEmpty) {
            ImageType type = ImageType.detect(file);
            if (type == null) {
                throw badRequest("사진 파일(jpg, png, gif, webp)만 올릴 수 있습니다");
            }
            uploads.add(new Upload(file, type));
        }
        return uploads;
    }

    /** 좌표를 DB 자리수(소수 4자리)에 맞춘다. */
    private static BigDecimal ratio(BigDecimal value) {
        return value.setScale(4, RoundingMode.HALF_UP);
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

    /**
     * 받을 수 있는 사진 형식. 형식 제한은 팀 미정이라 사진만 받는다.
     * 브라우저가 보낸 Content-Type은 믿지 않고 파일 앞부분(매직 바이트)으로 판단한다.
     */
    enum ImageType {
        JPEG("image/jpeg", "jpg"),
        PNG("image/png", "png"),
        GIF("image/gif", "gif"),
        WEBP("image/webp", "webp");

        final String contentType;
        final String extension;

        ImageType(String contentType, String extension) {
            this.contentType = contentType;
            this.extension = extension;
        }

        static ImageType detect(MultipartFile file) {
            byte[] head = new byte[12];
            int read;
            try (InputStream in = file.getInputStream()) {
                read = in.readNBytes(head, 0, head.length);
            } catch (IOException e) {
                return null;
            }
            if (read >= 3 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF) {
                return JPEG;
            }
            if (read >= 8 && Arrays.equals(Arrays.copyOf(head, 8),
                    new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'})) {
                return PNG;
            }
            String ascii = new String(head, 0, read, StandardCharsets.ISO_8859_1);
            if (ascii.startsWith("GIF87a") || ascii.startsWith("GIF89a")) {
                return GIF;
            }
            if (read >= 12 && ascii.startsWith("RIFF") && ascii.substring(8, 12).equals("WEBP")) {
                return WEBP;
            }
            return null;
        }
    }

    private record Upload(MultipartFile file, ImageType type) {
    }

    /** 신고 등록에 필요한 값. 필수 여부와 길이 검사는 컨트롤러의 요청 객체가 먼저 한다. */
    public record NewReport(Long floorId, Long categoryId, String title, String content,
                            BigDecimal posX, BigDecimal posY) {
    }

    public record ReportView(Report report, String categoryName, String placeName, List<ReportFile> files) {
    }

    public record StoredFile(ReportFile file, Path path) {
    }
}
