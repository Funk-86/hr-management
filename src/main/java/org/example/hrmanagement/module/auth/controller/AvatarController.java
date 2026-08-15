package org.example.hrmanagement.module.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.hrmanagement.common.file.AvatarService;
import org.example.hrmanagement.common.result.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "头像")
@RestController
@RequiredArgsConstructor
public class AvatarController {

    private final AvatarService avatarService;

    @Operation(summary = "上传当前用户头像")
    @PostMapping("/auth/avatar")
    public Result<String> uploadMyAvatar(@RequestParam("file") MultipartFile file) {
        return Result.success(avatarService.uploadMyAvatar(file));
    }

    @Operation(summary = "获取当前用户头像 URL")
    @GetMapping("/auth/avatar")
    public Result<String> getMyAvatar() {
        return Result.success(avatarService.getMyAvatarUrl());
    }

    @Operation(summary = "为指定员工上传头像")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','HR_ADMIN','DEPT_MANAGER','EMPLOYEE')")
    @PostMapping("/employees/{employeeId}/avatar")
    public Result<String> uploadEmployeeAvatar(
            @PathVariable Long employeeId,
            @RequestParam("file") MultipartFile file) {
        return Result.success(avatarService.uploadEmployeeAvatar(employeeId, file));
    }
}
