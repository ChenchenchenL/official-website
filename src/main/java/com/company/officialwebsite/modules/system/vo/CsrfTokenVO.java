package com.company.officialwebsite.modules.system.vo;

/**
 * CsrfTokenVO：向后台前端返回当前会话的 CSRF Token 及提交字段名。
 */
public class CsrfTokenVO {

    private String token;
    private String headerName;
    private String parameterName;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public String getParameterName() {
        return parameterName;
    }

    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }
}
