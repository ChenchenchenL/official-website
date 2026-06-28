package com.company.officialwebsite.modules.media.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.officialwebsite.common.config.properties.OfficialProperties;
import com.company.officialwebsite.modules.media.mapper.MediaAssetMapper;
import com.company.officialwebsite.support.BaseAdminControllerIntegrationTest;
import com.company.officialwebsite.support.TestConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

/**
 * AdminMediaAssetControllerTest：验证后台统一媒体上传接口的认证、类型校验、签名校验与生命周期边界。
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminMediaAssetControllerTest extends BaseAdminControllerIntegrationTest {

    private static final byte[] MINIMAL_PDF = createMinimalPdfBytes();

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MediaAssetMapper mediaAssetMapper;

    @Autowired
    private OfficialProperties officialProperties;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM media_reference WHERE id > 0");
        jdbcTemplate.update("DELETE FROM media_asset WHERE id > 0");
    }

    @Test
    void uploadImage_shouldReturnUnauthorized_whenNotLoggedIn() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", TestConstants.PNG_BYTES);

        mockMvc.perform(multipart("/admin/api/media/assets").file(file).with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(TestConstants.AUTH_UNAUTHORIZED));
    }

    @Test
    void uploadImage_shouldRejectWhenContentSignatureInvalid() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", "not-a-real-png".getBytes());

        mockMvc.perform(multipart("/admin/api/media/assets")
                        .file(file)
                        .with(csrf())
                        .session(loginAsAdmin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.MEDIA_FILE_SIGNATURE_INVALID));
    }

    @Test
    void uploadImage_shouldRejectEmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        mockMvc.perform(multipart("/admin/api/media/assets")
                        .file(file)
                        .with(csrf())
                        .session(loginAsAdmin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.MEDIA_FILE_INVALID));
    }

    @Test
    void uploadImage_shouldRejectSvgEvenWhenMimeLooksLikeImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "logo.svg", "image/svg+xml", TestConstants.SVG_BYTES);

        mockMvc.perform(multipart("/admin/api/media/assets")
                        .file(file)
                        .with(csrf())
                        .session(loginAsAdmin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.MEDIA_FILE_TYPE_UNSUPPORTED));
    }

    @Test
    void uploadImage_shouldRejectOversizedFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.png",
                "image/png",
                TestConstants.oversizedPngBytes(officialProperties.getStorage().getMaxImageSizeBytes()));

        mockMvc.perform(multipart("/admin/api/media/assets")
                        .file(file)
                        .with(csrf())
                        .session(loginAsAdmin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.MEDIA_FILE_SIZE_EXCEEDED))
                .andExpect(jsonPath("$.message").value("图片大小超出限制"));
    }

    @Test
    void uploadImage_shouldPersistAssetWithAllFields_whenFileIsValid() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", TestConstants.PNG_BYTES);

        MvcResult result = mockMvc.perform(multipart("/admin/api/media/assets")
                        .file(file)
                        .with(csrf())
                        .session(loginAsAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data.mediaId").isNumber())
                .andExpect(jsonPath("$.data.mediaType").value("IMAGE"))
                .andExpect(jsonPath("$.data.originalFilename").value("logo.png"))
                .andExpect(jsonPath("$.data.contentType").value("image/png"))
                .andExpect(jsonPath("$.data.size").value(TestConstants.PNG_BYTES.length))
                .andExpect(jsonPath("$.data.path").isNotEmpty())
                .andExpect(jsonPath("$.data.url").value(org.hamcrest.Matchers.startsWith("/media/public/")))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        long mediaId = root.path("data").path("mediaId").asLong();
        Assertions.assertNotNull(mediaAssetMapper.selectById(mediaId));
    }

    @Test
    void uploadDocument_shouldPersistPdfAsset_whenFileIsValid() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "whitepaper.pdf", "application/pdf", MINIMAL_PDF);

        MvcResult result = mockMvc.perform(multipart("/admin/api/media/assets")
                        .file(file)
                        .with(csrf())
                        .session(loginAsAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data.mediaType").value("DOCUMENT"))
                .andExpect(jsonPath("$.data.originalFilename").value("whitepaper.pdf"))
                .andExpect(jsonPath("$.data.contentType").value("application/pdf"))
                .andExpect(jsonPath("$.data.size").value(MINIMAL_PDF.length))
                .andExpect(jsonPath("$.data.url").value(org.hamcrest.Matchers.startsWith("/media/public/")))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        long mediaId = root.path("data").path("mediaId").asLong();
        Assertions.assertNotNull(mediaAssetMapper.selectById(mediaId));
    }

    @Test
    void uploadDocument_shouldRejectPdfWithInvalidSignature() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "fake.pdf", "application/pdf", "not-a-real-pdf".getBytes());

        mockMvc.perform(multipart("/admin/api/media/assets")
                        .file(file)
                        .with(csrf())
                        .session(loginAsAdmin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.MEDIA_FILE_SIGNATURE_INVALID));
    }

    @Test
    void uploadDocument_shouldRejectOversizedPdf() throws Exception {
        long maxSize = officialProperties.getStorage().getMaxDocumentSizeBytes();
        byte[] oversizedPdf = buildPdfBytesWithSize(maxSize + 1);
        MockMultipartFile file = new MockMultipartFile(
                "file", "large.pdf", "application/pdf", oversizedPdf);

        mockMvc.perform(multipart("/admin/api/media/assets")
                        .file(file)
                        .with(csrf())
                        .session(loginAsAdmin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.MEDIA_FILE_SIZE_EXCEEDED))
                .andExpect(jsonPath("$.message").value("文档大小超出限制"));
    }

    @Test
    void uploadDocument_shouldRejectNonPdfDocumentType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.doc", "application/msword", "binary-content".getBytes());

        mockMvc.perform(multipart("/admin/api/media/assets")
                        .file(file)
                        .with(csrf())
                        .session(loginAsAdmin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.MEDIA_FILE_TYPE_UNSUPPORTED));
    }

    @Test
    void uploadDocument_shouldRejectExecutableType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "script.exe", "application/octet-stream", new byte[] {0x4D, 0x5A});

        mockMvc.perform(multipart("/admin/api/media/assets")
                        .file(file)
                        .with(csrf())
                        .session(loginAsAdmin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.MEDIA_FILE_TYPE_UNSUPPORTED));
    }

    @Test
    void uploadImage_shouldWriteAuditLog() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", TestConstants.PNG_BYTES);

        mockMvc.perform(multipart("/admin/api/media/assets")
                        .file(file)
                        .with(csrf())
                        .session(loginAsAdmin()))
                .andExpect(status().isOk());

        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_audit_log WHERE module_name = 'MEDIA' AND action_name = 'UPLOAD_MEDIA'",
                Integer.class);
        Assertions.assertNotNull(auditCount);
        Assertions.assertTrue(auditCount > 0, "审计日志应存在 UPLOAD_MEDIA 记录");
    }

    @Test
    void uploadImage_shouldSetTemporaryStatus() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", TestConstants.PNG_BYTES);

        MvcResult result = mockMvc.perform(multipart("/admin/api/media/assets")
                        .file(file)
                        .with(csrf())
                        .session(loginAsAdmin()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        long mediaId = root.path("data").path("mediaId").asLong();
        String status = jdbcTemplate.queryForObject(
                "SELECT status FROM media_asset WHERE id = ?", String.class, mediaId);
        Assertions.assertEquals("TEMPORARY", status, "上传后媒体状态应为 TEMPORARY");
    }

    @Test
    void uploadDocument_shouldNotReturnAbsoluteUrl_whenPublicDomainNotConfigured() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "whitepaper.pdf", "application/pdf", MINIMAL_PDF);

        mockMvc.perform(multipart("/admin/api/media/assets")
                        .file(file)
                        .with(csrf())
                        .session(loginAsAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.absoluteUrl").doesNotExist());
    }

    private static byte[] createMinimalPdfBytes() {
        return "%PDF-1.4 minimal".getBytes();
    }

    private static byte[] buildPdfBytesWithSize(long targetSize) {
        byte[] header = "%PDF-1.4 ".getBytes();
        byte[] padding = new byte[Math.toIntExact(targetSize - header.length)];
        byte[] result = new byte[Math.toIntExact(targetSize)];
        System.arraycopy(header, 0, result, 0, header.length);
        System.arraycopy(padding, 0, result, header.length, padding.length);
        return result;
    }
}
