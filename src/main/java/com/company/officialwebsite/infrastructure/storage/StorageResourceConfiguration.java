package com.company.officialwebsite.infrastructure.storage;

import com.company.officialwebsite.common.config.properties.OfficialProperties;
import java.nio.file.Paths;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * StorageResourceConfiguration：暴露本地媒体目录的只读访问路径。
 */
@Configuration
public class StorageResourceConfiguration implements WebMvcConfigurer {

    private final OfficialProperties officialProperties;

    public StorageResourceConfiguration(OfficialProperties officialProperties) {
        this.officialProperties = officialProperties;
    }

    /**
     * 将统一配置的公开前缀映射到本地媒体根目录，避免业务代码自行拼装静态资源映射。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String prefix = officialProperties.getStorage().getPublicUrlPrefix();
        String normalizedPrefix = prefix.endsWith("/") ? prefix : prefix + "/";
        registry.addResourceHandler(normalizedPrefix + "**")
                .addResourceLocations(Paths.get(officialProperties.getStorage().getLocalRootDir()).toUri().toString());
    }
}
