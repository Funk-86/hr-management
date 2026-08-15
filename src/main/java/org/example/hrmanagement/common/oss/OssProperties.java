package org.example.hrmanagement.common.oss;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssProperties {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
    /** 对外访问域名，可填 CDN 或 Bucket 域名，带或不带 https:// 均可 */
    private String publicDomain;
    private Avatar avatar = new Avatar();
    private Attachment attachment = new Attachment();
    private Document document = new Document();

    @Data
    public static class Avatar {
        /** 最大文件大小（字节） */
        private long maxSize = 2_097_152L;
        /** 允许的 Content-Type，逗号分隔 */
        private String allowedTypes = "image/jpeg,image/png,image/webp";
        private String employeePrefix = "avatar/employee/";
        private String userPrefix = "avatar/user/";
        private String defaultKey = "avatar/default/default-avatar.png";
    }

    @Data
    public static class Attachment {
        /** 最大文件大小（字节），默认 10MB */
        private long maxSize = 10_485_760L;
        private String allowedTypes =
                "image/jpeg,image/png,image/webp,application/pdf,application/msword,"
                        + "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        private String prefix = "task/attachment/";
    }

    @Data
    public static class Document {
        /** 最大文件大小（字节），默认 20MB */
        private long maxSize = 20_971_520L;
        private String allowedTypes =
                "application/pdf,application/msword,"
                        + "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        private String prefix = "employee/document/";
    }
}
