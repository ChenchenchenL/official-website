package com.company.officialwebsite.infrastructure.storage;

import com.company.officialwebsite.common.config.properties.OfficialProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * LocalMediaStorageService：负责本地媒体文件的落盘和失败清理。
 */
@Service
public class LocalMediaStorageService {

    private static final DateTimeFormatter DIRECTORY_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM");

    private final OfficialProperties officialProperties;

    public LocalMediaStorageService(OfficialProperties officialProperties) {
        this.officialProperties = officialProperties;
    }

    /**
     * 统一生成存储路径，避免业务模块直接使用原始文件名作为最终路径。
     */
    public String storeImage(byte[] content, String extension) throws IOException {
        return storeFile(content, extension);
    }

    /**
     * 通用文件落盘方法，支撑图片与文档等多类型资源统一存储。
     */
    public String storeFile(byte[] content, String extension) throws IOException {
        String normalizedExtension = extension.startsWith(".") ? extension.substring(1) : extension;
        String relativeDirectory = DIRECTORY_FORMATTER.format(LocalDate.now());
        String storedFilename = UUID.randomUUID().toString().replace("-", "") + "." + normalizedExtension;
        Path directory = Paths.get(officialProperties.getStorage().getLocalRootDir(), relativeDirectory);
        Files.createDirectories(directory);
        Path storedPath = directory.resolve(storedFilename);
        Files.write(storedPath, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        return relativeDirectory + "/" + storedFilename;
    }

    /**
     * 数据库存储失败时尽力回收孤儿文件，避免媒体目录累积垃圾数据。
     */
    public void deleteQuietly(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(Paths.get(officialProperties.getStorage().getLocalRootDir(), relativePath));
        } catch (IOException ignored) {
        }
    }
}
