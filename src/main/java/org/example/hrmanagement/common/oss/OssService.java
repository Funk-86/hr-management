package org.example.hrmanagement.common.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.hrmanagement.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OssService {

    private static final DateTimeFormatter KEY_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OSS ossClient;
    private final OssProperties properties;

    /**
     * 上传文件到 OSS，返回对象 Key（不含域名）。
     */
    public String upload(InputStream inputStream, long contentLength, String contentType, String objectKey) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(contentLength);
            if (StringUtils.hasText(contentType)) {
                metadata.setContentType(contentType);
            }
            ossClient.putObject(properties.getBucketName(), objectKey, inputStream, metadata);
            return objectKey;
        } catch (Exception e) {
            log.error("OSS 上传失败, key={}", objectKey, e);
            throw new BusinessException("文件上传失败，请稍后重试");
        }
    }

    /**
     * 删除 OSS 对象；删除失败仅记录日志，不抛异常。
     */
    public void deleteIfExists(String objectKey) {
        if (!StringUtils.hasText(objectKey) || isFullUrl(objectKey)) {
            return;
        }
        String defaultKey = properties.getAvatar().getDefaultKey();
        if (objectKey.equals(defaultKey)) {
            return;
        }
        try {
            if (ossClient.doesObjectExist(properties.getBucketName(), objectKey)) {
                ossClient.deleteObject(properties.getBucketName(), objectKey);
            }
        } catch (Exception e) {
            log.warn("OSS 删除旧头像失败, key={}", objectKey, e);
        }
    }

    /**
     * 生成对象 Key，例如 avatar/employee/3/20260624153000_a1b2c3d4.jpg
     */
    public String buildObjectKey(String prefix, Long ownerId, String extension) {
        String normalizedPrefix = prefix.endsWith("/") ? prefix : prefix + "/";
        String timePart = LocalDateTime.now().format(KEY_TIME_FORMAT);
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return normalizedPrefix + ownerId + "/" + timePart + "_" + uuid + extension;
    }

    /**
     * 按 Key 读取 OSS 对象（调用方负责关闭返回的流）。
     */
    public OSSObject getObject(String objectKey) {
        if (!StringUtils.hasText(objectKey) || isFullUrl(objectKey)) {
            throw new BusinessException("文件不存在或无法访问");
        }
        try {
            if (!ossClient.doesObjectExist(properties.getBucketName(), objectKey)) {
                throw new BusinessException("文件不存在或已被删除");
            }
            return ossClient.getObject(properties.getBucketName(), objectKey);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("OSS 读取失败, key={}", objectKey, e);
            throw new BusinessException("文件读取失败，请稍后重试");
        }
    }

    /**
     * 将 OSS Key 转为可访问 URL；若已是完整 URL 则原样返回。
     */
    public String toPublicUrl(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return toPublicUrl(properties.getAvatar().getDefaultKey());
        }
        if (isFullUrl(objectKey)) {
            return objectKey;
        }
        String domain = properties.getPublicDomain();
        if (!StringUtils.hasText(domain)) {
            throw new BusinessException("OSS 访问域名未配置");
        }
        if (!domain.startsWith("http://") && !domain.startsWith("https://")) {
            domain = "https://" + domain;
        }
        if (domain.endsWith("/")) {
            domain = domain.substring(0, domain.length() - 1);
        }
        return domain + "/" + objectKey;
    }

    public String getDefaultKey() {
        return properties.getAvatar().getDefaultKey();
    }

    private boolean isFullUrl(String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }
}
