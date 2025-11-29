package com.cookmate.orchestrator.Common.ApiPayload.Status;

import com.cookmate.orchestrator.Common.ApiPayload.BaseCode;
import com.cookmate.orchestrator.Common.ApiPayload.ReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorStatus implements BaseCode {

    // COMMON ERROR
    _INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON500", "서버 에러, 관리자에게 문의 바랍니다."),
    _BAD_REQUEST(HttpStatus.BAD_REQUEST,"COMMON400","잘못된 요청입니다."),
    _UNAUTHORIZED(HttpStatus.UNAUTHORIZED,"COMMON401","인증이 필요합니다."),
    _FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON403", "금지된 요청입니다."),
    _NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON404", "페이지를 찾을 수 없습니다."),
    _NULL_JSON(HttpStatus.BAD_REQUEST, "COMMON400", "JSON에 내용이 존재하지 않습니다."),

    // OAuth ERROR
    INVALID_TOKEN(HttpStatus.BAD_REQUEST, "AUTH402", "AccessToken이 올바르지 않습니다."),
    INVALID_AUTHORIZATION_CODE(HttpStatus.UNAUTHORIZED, "AUTH401", "Authorization Code가 올바르지 않습니다."),

    //
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "VALID401", "입력값이 올바르지 않습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ReasonDTO getReason() {
        return ReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .build();
    }

    @Override
    public ReasonDTO getReasonHttpStatus() {
        return ReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .httpStatus(httpStatus)
                .build()
                ;
    }
}