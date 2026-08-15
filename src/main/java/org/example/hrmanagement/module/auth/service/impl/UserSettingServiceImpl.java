package org.example.hrmanagement.module.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.hrmanagement.common.exception.BusinessException;
import org.example.hrmanagement.common.util.SecurityUtil;
import org.example.hrmanagement.common.util.TotpUtil;
import org.example.hrmanagement.module.auth.dto.MfaConfirmDTO;
import org.example.hrmanagement.module.auth.dto.NotificationSettingDTO;
import org.example.hrmanagement.module.auth.dto.SecurityQuestionDTO;
import org.example.hrmanagement.module.auth.dto.SecurityToggleDTO;
import org.example.hrmanagement.module.auth.entity.User;
import org.example.hrmanagement.module.auth.entity.UserSetting;
import org.example.hrmanagement.module.auth.mapper.UserMapper;
import org.example.hrmanagement.module.auth.mapper.UserSettingMapper;
import org.example.hrmanagement.module.auth.service.UserSettingService;
import org.example.hrmanagement.module.auth.vo.MfaSetupVO;
import org.example.hrmanagement.module.auth.vo.UserSettingVO;
import org.example.hrmanagement.module.employee.entity.Employee;
import org.example.hrmanagement.module.employee.mapper.EmployeeMapper;
import org.example.hrmanagement.module.notification.constant.NotificationBizType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserSettingServiceImpl implements UserSettingService {

    private final UserSettingMapper userSettingMapper;
    private final UserMapper userMapper;
    private final EmployeeMapper employeeMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserSettingVO getMine() {
        Long userId = SecurityUtil.getUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        UserSetting setting = requireOrCreate(userId);
        Employee employee = user.getEmployeeId() == null
                ? null
                : employeeMapper.selectById(user.getEmployeeId());

        String phone = employee == null ? null : employee.getPhone();
        String email = employee == null ? null : employee.getEmail();

        UserSettingVO vo = new UserSettingVO();
        vo.setNotifyAccount(on(setting.getNotifyAccount()));
        vo.setNotifySystem(on(setting.getNotifySystem()));
        vo.setNotifyTodo(on(setting.getNotifyTodo()));

        vo.setPasswordSet(true);
        vo.setPasswordStrength("已设置（建议定期修改）");

        vo.setHasPhone(StringUtils.hasText(phone));
        vo.setMaskedPhone(maskPhone(phone));
        vo.setPhoneSecured(on(setting.getPhoneSecured()) && StringUtils.hasText(phone));

        vo.setHasEmail(StringUtils.hasText(email));
        vo.setMaskedEmail(maskEmail(email));
        vo.setEmailSecured(on(setting.getEmailSecured()) && StringUtils.hasText(email));

        boolean hasQ = StringUtils.hasText(setting.getSecurityQuestion())
                && StringUtils.hasText(setting.getSecurityAnswerHash());
        vo.setHasSecurityQuestion(hasQ);
        vo.setSecurityQuestion(hasQ ? setting.getSecurityQuestion() : null);
        vo.setSecurityQuestionEnabled(hasQ);

        vo.setMfaEnabled(on(setting.getMfaEnabled()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNotification(NotificationSettingDTO dto) {
        UserSetting setting = requireOrCreate(SecurityUtil.getUserId());
        setting.setNotifyAccount(bool(dto.getNotifyAccount()));
        setting.setNotifySystem(bool(dto.getNotifySystem()));
        setting.setNotifyTodo(bool(dto.getNotifyTodo()));
        userSettingMapper.updateById(setting);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleSecurity(SecurityToggleDTO dto) {
        Long userId = SecurityUtil.getUserId();
        UserSetting setting = requireOrCreate(userId);
        User user = userMapper.selectById(userId);
        Employee employee = user.getEmployeeId() == null
                ? null
                : employeeMapper.selectById(user.getEmployeeId());
        boolean enabled = Boolean.TRUE.equals(dto.getEnabled());
        String field = dto.getField();

        switch (field) {
            case "phoneSecured" -> {
                if (enabled) {
                    if (employee == null || !StringUtils.hasText(employee.getPhone())) {
                        throw new BusinessException("请先在「基本设置」中填写手机号");
                    }
                    setting.setPhoneSecured(1);
                } else {
                    setting.setPhoneSecured(0);
                }
            }
            case "emailSecured" -> {
                if (enabled) {
                    if (employee == null || !StringUtils.hasText(employee.getEmail())) {
                        throw new BusinessException("请先在「基本设置」中填写邮箱");
                    }
                    setting.setEmailSecured(1);
                } else {
                    setting.setEmailSecured(0);
                }
            }
            case "securityQuestion" -> {
                if (enabled) {
                    if (!StringUtils.hasText(setting.getSecurityQuestion())
                            || !StringUtils.hasText(setting.getSecurityAnswerHash())) {
                        throw new BusinessException("请先设置密保问题与答案");
                    }
                } else {
                    setting.setSecurityQuestion(null);
                    setting.setSecurityAnswerHash(null);
                }
            }
            case "mfa" -> {
                if (enabled) {
                    throw new BusinessException("请先完成 MFA 设备绑定");
                }
                setting.setMfaEnabled(0);
                setting.setMfaSecret(null);
            }
            default -> throw new BusinessException("不支持的安全项：" + field);
        }
        userSettingMapper.updateById(setting);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setSecurityQuestion(SecurityQuestionDTO dto) {
        UserSetting setting = requireOrCreate(SecurityUtil.getUserId());
        setting.setSecurityQuestion(dto.getQuestion().trim());
        setting.setSecurityAnswerHash(passwordEncoder.encode(dto.getAnswer().trim()));
        userSettingMapper.updateById(setting);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MfaSetupVO setupMfa() {
        UserSetting setting = requireOrCreate(SecurityUtil.getUserId());
        if (on(setting.getMfaEnabled())) {
            throw new BusinessException("已启用 MFA，请先关闭后再重新绑定");
        }
        User user = userMapper.selectById(SecurityUtil.getUserId());
        String secret = TotpUtil.generateSecret();
        setting.setMfaSecret(secret);
        setting.setMfaEnabled(0);
        userSettingMapper.updateById(setting);

        MfaSetupVO vo = new MfaSetupVO();
        vo.setSecret(secret);
        vo.setOtpauthUrl(TotpUtil.otpauthUrl("智汇人事", user.getUsername(), secret));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmMfa(MfaConfirmDTO dto) {
        UserSetting setting = requireOrCreate(SecurityUtil.getUserId());
        if (!StringUtils.hasText(setting.getMfaSecret())) {
            throw new BusinessException("请先获取 MFA 绑定信息");
        }
        if (!TotpUtil.verify(setting.getMfaSecret(), dto.getCode().trim())) {
            throw new BusinessException("验证码错误或已过期");
        }
        setting.setMfaEnabled(1);
        userSettingMapper.updateById(setting);
    }

    @Override
    public boolean allowsNotification(Long userId, String bizType) {
        if (userId == null) {
            return false;
        }
        UserSetting setting = userSettingMapper.selectOne(
                new LambdaQueryWrapper<UserSetting>().eq(UserSetting::getUserId, userId).last("LIMIT 1"));
        if (setting == null) {
            return true;
        }
        if (isTodoBiz(bizType)) {
            return on(setting.getNotifyTodo());
        }
        if (isAccountBiz(bizType)) {
            return on(setting.getNotifyAccount());
        }
        return on(setting.getNotifySystem());
    }

    @Override
    public void assertMfaIfRequired(Long userId, String mfaCode) {
        UserSetting setting = userSettingMapper.selectOne(
                new LambdaQueryWrapper<UserSetting>().eq(UserSetting::getUserId, userId).last("LIMIT 1"));
        if (setting == null || !on(setting.getMfaEnabled())) {
            return;
        }
        if (!StringUtils.hasText(mfaCode)) {
            throw new BusinessException(4001, "该账号已开启 MFA，请填写验证码");
        }
        if (!TotpUtil.verify(setting.getMfaSecret(), mfaCode.trim())) {
            throw new BusinessException("MFA 验证码错误或已过期");
        }
    }

    private UserSetting requireOrCreate(Long userId) {
        UserSetting setting = userSettingMapper.selectOne(
                new LambdaQueryWrapper<UserSetting>().eq(UserSetting::getUserId, userId).last("LIMIT 1"));
        if (setting != null) {
            return setting;
        }
        setting = new UserSetting();
        setting.setUserId(userId);
        setting.setNotifyAccount(1);
        setting.setNotifySystem(1);
        setting.setNotifyTodo(1);
        setting.setPhoneSecured(0);
        setting.setEmailSecured(0);
        setting.setMfaEnabled(0);
        userSettingMapper.insert(setting);
        return setting;
    }

    private static boolean isTodoBiz(String bizType) {
        return NotificationBizType.TASK_ASSIGN.equals(bizType)
                || NotificationBizType.TASK_URGE.equals(bizType)
                || NotificationBizType.TASK_REJECT.equals(bizType)
                || NotificationBizType.TASK_OVERDUE.equals(bizType);
    }

    private static boolean isAccountBiz(String bizType) {
        return "ACCOUNT_PASSWORD".equals(bizType) || "ACCOUNT_SECURITY".equals(bizType);
    }

    private static boolean on(Integer v) {
        return v != null && v == 1;
    }

    private static int bool(Boolean v) {
        return Boolean.TRUE.equals(v) ? 1 : 0;
    }

    private static String maskPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private static String maskEmail(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            return email;
        }
        int at = email.indexOf('@');
        String name = email.substring(0, at);
        String domain = email.substring(at);
        if (name.length() <= 2) {
            return name.charAt(0) + "***" + domain;
        }
        return name.substring(0, 2) + "***" + domain;
    }
}
