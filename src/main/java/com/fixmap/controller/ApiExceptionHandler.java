package com.fixmap.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

/** JSON API의 오류 응답을 {"message": "..."} 형식으로 통일한다. */
@RestControllerAdvice(annotations = org.springframework.web.bind.annotation.RestController.class)
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> status(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", String.valueOf(e.getReason())));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> missing(MissingServletRequestParameterException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", e.getParameterName() + " 파라미터가 필요합니다"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> mismatch(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", e.getName() + " 값이 올바르지 않습니다"));
    }

    /**
     * 요청 검사 실패(필수 항목 누락 등): 첫 번째 오류 메시지를 돌려준다.
     * BindException은 폼(@ModelAttribute)과 JSON(@RequestBody, MethodArgumentNotValidException) 검사 실패를 모두 받는다.
     * 숫자 칸에 글자가 오는 등 변환 실패는 "{필드} 값이 올바르지 않습니다"로 바꾼다.
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, String>> invalid(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.isBindingFailure() ? f.getField() + " 값이 올바르지 않습니다" : f.getDefaultMessage())
                .orElse("입력값이 올바르지 않습니다");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", message));
    }

    /** JSON 본문이 없거나 형식이 틀렸을 때 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> unreadable(HttpMessageNotReadableException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "요청 형식이 올바르지 않습니다"));
    }

    /** JSON 본문을 받는 API에 다른 Content-Type을 보냈을 때. 기본 HTML 오류 페이지 대신 JSON */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, String>> unsupported(HttpMediaTypeNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(Map.of("message", "요청 형식(Content-Type)이 올바르지 않습니다"));
    }

    /** 파일 크기 제한(WebAppInitializer: 1개 10MB, 요청 35MB)을 넘었을 때 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> tooLarge(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of("message", "파일이 너무 큽니다 (1개 최대 10MB)"));
    }
}
