package org.example.hrmanagement.common.file;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.example.hrmanagement.common.exception.BusinessException;
import org.example.hrmanagement.common.oss.OssProperties;
import org.example.hrmanagement.common.oss.OssService;
import org.example.hrmanagement.common.util.SecurityUtil;
import org.example.hrmanagement.module.auth.entity.User;
import org.example.hrmanagement.module.auth.mapper.UserMapper;
import org.example.hrmanagement.module.employee.entity.Employee;
import org.example.hrmanagement.module.employee.mapper.EmployeeMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AvatarService {

    private final OssService ossService;
    private final OssProperties ossProperties;
    private final UserMapper userMapper;
    private final EmployeeMapper employeeMapper;

    /** 上传当前登录用户头像 */
    @Transactional(rollbackFor = Exception.class)
    public String uploadMyAvatar(MultipartFile file) {
        User user = userMapper.selectById(SecurityUtil.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        validateFile(file);

        if (user.getEmployeeId() != null) {
            Employee employee = employeeMapper.selectById(user.getEmployeeId());
            if (employee == null) {
                throw new BusinessException("关联员工不存在");
            }
            String oldKey = employee.getAvatar();
            String newKey = uploadToOss(file, ossProperties.getAvatar().getEmployeePrefix(), employee.getId());
            employeeMapper.update(null, new LambdaUpdateWrapper<Employee>()
                    .eq(Employee::getId, employee.getId())
                    .set(Employee::getAvatar, newKey));
            ossService.deleteIfExists(oldKey);
            return ossService.toPublicUrl(newKey);
        }

        String oldKey = user.getAvatar();
        String newKey = uploadToOss(file, ossProperties.getAvatar().getUserPrefix(), user.getId());
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, user.getId())
                .set(User::getAvatar, newKey));
        ossService.deleteIfExists(oldKey);
        return ossService.toPublicUrl(newKey);
    }

    /** HR/经理为指定员工上传头像 */
    @Transactional(rollbackFor = Exception.class)
    public String uploadEmployeeAvatar(Long employeeId, MultipartFile file) {
        if (employeeId == null) {
            throw new BusinessException("员工 ID 不能为空");
        }
        Employee employee = employeeMapper.selectById(employeeId);
        if (employee == null) {
            throw new BusinessException("员工不存在");
        }
        assertCanManageEmployee(employee);
        validateFile(file);

        String oldKey = employee.getAvatar();
        String newKey = uploadToOss(file, ossProperties.getAvatar().getEmployeePrefix(), employeeId);
        employeeMapper.update(null, new LambdaUpdateWrapper<Employee>()
                .eq(Employee::getId, employeeId)
                .set(Employee::getAvatar, newKey));
        ossService.deleteIfExists(oldKey);
        return ossService.toPublicUrl(newKey);
    }

    /** 获取当前登录用户头像 URL */
    public String getMyAvatarUrl() {
        User user = userMapper.selectById(SecurityUtil.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return resolveAvatarUrl(user);
    }

    /** 根据用户解析头像 URL（登录、userinfo、列表展示共用） */
    public String resolveAvatarUrl(User user) {
        if (user == null) {
            return ossService.toPublicUrl(ossService.getDefaultKey());
        }
        if (user.getEmployeeId() != null) {
            Employee employee = employeeMapper.selectById(user.getEmployeeId());
            if (employee != null && StringUtils.hasText(employee.getAvatar())) {
                return ossService.toPublicUrl(employee.getAvatar());
            }
        }
        if (StringUtils.hasText(user.getAvatar())) {
            return ossService.toPublicUrl(user.getAvatar());
        }
        return ossService.toPublicUrl(ossService.getDefaultKey());
    }

    /** 员工列表/详情：Key → URL */
    public String resolveEmployeeAvatarUrl(Employee employee) {
        if (employee != null && StringUtils.hasText(employee.getAvatar())) {
            return ossService.toPublicUrl(employee.getAvatar());
        }
        return ossService.toPublicUrl(ossService.getDefaultKey());
    }

    private String uploadToOss(MultipartFile file, String prefix, Long ownerId) {
        String extension = resolveExtension(file);
        String objectKey = ossService.buildObjectKey(prefix, ownerId, extension);
        try {
            return ossService.upload(
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType(),
                    objectKey
            );
        } catch (IOException e) {
            throw new BusinessException("读取上传文件失败");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的头像文件");
        }
        OssProperties.Avatar avatarConfig = ossProperties.getAvatar();
        if (file.getSize() > avatarConfig.getMaxSize()) {
            throw new BusinessException("头像大小不能超过 2MB");
        }
        String contentType = file.getContentType();
        Set<String> allowed = Arrays.stream(avatarConfig.getAllowedTypes().split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        if (!StringUtils.hasText(contentType) || !allowed.contains(contentType)) {
            throw new BusinessException("仅支持 JPG、PNG、WEBP 格式头像");
        }
        resolveExtension(file);
    }

    private String resolveExtension(MultipartFile file) {
        String contentType = file.getContentType();
        if ("image/jpeg".equals(contentType)) {
            return ".jpg";
        }
        if ("image/png".equals(contentType)) {
            return ".png";
        }
        if ("image/webp".equals(contentType)) {
            return ".webp";
        }
        String original = file.getOriginalFilename();
        if (StringUtils.hasText(original) && original.contains(".")) {
            String ext = original.substring(original.lastIndexOf('.')).toLowerCase(Locale.ROOT);
            if (".jpg".equals(ext) || ".jpeg".equals(ext) || ".png".equals(ext) || ".webp".equals(ext)) {
                return ".jpg".equals(ext) || ".jpeg".equals(ext) ? ".jpg" : ext;
            }
        }
        throw new BusinessException("无法识别头像文件格式");
    }

    private void assertCanManageEmployee(Employee employee) {
        if (SecurityUtil.isHrStaff()) {
            return;
        }
        if (SecurityUtil.hasRole("DEPT_MANAGER")) {
            Long deptId = SecurityUtil.requireDeptId();
            if (employee.getDeptId() != null && employee.getDeptId().equals(deptId)) {
                return;
            }
        }
        Long myEmployeeId = SecurityUtil.getEmployeeId();
        if (myEmployeeId != null && myEmployeeId.equals(employee.getId())) {
            return;
        }
        throw new BusinessException("无权修改该员工头像");
    }
}
