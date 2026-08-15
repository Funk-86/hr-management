package org.example.hrmanagement.module.auth.vo;

import lombok.Data;

@Data
public class MfaSetupVO {
    /** Base32 密钥，供 Authenticator 手动录入 */
    private String secret;
    /** otpauth:// URI，前端可生成二维码 */
    private String otpauthUrl;
}
