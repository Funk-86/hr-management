package org.example.hrmanagement.module.auth;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.example.hrmanagement.common.exception.BusinessException;
import org.example.hrmanagement.common.file.AvatarService;
import org.example.hrmanagement.common.util.JwtUtil;
import org.example.hrmanagement.module.auth.dto.LoginDTO;
import org.example.hrmanagement.module.auth.entity.User;
import org.example.hrmanagement.module.auth.mapper.PermissionMapper;
import org.example.hrmanagement.module.auth.mapper.UserMapper;
import org.example.hrmanagement.module.auth.service.UserSettingService;
import org.example.hrmanagement.module.auth.service.impl.AuthServiceImpl;
import org.example.hrmanagement.module.auth.vo.LoginVO;
import org.example.hrmanagement.module.employee.mapper.EmployeeMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private PermissionMapper permissionMapper;
    @Mock
    private EmployeeMapper employeeMapper;
    @Mock
    private AvatarService avatarService;
    @Mock
    private UserSettingService userSettingService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User mockUser;

    @BeforeAll
    static void initMybatisPlusLambdaCache() {
        // 纯 Mockito 环境需初始化实体表元数据，否则 LambdaUpdateWrapper 会失败
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                User.class);
    }

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("admin");
        mockUser.setPassword("$2a$10$encodedPassword");
        mockUser.setStatus(1);
    }

    private LoginDTO buildLoginDto(String username, String password, String roleCode) {
        LoginDTO dto = new LoginDTO();
        dto.setUsername(username);
        dto.setPassword(password);
        dto.setRoleCode(roleCode);
        return dto;
    }

    @Test
    void loginSuccess() {
        LoginDTO dto = buildLoginDto("admin", "123456", "SUPER_ADMIN");

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(mockUser);
        when(passwordEncoder.matches("123456", mockUser.getPassword())).thenReturn(true);
        when(permissionMapper.countUserRole(1L, "SUPER_ADMIN")).thenReturn(1);
        when(jwtUtil.generateToken(1L, "admin", "SUPER_ADMIN")).thenReturn("jwt-token-mock");
        when(permissionMapper.selectPermCodesByUserIdAndRole(1L, "SUPER_ADMIN")).thenReturn(java.util.List.of());
        when(permissionMapper.selectMenusByUserIdAndRole(1L, "SUPER_ADMIN")).thenReturn(java.util.List.of());
        when(avatarService.resolveAvatarUrl(any())).thenReturn("/avatar.png");
        doNothing().when(userSettingService).assertMfaIfRequired(1L, null);

        LoginVO result = authService.login(dto);

        assertNotNull(result);
        assertEquals("jwt-token-mock", result.getToken());
        assertEquals("admin", result.getUsername());
        assertEquals(List.of("SUPER_ADMIN"), result.getRoles());
        verify(userMapper, times(1)).update(any(), any());
    }

    @Test
    void loginFailWrongRole() {
        LoginDTO dto = buildLoginDto("admin", "123456", "HR_ADMIN");

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(mockUser);
        when(passwordEncoder.matches("123456", mockUser.getPassword())).thenReturn(true);
        when(permissionMapper.countUserRole(1L, "HR_ADMIN")).thenReturn(0);

        assertThrows(BusinessException.class, () -> authService.login(dto));
    }

    @Test
    void loginFailWrongPassword() {
        LoginDTO dto = buildLoginDto("admin", "wrong", "SUPER_ADMIN");

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(mockUser);
        when(passwordEncoder.matches("wrong", mockUser.getPassword())).thenReturn(false);

        assertThrows(BusinessException.class, () -> authService.login(dto));
    }

    @Test
    void loginFailUserDisabled() {
        mockUser.setStatus(0);
        LoginDTO dto = buildLoginDto("admin", "123456", "SUPER_ADMIN");

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(mockUser);
        when(passwordEncoder.matches("123456", mockUser.getPassword())).thenReturn(true);

        assertThrows(BusinessException.class, () -> authService.login(dto));
    }

    @Test
    void loginFailUserNotFound() {
        LoginDTO dto = buildLoginDto("nobody", "123456", "SUPER_ADMIN");

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        assertThrows(BusinessException.class, () -> authService.login(dto));
    }
}
