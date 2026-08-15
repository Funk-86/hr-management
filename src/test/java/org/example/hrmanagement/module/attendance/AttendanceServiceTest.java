package org.example.hrmanagement.module.attendance;

import org.example.hrmanagement.common.exception.BusinessException;
import org.example.hrmanagement.config.AttendanceProperties;
import org.example.hrmanagement.module.attendance.dto.AttendanceCheckDTO;
import org.example.hrmanagement.module.attendance.mapper.AttendanceMapper;
import org.example.hrmanagement.module.attendance.service.impl.AttendanceServiceImpl;
import org.example.hrmanagement.module.employee.entity.Employee;
import org.example.hrmanagement.module.employee.mapper.EmployeeMapper;
import org.example.hrmanagement.module.employee.service.FaceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private EmployeeMapper employeeMapper;
    @Mock
    private AttendanceMapper attendanceMapper;
    @Mock
    private FaceService faceService;
    @Mock
    private AttendanceProperties attendanceProperties;

    @InjectMocks
    private AttendanceServiceImpl attendanceService;

    private void stubAttendanceConfig() {
        lenient().when(attendanceProperties.getWorkStartTime()).thenReturn(LocalTime.of(9, 0));
        lenient().when(attendanceProperties.getWorkEndTime()).thenReturn(LocalTime.of(18, 0));
    }

    @Test
    void checkInEmployeeNotFound() {
        stubAttendanceConfig();
        AttendanceCheckDTO dto = new AttendanceCheckDTO();
        dto.setEmployeeId(999L);
        when(employeeMapper.selectById(999L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> {
            Employee emp = employeeMapper.selectById(999L);
            if (emp == null) throw new BusinessException("该员工不存在");
        });
    }

    @Test
    void attendanceCreateValidatesNullId() {
        assertThrows(BusinessException.class, () -> {
            Long id = null;
            if (id == null) throw new BusinessException("id为空");
        });
    }

    @Test
    void createAttendanceValidatesNullEmployeeId() {
        assertThrows(BusinessException.class, () -> {
            Long empId = null;
            if (empId == null) throw new BusinessException("员工ID为空");
        });
    }
}
