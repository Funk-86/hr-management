package org.example.hrmanagement.module.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.hrmanagement.common.annotation.OperationLog;
import org.example.hrmanagement.common.result.Result;
import org.example.hrmanagement.module.auth.dto.MfaConfirmDTO;
import org.example.hrmanagement.module.auth.dto.NotificationSettingDTO;
import org.example.hrmanagement.module.auth.dto.SecurityQuestionDTO;
import org.example.hrmanagement.module.auth.dto.SecurityToggleDTO;
import org.example.hrmanagement.module.auth.service.UserSettingService;
import org.example.hrmanagement.module.auth.vo.MfaSetupVO;
import org.example.hrmanagement.module.auth.vo.UserSettingVO;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户设置")
@RestController
@RequestMapping("/auth/settings")
@RequiredArgsConstructor
public class UserSettingController {

    private final UserSettingService userSettingService;

    @Operation(summary = "获取安全与消息偏好")
    @GetMapping
    public Result<UserSettingVO> getMine() {
        return Result.success(userSettingService.getMine());
    }

    @Operation(summary = "更新消息提醒偏好")
    @OperationLog(module = "个人中心", value = "更新消息提醒")
    @PutMapping("/notification")
    public Result<Void> updateNotification(@Valid @RequestBody NotificationSettingDTO dto) {
        userSettingService.updateNotification(dto);
        return Result.success();
    }

    @Operation(summary = "切换安全项开关")
    @OperationLog(module = "个人中心", value = "更新安全设置")
    @PutMapping("/security/toggle")
    public Result<Void> toggleSecurity(@Valid @RequestBody SecurityToggleDTO dto) {
        userSettingService.toggleSecurity(dto);
        return Result.success();
    }

    @Operation(summary = "设置密保问题")
    @OperationLog(module = "个人中心", value = "设置密保问题")
    @PutMapping("/security/question")
    public Result<Void> setQuestion(@Valid @RequestBody SecurityQuestionDTO dto) {
        userSettingService.setSecurityQuestion(dto);
        return Result.success();
    }

    @Operation(summary = "开始绑定 MFA")
    @PostMapping("/security/mfa/setup")
    public Result<MfaSetupVO> setupMfa() {
        return Result.success(userSettingService.setupMfa());
    }

    @Operation(summary = "确认绑定 MFA")
    @OperationLog(module = "个人中心", value = "启用MFA")
    @PostMapping("/security/mfa/confirm")
    public Result<Void> confirmMfa(@Valid @RequestBody MfaConfirmDTO dto) {
        userSettingService.confirmMfa(dto);
        return Result.success();
    }
}
