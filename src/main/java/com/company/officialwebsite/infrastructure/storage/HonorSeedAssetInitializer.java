package com.company.officialwebsite.infrastructure.storage;

import com.company.officialwebsite.common.config.properties.OfficialProperties;
import com.company.officialwebsite.common.enums.MediaAssetStatusEnum;
import com.company.officialwebsite.modules.media.entity.MediaAssetEntity;
import com.company.officialwebsite.modules.media.mapper.MediaAssetMapper;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * HonorSeedAssetInitializer：为默认荣誉种子补齐本地媒体文件与元数据，避免启动后出现断链图标。
 */
@Component
public class HonorSeedAssetInitializer {

    private static final byte[] PNG_BYTES = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x02, 0x00, 0x00, 0x00, (byte) 0x90, 0x77, 0x53, (byte) 0xDE,
            0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41, 0x54,
            0x08, (byte) 0xD7, 0x63, (byte) 0xF8, (byte) 0xCF, (byte) 0xC0, 0x00, 0x00,
            0x03, 0x01, 0x01, 0x00, 0x18, (byte) 0xDD, (byte) 0x8D, (byte) 0xB0,
            0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,
            (byte) 0xAE, 0x42, 0x60, (byte) 0x82
    };

    private final OfficialProperties officialProperties;
    private final MediaAssetMapper mediaAssetMapper;

    public HonorSeedAssetInitializer(OfficialProperties officialProperties, MediaAssetMapper mediaAssetMapper) {
        this.officialProperties = officialProperties;
        this.mediaAssetMapper = mediaAssetMapper;
    }

    @PostConstruct
    public void initialize() {
        List<SeedMedia> seedMediaList = List.of(
                new SeedMedia(-9101L, "national-high-tech-enterprise.png"),
                new SeedMedia(-9102L, "hubei-science-innovation-enterprise.png"),
                new SeedMedia(-9103L, "hubei-artificial-intelligence-enterprise.png"),
                new SeedMedia(-9104L, "china-optics-valley-3551-enterprise.png"));
        for (SeedMedia seedMedia : seedMediaList) {
            MediaAssetEntity entity = mediaAssetMapper.selectById(seedMedia.mediaId());
            if (entity == null || entity.getDeletedMarker() == null || entity.getDeletedMarker() != 0L) {
                continue;
            }
            writeSeedFile(seedMedia.filename());
            entity.setStoragePath("seed/honors/" + seedMedia.filename());
            entity.setPublicUrl(buildPublicUrl(entity.getStoragePath()));
            entity.setContentType("image/png");
            entity.setFileSize((long) PNG_BYTES.length);
            entity.setStatus(MediaAssetStatusEnum.BOUND.getCode());
            mediaAssetMapper.updateById(entity);
        }
    }

    private void writeSeedFile(String filename) {
        Path path = Paths.get(officialProperties.getStorage().getLocalRootDir(), "seed", "honors", filename);
        try {
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                Files.write(path, PNG_BYTES, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to initialize honor seed icon: " + filename, ex);
        }
    }

    private String buildPublicUrl(String relativePath) {
        String prefix = officialProperties.getStorage().getPublicUrlPrefix();
        String normalizedPrefix = prefix.endsWith("/") ? prefix : prefix + "/";
        return normalizedPrefix + relativePath;
    }

    private record SeedMedia(Long mediaId, String filename) {
    }
}
