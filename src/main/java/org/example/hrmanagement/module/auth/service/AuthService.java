package org.example.hrmanagement.module.auth.service;

import org.example.hrmanagement.module.auth.dto.LoginDTO;
import org.example.hrmanagement.module.auth.dto.ProfileUpdateDTO;
import org.example.hrmanagement.module.auth.dto.RegisLoginDTO;
import org.example.hrmanagement.module.auth.vo.LoginVO;
import org.example.hrmanagement.module.auth.vo.ProfileUpdateVO;
import org.example.hrmanagement.module.auth.vo.ProfileVO;
import org.example.hrmanagement.module.auth.vo.UserInfoVO;

public interface AuthService {
    LoginVO login(LoginDTO dto);
    UserInfoVO userinfo();
    ProfileVO profile();
    ProfileUpdateVO updateProfile(ProfileUpdateDTO dto);
    void regisLogin(RegisLoginDTO dto);

    /** 校验当前登录用户密码（锁屏解锁等） */
    void verifyPassword(String password);
}
