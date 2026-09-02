package org.example.hrmanagement.common.security;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.example.hrmanagement.common.exception.BusinessException;
import org.example.hrmanagement.module.employee.entity.Employee;
import org.example.hrmanagement.module.employee.mapper.EmployeeMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeDataScopeTest {

    @Mock
    private EmployeeMapper employeeMapper;

    @InjectMocks
    private EmployeeDataScope employeeDataScope;

    @BeforeAll
    static void initMybatisPlusLambdaCache() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                Employee.class);
    }

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void hrStaffCanViewAnyEmployee() {
        loginAs(1L, 10L, 100L, "HR_ADMIN");
        assertDoesNotThrow(() -> employeeDataScope.assertCanView(999L));
    }

    @Test
    void employeeCanViewSelfOnly() {
        loginAs(2L, 20L, 200L, "EMPLOYEE");
        assertDoesNotThrow(() -> employeeDataScope.assertCanView(20L));
        assertThrows(BusinessException.class, () -> employeeDataScope.assertCanView(21L));
    }

    @Test
    void managerCanViewSameDeptOnly() {
        loginAs(3L, 30L, 300L, "DEPT_MANAGER");
        Employee other = new Employee();
        other.setId(40L);
        other.setDeptId(300L);
        when(employeeMapper.selectById(40L)).thenReturn(other);

        Employee outsider = new Employee();
        outsider.setId(50L);
        outsider.setDeptId(999L);
        when(employeeMapper.selectById(50L)).thenReturn(outsider);

        assertDoesNotThrow(() -> employeeDataScope.assertCanView(40L));
        assertThrows(BusinessException.class, () -> employeeDataScope.assertCanView(50L));
    }

    @Test
    void employeeCannotManageOthers() {
        loginAs(2L, 20L, 200L, "EMPLOYEE");
        assertThrows(BusinessException.class, () -> employeeDataScope.assertCanManage(21L));
    }

    private void loginAs(Long userId, Long employeeId, Long deptId, String role) {
        LoginUser loginUser = new LoginUser(userId, "tester");
        loginUser.setRoles(java.util.List.of(role));
        loginUser.setEmployeeId(employeeId);
        loginUser.setDeptId(deptId);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
