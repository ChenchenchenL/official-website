package com.company.officialwebsite.common.response;

import java.util.ArrayList;
import java.util.List;

/**
 * ValidationErrorData：统一承载参数校验失败时的字段级错误集合。
 */
public class ValidationErrorData {

    private List<FieldErrorDetail> fieldErrors = new ArrayList<>();

    public ValidationErrorData() {
    }

    public ValidationErrorData(List<FieldErrorDetail> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }

    public List<FieldErrorDetail> getFieldErrors() {
        return fieldErrors;
    }

    public void setFieldErrors(List<FieldErrorDetail> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }
}
