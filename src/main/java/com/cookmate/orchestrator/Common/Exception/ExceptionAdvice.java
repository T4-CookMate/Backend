package com.cookmate.orchestrator.Common.Exception;

import com.cookmate.orchestrator.Common.ApiPayload.ApiResponse;
import com.cookmate.orchestrator.Common.ApiPayload.Status.ErrorStatus;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 전역 예외 처리 담당
 * @RestController에서 발생하는 예외를 전역적으로 잡아냄
 */
@Slf4j
@RestControllerAdvice
public class ExceptionAdvice {

    /**
     * JSON 형식 자체가 깨졌거나, body를 읽을 수 없을 때
     * 예: 문법 에러, 타입 완전 안 맞는 경우
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<?>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("JSON 파싱 실패: {}", e.getMessage());

        ErrorStatus status = ErrorStatus._BAD_REQUEST;

        ApiResponse<?> body = ApiResponse.onFailure(
                status,
                "요청 JSON이 잘못되었습니다. enum 값 또는 데이터 타입을 확인해주세요."
        );

        return ResponseEntity
                .status(status.getHttpStatus())
                .body(body);
    }

    /**
     * @Validated 가 붙은 쿼리 파라미터, path variable 등에서 제약 조건 위반 시 발생
     * 예: @Min(1) 인데 page=0 인 케이스
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleConstraintViolation(ConstraintViolationException e) {
        // 첫 번째 validation 메시지 하나만 사용 (원하면 여러 개 합칠 수도 있음)
        String errorMessage = e.getConstraintViolations().stream()
                .map(constraintViolation -> constraintViolation.getMessage())
                .findFirst()
                .orElse("요청 값이 유효하지 않습니다.");

        ErrorStatus status = ErrorStatus.VALIDATION_ERROR;

        ApiResponse<?> body = ApiResponse.onFailure(
                status,
                errorMessage // 여기서 실제 validation 메시지를 내려줌
        );

        return ResponseEntity
                .status(status.getHttpStatus())
                .body(body);
    }

    /**
     * @Valid @RequestBody DTO 검증 실패 시 발생
     * 예: DTO 필드에 @NotBlank, @Size 등 위반
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        // 필드 에러 중 첫 번째 메시지 가져오기
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getDefaultMessage())
                .findFirst()
                .orElse("요청 값이 유효하지 않습니다.");

        ErrorStatus status = ErrorStatus.VALIDATION_ERROR;

        ApiResponse<?> body = ApiResponse.onFailure(
                status,
                errorMessage
        );

        return ResponseEntity
                .status(status.getHttpStatus())
                .body(body);
    }

    /**
     * 존재하지 않는 URL로 접근했을 때 (Spring 6 기준 기본 404 예외)
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("NoResourceFoundException: {}", e.getMessage());

        ErrorStatus status = ErrorStatus._NOT_FOUND;

        ApiResponse<?> body = ApiResponse.onFailure(status);

        return ResponseEntity
                .status(status.getHttpStatus())
                .body(body);
    }

    /**
     * 우리가 직접 던지는 도메인 예외
     * ex) throw new GeneralException(ErrorStatus.INVALID_TOKEN, "구글 유저 정보 조회 실패");
     */
    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<ApiResponse<?>> handleGeneralException(GeneralException e) {
        ErrorStatus status = e.getErrorStatus();

        log.warn("GeneralException 발생: code={}, message={}", status.getCode(), e.getMessage());

        // e.getMessage()에 커스텀 메시지를 실어놨다면 그걸 그대로 내려주고 싶을 수도 있음
        ApiResponse<?> body = ApiResponse.onFailure(
                status,
                e.getMessage() // or null 넣으면 ErrorStatus 기본 메시지 사용
        );

        return ResponseEntity
                .status(status.getHttpStatus())
                .body(body);
    }

    /**
     * 파라미터 타입이 맞지 않을 때
     * 예: Long id 인데 /abc 로 들어온 경우
     */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, ConversionFailedException.class})
    public ResponseEntity<ApiResponse<?>> handleConversionFailedException(Exception e) {
        log.warn("타입 변환 실패: {}", e.getMessage());

        ErrorStatus status = ErrorStatus._BAD_REQUEST;
        ApiResponse<?> body = ApiResponse.onFailure(status);

        return ResponseEntity
                .status(status.getHttpStatus())
                .body(body);
    }

    /**
     * 위에서 처리하지 못한 나머지 모든 예외들
     * (예상 못 한 서버 내부 에러)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        log.error("Unhandled Exception: ", e);

        ErrorStatus status = ErrorStatus._INTERNAL_SERVER_ERROR;
        ApiResponse<?> body = ApiResponse.onFailure(status);

        return ResponseEntity
                .status(status.getHttpStatus())
                .body(body);
    }
}