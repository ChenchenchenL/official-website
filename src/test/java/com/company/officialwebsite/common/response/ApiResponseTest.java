package com.company.officialwebsite.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.officialwebsite.common.enums.ErrorCode;
import org.junit.jupiter.api.Test;

/**
 * ApiResponseTest：验证统一响应类的成功和失败响应契约。
 */
class ApiResponseTest {

    @Test
    void success_shouldUseSuccessCode() {
        ApiResponse<String> response = ApiResponse.success("ok");

        assertThat(response.getCode()).isEqualTo(ErrorCode.SUCCESS.getCode());
        assertThat(response.getMessage()).isEqualTo(ErrorCode.SUCCESS.getDefaultMessage());
        assertThat(response.getData()).isEqualTo("ok");
    }

    @Test
    void fail_shouldUseErrorCodeAndMessage() {
        ApiResponse<Void> response = ApiResponse.fail(ErrorCode.COMMON_PARAM_INVALID);

        assertThat(response.getCode()).isEqualTo(ErrorCode.COMMON_PARAM_INVALID.getCode());
        assertThat(response.getMessage()).isEqualTo(ErrorCode.COMMON_PARAM_INVALID.getDefaultMessage());
        assertThat(response.getData()).isNull();
    }
}
