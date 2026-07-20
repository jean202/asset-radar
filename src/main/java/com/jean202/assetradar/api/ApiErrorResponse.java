package com.jean202.assetradar.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "공통 API 에러 응답")
public record ApiErrorResponse(
        @Schema(description = "에러 발생 시각", example = "2026-05-17T10:15:30Z")
        Instant timestamp,
        @Schema(description = "HTTP status code", example = "400")
        int status,
        @Schema(description = "HTTP reason phrase", example = "Bad Request")
        String error,
        @Schema(description = "애플리케이션 에러 코드", example = "INVALID_REQUEST")
        String code,
        @Schema(description = "상세 에러 메시지", example = "period must be one of Ns, Nm, Nh, Nd, Nw")
        String message,
        @Schema(description = "요청 경로", example = "/api/compare")
        String path
) {
}
