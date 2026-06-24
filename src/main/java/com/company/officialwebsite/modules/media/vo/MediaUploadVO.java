package com.company.officialwebsite.modules.media.vo;

/**
 * MediaUploadVO：后台媒体上传成功后的返回结果。
 */
public class MediaUploadVO {

    private Long mediaId;
    private String url;
    private String contentType;
    private Long size;

    public Long getMediaId() {
        return mediaId;
    }

    public void setMediaId(Long mediaId) {
        this.mediaId = mediaId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }
}
