package com.company.officialwebsite.common.utils;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.officialwebsite.common.entity.BaseEntity;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import java.util.Objects;

public final class ConcurrencyHelper {

    public static final String STATE_CONFLICT_MSG = "数据已被其他操作更新，请刷新后重试";

    public static void assertVersion(Integer currentVersion, Integer requestVersion) {
        if (requestVersion == null || requestVersion < 0) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "版本号不能为负数");
        }
        if (!Objects.equals(currentVersion, requestVersion)) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, STATE_CONFLICT_MSG);
        }
    }

    public static <T extends BaseEntity> void tryUpdate(BaseMapper<T> mapper, T entity) {
        Integer requestVersion = entity.getVersion();
        int updated = mapper.updateById(entity);
        if (updated != 1) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, STATE_CONFLICT_MSG);
        }
        if (entity.getVersion() == null || entity.getVersion().equals(requestVersion)) {
            entity.setVersion((requestVersion == null ? 0 : requestVersion) + 1);
        }
    }

    private ConcurrencyHelper() {}
}
