package com.company.officialwebsite.modules.pagebuilder.service.impl;

import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * BindingQueryValidationHelper: 组件数据绑定 (Binding.query) 筛选入参白名单与安全性校验工具类。
 */
public class BindingQueryValidationHelper {

    private static final Logger log = LoggerFactory.getLogger(BindingQueryValidationHelper.class);

    public static final Set<String> ALLOWED_QUERY_KEYS = Set.of("id", "categoryId", "refBlockId", "ids", "limit", "pageSize", "sortOrder", "orderBy");
    public static final Set<String> ALLOWED_SORT_ORDERS = Set.of("SORT_ASC", "SORT_DESC", "CREATE_TIME_DESC", "LATEST");

    public static final int MAX_BINDING_IDS_COUNT = 50;
    public static final int MAX_BINDING_LIMIT = 50;

    /**
     * 校验组件绑定模型中的 query 参数白名单、数量与排序枚举合法性。
     */
    public static void validateBindingQuery(Map<String, Object> query, String componentCode) {
        if (query == null || query.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : query.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();

            if (key == null || key.trim().isEmpty()) {
                logAndThrow("组件 " + componentCode + " 的 Binding query 参数 Key 不能为空");
            }

            // 1. 参数 Key 白名单校验
            if (!ALLOWED_QUERY_KEYS.contains(key)) {
                logAndThrow("组件 " + componentCode + " 的 Binding query 包含非允许的参数Key: " + key + "，支持参数为: " + ALLOWED_QUERY_KEYS);
            }

            if (val == null) {
                continue;
            }

            // 2. id / categoryId / refBlockId 格式校验
            if ("id".equals(key) || "categoryId".equals(key) || "refBlockId".equals(key)) {
                try {
                    long idVal = Long.parseLong(val.toString().trim());
                    if (idVal <= 0) {
                        logAndThrow("组件 " + componentCode + " 的 Binding query." + key + " 必须为正整数");
                    }
                } catch (NumberFormatException e) {
                    logAndThrow("组件 " + componentCode + " 的 Binding query." + key + " 格式不正确");
                }
            }

            // 3. ids 集合大小上限校验 (<= 50)
            if ("ids".equals(key)) {
                if (val instanceof Collection) {
                    Collection<?> coll = (Collection<?>) val;
                    if (coll.size() > MAX_BINDING_IDS_COUNT) {
                        logAndThrow("组件 " + componentCode + " 的 Binding query.ids 单次最多查询 " + MAX_BINDING_IDS_COUNT + " 个 ID");
                    }
                } else if (val.getClass().isArray()) {
                    Object[] arr = (Object[]) val;
                    if (arr.length > MAX_BINDING_IDS_COUNT) {
                        logAndThrow("组件 " + componentCode + " 的 Binding query.ids 单次最多查询 " + MAX_BINDING_IDS_COUNT + " 个 ID");
                    }
                }
            }

            // 4. limit / pageSize 范围校验 (1 ~ 50)
            if ("limit".equals(key) || "pageSize".equals(key)) {
                try {
                    int limit = Integer.parseInt(val.toString().trim());
                    if (limit < 1 || limit > MAX_BINDING_LIMIT) {
                        logAndThrow("组件 " + componentCode + " 的 Binding query." + key + " 必须在 [1, " + MAX_BINDING_LIMIT + "] 范围内: " + limit);
                    }
                } catch (NumberFormatException e) {
                    logAndThrow("组件 " + componentCode + " 的 Binding query." + key + " 格式不正确");
                }
            }

            // 5. sortOrder / orderBy 排序枚举校验
            if ("sortOrder".equals(key) || "orderBy".equals(key)) {
                String sortVal = val.toString().trim().toUpperCase();
                if (!ALLOWED_SORT_ORDERS.contains(sortVal)) {
                    logAndThrow("组件 " + componentCode + " 的 Binding query." + key + " 排序方式不合法: " + val + "，受控枚举为: " + ALLOWED_SORT_ORDERS);
                }
            }
        }
    }

    private static void logAndThrow(String message) {
        log.warn("binding query validation failed: {}", message);
        throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, message);
    }
}
