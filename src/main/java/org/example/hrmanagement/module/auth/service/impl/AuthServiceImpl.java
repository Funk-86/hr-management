package org.example.hrmanagement.module.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.example.hrmanagement.common.exception.BusinessException;
import org.example.hrmanagement.common.file.AvatarService;
import org.example.hrmanagement.common.util.JwtUtil;
import org.example.hrmanagement.common.util.SecurityUtil;
import org.example.hrmanagement.module.auth.dto.LoginDTO;
import org.example.hrmanagement.module.auth.dto.ProfileUpdateDTO;
import org.example.hrmanagement.module.auth.dto.RegisLoginDTO;
import org.example.hrmanagement.module.auth.entity.Permission;
import org.example.hrmanagement.module.auth.entity.User;
import org.example.hrmanagement.module.auth.mapper.PermissionMapper;
import org.example.hrmanagement.module.auth.mapper.UserMapper;
import org.example.hrmanagement.module.auth.service.AuthService;
import org.example.hrmanagement.module.auth.service.UserSettingService;
import org.example.hrmanagement.module.auth.vo.LoginVO;
import org.example.hrmanagement.module.notification.service.NotificationService;
import org.example.hrmanagement.module.auth.vo.MenuVO;
import org.example.hrmanagement.module.auth.vo.ProfileUpdateVO;
import org.example.hrmanagement.module.auth.vo.ProfileVO;
import org.example.hrmanagement.module.auth.vo.UserInfoVO;
import org.example.hrmanagement.module.employee.entity.Employee;
import org.example.hrmanagement.module.employee.mapper.EmployeeMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final PermissionMapper permissionMapper;
    private final EmployeeMapper employeeMapper;
    private final AvatarService avatarService;
    private final UserSettingService userSettingService;
    private final NotificationService notificationService;

    @Override
    public LoginVO login(LoginDTO dto) {
        User user=userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername,dto.getUsername()));
        if(user==null||!passwordEncoder.matches(dto.getPassword(),user.getPassword())){
            throw new BusinessException("用户名或密码错误");
        }
        if(user.getStatus()==0){
            throw new BusinessException("该账户已被禁用");
        }
        String roleCode = dto.getRoleCode().trim();
        if (permissionMapper.countUserRole(user.getId(), roleCode) <= 0) {
            throw new BusinessException("该账号不具备所选角色，无法登录");
        }
        userSettingService.assertMfaIfRequired(user.getId(), dto.getMfaCode());
        userMapper.update(null,new LambdaUpdateWrapper<User>()
                .eq(User::getId,user.getId())
                .set(User::getLastLogin, LocalDateTime.now()));

        return toVO(user, roleCode);
    }

    @Override
    public UserInfoVO userinfo() {
        Long userId=SecurityUtil.getUserId();
        User user=userMapper.selectById(userId);
        if (user==null){
            throw new BusinessException("没有该用户");
        }
        // 以本次登录选择的角色为准（JWT / SecurityContext）
        List<String> roles = SecurityUtil.getRoles();
        List<String> permissions = SecurityUtil.getPermissions();

        UserInfoVO userInfoVO=new UserInfoVO();
        userInfoVO.setRoles(roles);
        userInfoVO.setPermissions(permissions);
        userInfoVO.setUsername(user.getUsername());
        userInfoVO.setRealName(resolveRealName(user));
        userInfoVO.setAvatar(avatarService.resolveAvatarUrl(user));
        return userInfoVO;
    }

    @Override
    public ProfileVO profile() {
        Long userId = SecurityUtil.getUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("没有该用户");
        }
        ProfileVO vo = new ProfileVO();
        vo.setUsername(user.getUsername());
        vo.setRealName(resolveRealName(user));
        vo.setAvatar(avatarService.resolveAvatarUrl(user));
        vo.setRoles(SecurityUtil.getRoles());
        vo.setBoundEmployee(false);

        if (user.getEmployeeId() != null) {
            Employee employee = employeeMapper.selectById(user.getEmployeeId());
            if (employee != null) {
                vo.setBoundEmployee(true);
                vo.setRealName(employee.getName());
                vo.setPhone(employee.getPhone());
                vo.setEmail(employee.getEmail());
                vo.setGender(employee.getGender());
                vo.setIntroduction(employee.getRemark());
            }
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProfileUpdateVO updateProfile(ProfileUpdateDTO dto) {
        Long userId = SecurityUtil.getUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("该用户不存在");
        }

        boolean usernameChanged = false;
        if (dto.getUsername() != null) {
            String username = dto.getUsername().trim();
            if (!StringUtils.hasText(username)) {
                throw new BusinessException("用户名不能为空");
            }
            if (!username.equals(user.getUsername())) {
                Long exists = userMapper.selectCount(
                        new LambdaQueryWrapper<User>()
                                .eq(User::getUsername, username)
                                .ne(User::getId, userId));
                if (exists != null && exists > 0) {
                    throw new BusinessException("该用户名已被占用");
                }
                user.setUsername(username);
                userMapper.updateById(user);
                usernameChanged = true;
            }
        }

        boolean hasEmployeeFields = dto.getRealName() != null
                || dto.getPhone() != null
                || dto.getEmail() != null
                || dto.getGender() != null
                || dto.getIntroduction() != null;
        if (hasEmployeeFields) {
            if (user.getEmployeeId() == null) {
                throw new BusinessException("当前账号未关联员工档案，无法修改档案信息");
            }
            Employee employee = employeeMapper.selectById(user.getEmployeeId());
            if (employee == null) {
                throw new BusinessException("关联员工档案不存在");
            }
            if (dto.getRealName() != null) {
                if (!StringUtils.hasText(dto.getRealName())) {
                    throw new BusinessException("姓名不能为空");
                }
                employee.setName(dto.getRealName().trim());
            }
            if (dto.getPhone() != null) {
                employee.setPhone(StringUtils.hasText(dto.getPhone()) ? dto.getPhone().trim() : null);
            }
            if (dto.getEmail() != null) {
                employee.setEmail(StringUtils.hasText(dto.getEmail()) ? dto.getEmail().trim() : null);
            }
            if (dto.getGender() != null) {
                if (dto.getGender() != 1 && dto.getGender() != 2) {
                    throw new BusinessException("性别取值无效");
                }
                employee.setGender(dto.getGender());
            }
            if (dto.getIntroduction() != null) {
                employee.setRemark(StringUtils.hasText(dto.getIntroduction())
                        ? dto.getIntroduction().trim() : null);
            }
            employeeMapper.updateById(employee);
        }

        ProfileUpdateVO result = new ProfileUpdateVO();
        result.setUsername(user.getUsername());
        if (usernameChanged) {
            List<String> roles = SecurityUtil.getRoles();
            String roleCode = roles.isEmpty() ? null : roles.get(0);
            if (!StringUtils.hasText(roleCode)) {
                throw new BusinessException("登录角色失效，请重新登录");
            }
            result.setToken(jwtUtil.generateToken(userId, user.getUsername(), roleCode));
        }
        return result;
    }

    @Override
    public void regisLogin(RegisLoginDTO dto) {
        if (!Objects.equals(dto.getNewPassword(), dto.getConfirmPassword())) {
            throw new BusinessException("两次密码不一致");
        }
        Long userId = SecurityUtil.getUserId();
        User user = userMapper.selectById(userId);
        if (user==null){
            throw new BusinessException("该用户不存在");
        }
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getPassword, passwordEncoder.encode(dto.getNewPassword())));
        notificationService.sendToUsers(
                List.of(userId),
                "密码已修改",
                "您的登录密码已成功修改，如非本人操作请立即联系管理员。",
                "ACCOUNT_PASSWORD",
                userId,
                "/profile");
    }

    @Override
    public void verifyPassword(String password) {
        if (!StringUtils.hasText(password)) {
            throw new BusinessException("密码不能为空");
        }
        Long userId = SecurityUtil.getUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("该用户不存在");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException("密码错误");
        }
    }

    private LoginVO toVO(User user, String roleCode){
        Long userId = user.getId();

        // 仅返回本次登录角色及其权限、菜单
        List<String> roles = List.of(roleCode);
        List<String> permissions = permissionMapper.selectPermCodesByUserIdAndRole(userId, roleCode);
        List<Permission> menuPermissions = permissionMapper.selectMenusByUserIdAndRole(userId, roleCode);
        List<MenuVO> menus = buildMenuTree(menuPermissions);

        LoginVO vo = new LoginVO();
        vo.setToken(jwtUtil.generateToken(userId, user.getUsername(), roleCode));
        vo.setUsername(user.getUsername());
        vo.setRealName(resolveRealName(user));
        vo.setAvatar(avatarService.resolveAvatarUrl(user));
        vo.setRoles(roles);

        vo.setPermissions(permissions);
        vo.setMenus(menus);
        return vo;
    }

    private List<MenuVO> buildMenuTree(List<Permission> permissions){
        List<MenuVO> menus=permissions.stream().map(p->{
            MenuVO menu=new MenuVO();
            menu.setId(p.getId());
            menu.setName(p.getPermName());
            menu.setCode(p.getPermCode());
            menu.setPath(p.getPath());
            menu.setSortOrder(p.getSortOrder());
            menu.setChildren(new ArrayList<>());
            return menu;
        }).toList();

        Map<Long,MenuVO> menuMap=menus.stream()
                .collect(Collectors.toMap(MenuVO::getId,m->m));

        List<MenuVO> tree=new ArrayList<>();
        for(Permission permission:permissions){
            MenuVO current=menuMap.get(permission.getId());
            if(permission.getId()==null||permission.getParentId()==0){
                tree.add(current);
            }else{
                MenuVO parent=menuMap.get(permission.getParentId());
                if(parent!=null){
                    parent.getChildren().add(current);
                }
            }
        }
        return tree;
    }

    /** 绑定员工时返回 hr_employee.name，纯超管等无绑定时返回 username */
    private String resolveRealName(User user) {
        if (user.getEmployeeId() != null) {
            Employee employee = employeeMapper.selectById(user.getEmployeeId());
            if (employee != null && StringUtils.hasText(employee.getName())) {
                return employee.getName();
            }
        }
        return user.getUsername();
    }
}
