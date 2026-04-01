package com.polo.boot.storage.config;

import com.aliyun.oss.OSS;
import com.polo.boot.storage.properties.CosProperties;
import com.polo.boot.storage.properties.LocalProperties;
import com.polo.boot.storage.properties.MinioProperties;
import com.polo.boot.storage.properties.OSSProperties;
import com.polo.boot.storage.properties.StorageProperties;
import com.polo.boot.storage.resolver.UploadFileArgumentResolver;
import com.polo.boot.storage.service.impl.CosFileStorage;
import com.polo.boot.storage.service.FileStorage;
import com.polo.boot.storage.service.FileUploadService;
import com.polo.boot.storage.service.ChunkUploadService;
import com.polo.boot.storage.service.impl.DefaultFileUploadService;
import com.polo.boot.storage.service.impl.FileSystemChunkUploadService;
import com.polo.boot.storage.service.impl.LocalFileStorage;
import com.polo.boot.storage.service.impl.MinioFileStorage;
import com.polo.boot.storage.service.impl.OssFileStorage;
import com.polo.boot.storage.service.impl.FileStorageFactory;
import com.polo.boot.storage.support.ThumbnailGenerator;
import io.minio.MinioClient;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@AutoConfiguration
@EnableConfigurationProperties({StorageProperties.class, MinioProperties.class, OSSProperties.class, CosProperties.class, LocalProperties.class})
@ConditionalOnProperty(prefix = "polo.storage", name = "enabled", havingValue = "true", matchIfMissing = true)
public class StorageAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(MinioClient.class)
    @ConditionalOnProperty(prefix = "polo.storage.minio", name = "minio-enabled", havingValue = "true", matchIfMissing = true)
    public static class MinioStorageConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public MinioClient minioClient(MinioProperties minioProperties) {
            return MinioClient.builder()
                    .endpoint(minioProperties.getEndpoint())
                    .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                    .build();
        }

        @Bean
        @ConditionalOnMissingBean(MinioFileStorage.class)
        @ConditionalOnBean(MinioClient.class)
        public MinioFileStorage minioFileStorage(MinioClient minioClient, MinioProperties minioProperties) {
            return new MinioFileStorage(minioClient, minioProperties);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(OSS.class)
    @ConditionalOnProperty(prefix = "polo.storage.oss", name = "oss-enabled", havingValue = "true")
    public static class OssStorageConfiguration {

        @Bean
        @ConditionalOnMissingBean(OssFileStorage.class)
        @ConditionalOnBean(OSS.class)
        public OssFileStorage ossFileStorage(OSS oss, OSSProperties ossProperties) {
            return new OssFileStorage(oss, ossProperties);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(COSClient.class)
    @ConditionalOnProperty(prefix = "polo.storage.cos", name = "cos-enabled", havingValue = "true")
    public static class CosStorageConfiguration {

        @Bean(destroyMethod = "shutdown")
        @ConditionalOnMissingBean
        public COSClient cosClient(CosProperties cosProperties) {
            COSCredentials credentials = new BasicCOSCredentials(cosProperties.getSecretId(), cosProperties.getSecretKey());
            ClientConfig clientConfig = new ClientConfig(new Region(cosProperties.getRegion()));
            return new COSClient(credentials, clientConfig);
        }

        @Bean
        @ConditionalOnMissingBean(CosFileStorage.class)
        @ConditionalOnBean(COSClient.class)
        public CosFileStorage cosFileStorage(COSClient cosClient, CosProperties cosProperties) {
            return new CosFileStorage(cosClient, cosProperties);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "polo.storage.local", name = "local-enabled", havingValue = "true")
    public static class LocalStorageConfiguration {

        @Bean
        @ConditionalOnMissingBean(LocalFileStorage.class)
        public LocalFileStorage localFileStorage(LocalProperties localProperties) {
            return new LocalFileStorage(localProperties);
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public FileStorageFactory fileStorageFactory(StorageProperties storageProperties, List<FileStorage> storages) {
        return new FileStorageFactory(storageProperties, storages);
    }

    @Bean
    @ConditionalOnMissingBean
    public ThumbnailGenerator thumbnailGenerator() {
        return new ThumbnailGenerator();
    }

    @Bean
    @ConditionalOnMissingBean
    public FileUploadService fileUploadService(FileStorageFactory fileStorageFactory,
                                               ThumbnailGenerator thumbnailGenerator,
                                               StorageProperties storageProperties) {
        return new DefaultFileUploadService(fileStorageFactory, thumbnailGenerator, storageProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "polo.storage.chunk", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ChunkUploadService chunkUploadService(FileUploadService fileUploadService, StorageProperties storageProperties) {
        return new FileSystemChunkUploadService(fileUploadService, storageProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public UploadFileArgumentResolver uploadFileArgumentResolver(FileUploadService fileUploadService) {
        return new UploadFileArgumentResolver(fileUploadService);
    }

    @Bean
    @ConditionalOnMissingBean(name = "storageWebMvcConfigurer")
    public WebMvcConfigurer storageWebMvcConfigurer(UploadFileArgumentResolver uploadFileArgumentResolver) {
        return new WebMvcConfigurer() {
            @Override
            public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
                resolvers.add(uploadFileArgumentResolver);
            }
        };
    }
}
