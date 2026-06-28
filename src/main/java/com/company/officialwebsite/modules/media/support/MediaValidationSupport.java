package com.company.officialwebsite.modules.media.support;

import com.company.officialwebsite.common.config.properties.OfficialProperties;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * MediaValidationSupport：统一承接媒体上传的扩展名、MIME、文件头签名与大小校验。
 */
@Component
public class MediaValidationSupport {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp");
    private static final Set<String> DOCUMENT_EXTENSIONS = Set.of("pdf");
    private static final Set<String> ALL_ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp", "pdf");

    private final OfficialProperties officialProperties;

    public MediaValidationSupport(OfficialProperties officialProperties) {
        this.officialProperties = officialProperties;
    }

    /**
     * 对上传文件进行非空、扩展名、MIME、大小、签名五重校验，并返回识别出的媒体类型。
     */
    public String validate(MultipartFile file, byte[] fileBytes) {
        if (file == null || file.isEmpty() || fileBytes.length == 0) {
            throw new BusinessException(ErrorCode.MEDIA_FILE_INVALID, "上传文件不能为空");
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (!ALL_ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.MEDIA_FILE_TYPE_UNSUPPORTED);
        }

        String mediaType = resolveMediaType(extension);
        validateContentType(file.getContentType(), mediaType);
        validateSize(file.getSize(), mediaType);
        validateSignature(extension, fileBytes);
        return mediaType;
    }

    private String resolveMediaType(String extension) {
        if (IMAGE_EXTENSIONS.contains(extension)) {
            return "IMAGE";
        }
        if (DOCUMENT_EXTENSIONS.contains(extension)) {
            return "DOCUMENT";
        }
        throw new BusinessException(ErrorCode.MEDIA_FILE_TYPE_UNSUPPORTED);
    }

    private void validateContentType(String contentType, String mediaType) {
        if (!StringUtils.hasText(contentType)) {
            throw new BusinessException(ErrorCode.MEDIA_FILE_INVALID, "Content-Type 不能为空");
        }
        if ("IMAGE".equals(mediaType) && !contentType.startsWith("image/")) {
            throw new BusinessException(ErrorCode.MEDIA_FILE_INVALID, "图片 Content-Type 必须以 image/ 开头");
        }
        if ("DOCUMENT".equals(mediaType)) {
            boolean allowed = "application/pdf".equalsIgnoreCase(contentType)
                    || "application/x-pdf".equalsIgnoreCase(contentType);
            if (!allowed) {
                throw new BusinessException(ErrorCode.MEDIA_FILE_INVALID, "文档 Content-Type 必须为 application/pdf");
            }
        }
    }

    private void validateSize(long size, String mediaType) {
        if ("IMAGE".equals(mediaType) && size > officialProperties.getStorage().getMaxImageSizeBytes()) {
            throw new BusinessException(ErrorCode.MEDIA_FILE_SIZE_EXCEEDED, "图片大小超出限制");
        }
        if ("DOCUMENT".equals(mediaType) && size > officialProperties.getStorage().getMaxDocumentSizeBytes()) {
            throw new BusinessException(ErrorCode.MEDIA_FILE_SIZE_EXCEEDED, "文档大小超出限制");
        }
    }

    private void validateSignature(String extension, byte[] fileBytes) {
        boolean matched = switch (extension) {
            case "png" -> hasPrefix(fileBytes, new int[] {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
            case "jpg", "jpeg" -> hasPrefix(fileBytes, new int[] {0xFF, 0xD8, 0xFF});
            case "webp" -> hasWebpSignature(fileBytes);
            case "pdf" -> hasPdfSignature(fileBytes);
            default -> false;
        };
        if (!matched) {
            throw new BusinessException(ErrorCode.MEDIA_FILE_SIGNATURE_INVALID);
        }
    }

    private boolean hasPrefix(byte[] fileBytes, int[] prefix) {
        if (fileBytes.length < prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if ((fileBytes[index] & 0xFF) != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    private boolean hasWebpSignature(byte[] fileBytes) {
        if (fileBytes.length < 12) {
            return false;
        }
        return fileBytes[0] == 'R'
                && fileBytes[1] == 'I'
                && fileBytes[2] == 'F'
                && fileBytes[3] == 'F'
                && fileBytes[8] == 'W'
                && fileBytes[9] == 'E'
                && fileBytes[10] == 'B'
                && fileBytes[11] == 'P';
    }

    private boolean hasPdfSignature(byte[] fileBytes) {
        return fileBytes.length >= 5
                && fileBytes[0] == '%'
                && fileBytes[1] == 'P'
                && fileBytes[2] == 'D'
                && fileBytes[3] == 'F'
                && fileBytes[4] == '-';
    }

    private String extractExtension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            throw new BusinessException(ErrorCode.MEDIA_FILE_INVALID, "文件名缺少扩展名");
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
