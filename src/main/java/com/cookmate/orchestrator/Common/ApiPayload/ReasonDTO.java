package com.cookmate.orchestrator.Common.ApiPayload;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@Builder
public class ReasonDTO {

    private HttpStatus httpStatus;

    private final boolean isSuccess;  // 성공여부
    private final String code;        // 커스텀 에러 코드
    private final String message;     // 에러 메세지
}
