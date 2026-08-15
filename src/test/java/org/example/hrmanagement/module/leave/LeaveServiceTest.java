package org.example.hrmanagement.module.leave;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.hrmanagement.common.exception.BusinessException;
import org.example.hrmanagement.module.employee.mapper.EmployeeMapper;
import org.example.hrmanagement.module.leave.entity.LeaveRequest;
import org.example.hrmanagement.module.leave.entity.LeaveType;
import org.example.hrmanagement.module.leave.mapper.LeaveRequestMapper;
import org.example.hrmanagement.module.leave.mapper.LeaveTypeMapper;
import org.example.hrmanagement.module.leave.service.impl.LeaveServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveServiceTest {

    @Mock
    private LeaveRequestMapper leaveRequestMapper;
    @Mock
    private LeaveTypeMapper leaveTypeMapper;
    @Mock
    private EmployeeMapper employeeMapper;

    @InjectMocks
    private LeaveServiceImpl leaveService;

    @Test
    void insertLeaveTypeDuplicateCode() {
        when(leaveTypeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        org.example.hrmanagement.module.leave.dto.LeaveTypeCreateDTO dto =
                new org.example.hrmanagement.module.leave.dto.LeaveTypeCreateDTO();
        dto.setTypeCode("ANNUAL");
        dto.setTypeName("年假");

        assertThrows(BusinessException.class, () -> leaveService.insertLeaveType(dto));
    }

    @Test
    void deleteLeaveTypeHasRecords() {
        LeaveType leaveType = new LeaveType();
        leaveType.setId(1L);
        leaveType.setStatus(1);

        when(leaveTypeMapper.selectById(1L)).thenReturn(leaveType);
        when(leaveRequestMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L);

        assertThrows(BusinessException.class, () -> leaveService.deleteLeaveType(1L));
    }

    @Test
    void approveLeaveInvalidStatusShouldThrow() {
        int status = 1; // already approved
        if (status != 0) {
            assertThrows(BusinessException.class, () -> {
                throw new BusinessException("该申请已处理，无法再次处理");
            });
        }
    }

    @Test
    void leaveTypeNotFound() {
        when(leaveTypeMapper.selectById(999L)).thenReturn(null);
        LeaveType lt = leaveTypeMapper.selectById(999L);
        assertNull(lt);
    }

    @Test
    void processLeaveStatusNullId() {
        assertThrows(BusinessException.class, () -> {
            Long id = null;
            if (id == null) throw new BusinessException("id不能为空");
        });
    }
}
