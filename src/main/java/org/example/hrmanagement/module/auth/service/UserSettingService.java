package org.example.hrmanagement.module.auth.service;

import org.example.hrmanagement.module.auth.dto.MfaConfirmDTO;
import org.example.hrmanagement.module.auth.dto.NotificationSettingDTO;
import org.example.hrmanagement.module.auth.dto.SecurityQuestionDTO;
import org.example.hrmanagement.module.auth.dto.SecurityToggleDTO;
import org.example.hrmanagement.module.auth.vo.MfaSetupVO;
import org.example.hrmanagement.module.auth.vo.UserSettingVO;

public interface UserSettingService {

    UserSettingVO getMine();

    void updateNotification(NotificationSettingDTO dto);

    void toggleSecurity(SecurityToggleDTO dto);

    void setSecurityQuestion(SecurityQuestionDTO dto);

    MfaSetupVO setupMfa();

    void confirmMfa(MfaConfirmDTO dto);

    /** 用户是否允许接收该业务类型的站内信 */
    boolean allowsNotification(Long userId, String bizType);

    /** MFA 已开启时校验验证码 */
    void assertMfaIfRequired(Long userId, String mfaCode);
}
