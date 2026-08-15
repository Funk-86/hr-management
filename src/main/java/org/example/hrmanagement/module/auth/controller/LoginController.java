package org.example.hrmanagement.module.auth.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.hrmanagement.common.annotation.RateLimit;
import org.example.hrmanagement.common.result.Result;
import org.example.hrmanagement.module.auth.dto.LoginDTO;
import org.example.hrmanagement.module.auth.dto.ProfileUpdateDTO;
import org.example.hrmanagement.module.auth.dto.RegisLoginDTO;
import org.example.hrmanagement.module.auth.dto.VerifyPasswordDTO;
import org.example.hrmanagement.module.auth.service.AuthService;
import org.example.hrmanagement.module.auth.vo.LoginVO;
import org.example.hrmanagement.module.auth.vo.ProfileUpdateVO;
import org.example.hrmanagement.module.auth.vo.ProfileVO;
import org.example.hrmanagement.module.auth.vo.UserInfoVO;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class LoginController {
    private final AuthService authService;

    @RateLimit(value = 5, time = 60)
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto){
        return Result.success(authService.login(dto));
    }

    @GetMapping("/userinfo")
    public Result<UserInfoVO> userinfo(){
        return Result.success(authService.userinfo());
    }

    @GetMapping("/profile")
    public Result<ProfileVO> profile() {
        return Result.success(authService.profile());
    }

    @PutMapping("/profile")
    public Result<ProfileUpdateVO> updateProfile(@Valid @RequestBody ProfileUpdateDTO dto) {
        return Result.success(authService.updateProfile(dto));
    }

    @PostMapping("/regis")
    public Result<Void> regisLogin(@Valid @RequestBody RegisLoginDTO dto){
        authService.regisLogin(dto);
        return Result.success();
    }

    @RateLimit(value = 10, time = 60)
    @PostMapping("/verify-password")
    public Result<Void> verifyPassword(@Valid @RequestBody VerifyPasswordDTO dto) {
        authService.verifyPassword(dto.getPassword());
        return Result.success();
    }
}
