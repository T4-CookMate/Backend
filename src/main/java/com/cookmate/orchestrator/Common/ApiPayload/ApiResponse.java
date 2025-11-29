package com.cookmate.orchestrator.Common.ApiPayload;

import com.cookmate.orchestrator.Common.ApiPayload.Status.ErrorStatus;
import com.cookmate.orchestrator.Common.ApiPayload.Status.SuccessStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * API 응답 통일
 * isSuccess : 오류/성공
 * code : 오류/성공 커스텀 코드
 * message : 오류/성공 메세지
 * result : 실제 프론트에게 줄 json
 */
@Getter
@AllArgsConstructor
@JsonPropertyOrder({"isSuccess", "code", "message", "result"})
public class ApiResponse<T> {

    @JsonProperty("isSuccess")
    private final Boolean isSuccess;
    private final String code;
    private final String message;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T result;

    // 성공 응답
    public static <T> ApiResponse<T> onSuccess(T result) {
        return new ApiResponse<>(
                true,
                SuccessStatus._OK.getCode(),
                SuccessStatus._OK.getMessage(),
                result
        );
    }

    // 실패 응답 (기본: ErrorStatus에 정의된 message 사용)
    public static <T> ApiResponse<T> onFailure(ErrorStatus errorStatus) {
        return new ApiResponse<>(
                false,
                errorStatus.getCode(),
                errorStatus.getMessage(),
                null
        );
    }

    // 실패 응답 (커스텀 메시지 사용하고 싶을 때)
    public static <T> ApiResponse<T> onFailure(ErrorStatus errorStatus, String customMessage) {
        return new ApiResponse<>(
                false,
                errorStatus.getCode(),
                customMessage != null ? customMessage : errorStatus.getMessage(),
                null
        );
    }
}
