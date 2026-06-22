package com.company.officialwebsite.common.enums;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * ErrorCodeTest：验证错误码枚举的唯一性和成功码约定。
 */
class ErrorCodeTest {

    @Test
    void values_shouldHaveUniqueNumericCodes() {
        int distinctCount = Arrays.stream(ErrorCode.values())
                .map(ErrorCode::getCode)
                .collect(Collectors.toSet())
                .size();

        assertThat(distinctCount).isEqualTo(ErrorCode.values().length);
    }

    @Test
    void success_shouldUseZero() {
        assertThat(ErrorCode.SUCCESS.getCode()).isZero();
        assertThat(ErrorCode.SUCCESS.isSuccess()).isTrue();
    }
}
