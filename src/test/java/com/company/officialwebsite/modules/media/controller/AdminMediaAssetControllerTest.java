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
 * AdminMediaAssetControllerTest：验证后台媒体上传接口的认证与边界行为。
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminMediaAssetControllerTest extends BaseAdminControllerIntegrationTest {

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
                .andExpect(jsonPath("$.code").value(TestConstants.MEDIA_FILE_INVALID));
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
                .andExpect(jsonPath("$.code").value(TestConstants.MEDIA_FILE_INVALID));
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
                .andExpect(jsonPath("$.code").value(TestConstants.MEDIA_FILE_INVALID))
                .andExpect(jsonPath("$.message").value("图片大小超出限制"));
    }

    @Test
    void uploadImage_shouldPersistAsset_whenFileIsValid() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", TestConstants.PNG_BYTES);

        MvcResult result = mockMvc.perform(multipart("/admin/api/media/assets")
                        .file(file)
                        .with(csrf())
                        .session(loginAsAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data.url").value(org.hamcrest.Matchers.startsWith("/media/public/")))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        long mediaId = root.path("data").path("mediaId").asLong();
        Assertions.assertNotNull(mediaAssetMapper.selectById(mediaId));
    }
}
