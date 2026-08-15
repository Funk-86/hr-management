package org.example.hrmanagement.module.attendance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.hrmanagement.common.dto.PageQuery;
import org.example.hrmanagement.common.exception.BusinessException;
import org.example.hrmanagement.common.result.PageResult;
import org.example.hrmanagement.common.util.SecurityUtil;
import org.example.hrmanagement.config.AttendanceProperties;
import org.example.hrmanagement.module.attendance.dto.AttendanceCheckDTO;
import org.example.hrmanagement.module.attendance.dto.FaceCheckDTO;
import org.example.hrmanagement.module.attendance.entity.Attendance;
import org.example.hrmanagement.module.attendance.mapper.AttendanceMapper;
import org.example.hrmanagement.module.attendance.service.AttendanceService;
import org.example.hrmanagement.module.attendance.vo.AttendanceVO;
import org.example.hrmanagement.module.employee.entity.Employee;
import org.example.hrmanagement.module.employee.mapper.EmployeeMapper;
import org.example.hrmanagement.module.employee.service.FaceService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {
    private final AttendanceProperties attendanceProperties;
    private final EmployeeMapper employeeMapper;
    private final AttendanceMapper attendanceMapper;
    private final FaceService faceService;


    @Override
    public void checkIn(AttendanceCheckDTO dto) {
        Long employeeId = resolveCheckEmployeeId(dto != null ? dto.getEmployeeId() : null);
        Employee employee = employeeMapper.selectById(employeeId);
        if (employee == null) {
            throw new BusinessException("该员工不存在");
        }
        LocalDate today = LocalDate.now();
        Attendance existing = attendanceMapper.selectOne(
                new LambdaQueryWrapper<Attendance>()
                        .eq(Attendance::getEmployeeId, employeeId)
                        .eq(Attendance::getAttendDate, today)
        );
        if (existing != null && existing.getCheckIn() != null) {
            throw new BusinessException("该员工今日已打卡");
        }

        LocalTime now = LocalTime.now();
        int status = now.compareTo(attendanceProperties.getWorkStartTime()) <= 0 ? 1 : 2;

        if (existing != null) {
            existing.setCheckIn(now);
            existing.setStatus(status);
            attendanceMapper.updateById(existing);
            return;
        }

        Attendance attendance = new Attendance();
        attendance.setAttendDate(today);
        attendance.setEmployeeId(employeeId);
        attendance.setCheckIn(now);
        attendance.setStatus(status);
        attendanceMapper.insert(attendance);
    }

    @Override
    public void checkOut(AttendanceCheckDTO dto) {
        Long employeeId = resolveCheckEmployeeId(dto != null ? dto.getEmployeeId() : null);
        Employee employee = employeeMapper.selectById(employeeId);
        if (employee == null) {
            throw new BusinessException("该员工不存在");
        }
        LocalDate today = LocalDate.now();
        Attendance existing = attendanceMapper.selectOne(
                new LambdaQueryWrapper<Attendance>()
                        .eq(Attendance::getEmployeeId, employeeId)
                        .eq(Attendance::getAttendDate, today)
        );
        if (existing == null || existing.getCheckIn() == null) {
            throw new BusinessException("请先上班打卡");
        }
        if (existing.getCheckOut() != null) {
            throw new BusinessException("该员工今日已下班打卡");
        }

        LocalTime now = LocalTime.now();
        Duration duration = Duration.between(existing.getCheckIn(), now);
        BigDecimal workHours = BigDecimal.valueOf(duration.toMinutes())
                .divide(BigDecimal.valueOf(60), 1, RoundingMode.HALF_UP);

        existing.setCheckOut(now);
        existing.setWorkHours(workHours);
        if (now.compareTo(attendanceProperties.getWorkEndTime()) < 0) {
            existing.setStatus(3);
        }

        attendanceMapper.updateById(existing);
    }

    private static final int EXPORT_LIMIT = 5000;

    @Override
    public PageResult<AttendanceVO> getAll(PageQuery page) {
        LambdaQueryWrapper<Attendance> wrapper = new LambdaQueryWrapper<>();
        applyDataScope(wrapper);
        wrapper.orderByDesc(Attendance::getAttendDate);

        IPage<Attendance> iPage = attendanceMapper.selectPage(
                new Page<>(page.getPageNum(), page.getPageSize()), wrapper);

        List<Attendance> list = iPage.getRecords();
        if (list == null || list.isEmpty()) {
            return PageResult.empty();
        }

        List<AttendanceVO> vos = toVoList(list);

        PageResult<AttendanceVO> result = new PageResult<>();
        result.setRecords(vos);
        result.setTotal(iPage.getTotal());
        result.setPageNum(iPage.getCurrent());
        result.setPageSize(iPage.getSize());
        result.setPages(iPage.getPages());
        return result;
    }

    @Override
    public List<AttendanceVO> listForExport() {
        LambdaQueryWrapper<Attendance> wrapper = new LambdaQueryWrapper<>();
        applyDataScope(wrapper);
        wrapper.orderByDesc(Attendance::getAttendDate);
        wrapper.last("LIMIT " + (EXPORT_LIMIT + 1));
        List<Attendance> list = attendanceMapper.selectList(wrapper);
        if (list.size() > EXPORT_LIMIT) {
            throw new BusinessException("导出数据超过 " + EXPORT_LIMIT + " 条，请缩小范围后重试");
        }
        return toVoList(list);
    }

    private List<AttendanceVO> toVoList(List<Attendance> list) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        Set<Long> empIds = list.stream()
                .map(Attendance::getEmployeeId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        Map<Long, Employee> empMap = empIds.isEmpty()
                ? Map.of()
                : employeeMapper.selectBatchIds(empIds).stream()
                .collect(Collectors.toMap(Employee::getId, e -> e));

        return list.stream().map(a -> {
            Employee emp = empMap.get(a.getEmployeeId());
            return toVO(a, emp != null ? emp.getName() : null, emp != null ? emp.getEmpNo() : null);
        }).collect(Collectors.toList());
    }

    @Override
    public AttendanceVO getOne(Long id) {
        if(id==null){
            throw new BusinessException("id为空");
        }
        Attendance attendance=attendanceMapper.selectById(id);
        if(attendance==null){
            throw new BusinessException("该记录为空");
        }
        Employee employee=employeeMapper.selectById(attendance.getEmployeeId());
        if(employee==null){
            throw new BusinessException("该员工不存在");
        }
        return toVO(attendance,employee.getName(),employee.getEmpNo());
    }

    @Override
    public void create(AttendanceVO vo) {
        if (vo == null) {
            throw new BusinessException("内容不为空");
        }
        if (vo.getEmployeeId() == null) {
            throw new BusinessException("员工ID为空");
        }
        if (vo.getAttendDate() == null) {
            throw new BusinessException("考勤日期为空");
        }
        Employee employee = employeeMapper.selectById(vo.getEmployeeId());
        if (employee == null) {
            throw new BusinessException("该员工不存在");
        }

        Attendance existing = attendanceMapper.selectOne(
                new LambdaQueryWrapper<Attendance>()
                        .eq(Attendance::getEmployeeId, vo.getEmployeeId())
                        .eq(Attendance::getAttendDate, vo.getAttendDate())
        );
        if (existing != null) {
            throw new BusinessException("该员工该日期已有考勤记录");
        }

        attendanceMapper.insert(toEntity(vo));
    }

    @Override
    public void update(Long id,AttendanceVO vo) {
        if(id==null){
            throw new BusinessException("id为空");
        }
        if(vo==null){
            throw new BusinessException("内容不为空");
        }
        if (vo.getEmployeeId() == null) {
            throw new BusinessException("员工ID为空");
        }
        if (vo.getAttendDate() == null) {
            throw new BusinessException("考勤日期为空");
        }
        Employee employee = employeeMapper.selectById(vo.getEmployeeId());
        if (employee == null) {
            throw new BusinessException("该员工不存在");
        }
        Attendance existing = attendanceMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("考勤记录为空");
        }

        Attendance duplicate = attendanceMapper.selectOne(
                new LambdaQueryWrapper<Attendance>()
                        .eq(Attendance::getEmployeeId, vo.getEmployeeId())
                        .eq(Attendance::getAttendDate, vo.getAttendDate())
                        .ne(Attendance::getId, id)
        );
        if (duplicate != null) {
            throw new BusinessException("该员工该日期已有考勤记录");
        }
        existing.setEmployeeId(vo.getEmployeeId());
        existing.setAttendDate(vo.getAttendDate());
        existing.setCheckIn(vo.getCheckIn());
        existing.setCheckOut(vo.getCheckOut());
        existing.setStatus(vo.getStatus() != null ? vo.getStatus() : existing.getStatus());
        existing.setWorkHours(vo.getWorkHours());
        existing.setRemark(vo.getRemark());
        attendanceMapper.updateById(existing);
    }

    @Override
    public void delete(Long id) {
        if(id==null){
            throw new BusinessException("该ID为空");
        }
        Attendance attendance=attendanceMapper.selectById(id);
        if(attendance==null){
            throw new BusinessException("考勤记录不存在");
        }
        attendanceMapper.deleteById(id);
    }

    /** 仅超管可代他人打卡；其余角色只能为自己打卡 */
    @Override
    public Long resolveCheckEmployeeId(Long requestedEmployeeId) {
        if (SecurityUtil.hasRole("SUPER_ADMIN")) {
            if (requestedEmployeeId == null) {
                throw new BusinessException("请选择员工");
            }
            return requestedEmployeeId;
        }
        Long selfId = SecurityUtil.requireEmployeeId();
        if (requestedEmployeeId != null && !requestedEmployeeId.equals(selfId)) {
            throw new BusinessException("只能为自己打卡");
        }
        return selfId;
    }

    @Override
    public void checkInByFace(FaceCheckDTO dto) {
        Long employeeId = resolveCheckEmployeeId(dto.getEmployeeId());
        faceService.verifyEmployeeFace(employeeId, dto.getDescriptor());
        AttendanceCheckDTO checkDto = new AttendanceCheckDTO();
        checkDto.setEmployeeId(employeeId);
        checkIn(checkDto);
    }

    @Override
    public void checkOutByFace(FaceCheckDTO dto) {
        Long employeeId = resolveCheckEmployeeId(dto.getEmployeeId());
        faceService.verifyEmployeeFace(employeeId, dto.getDescriptor());

        AttendanceCheckDTO checkDto = new AttendanceCheckDTO();
        checkDto.setEmployeeId(employeeId);
        checkOut(checkDto);
    }

    /** 按角色过滤可见考勤记录 */
    private void applyDataScope(LambdaQueryWrapper<Attendance> wrapper) {
        if (SecurityUtil.hasRole("EMPLOYEE") && !SecurityUtil.isManagerUp()) {
            wrapper.eq(Attendance::getEmployeeId, SecurityUtil.requireEmployeeId());
            return;
        }
        if (SecurityUtil.hasRole("DEPT_MANAGER") && !SecurityUtil.isHrStaff()) {
            Long deptId = SecurityUtil.requireDeptId();
            List<Long> empIds = employeeMapper.selectList(
                    new LambdaQueryWrapper<Employee>().eq(Employee::getDeptId, deptId)
            ).stream().map(Employee::getId).toList();
            if (empIds.isEmpty()) {
                wrapper.eq(Attendance::getEmployeeId, -1L);
                return;
            }
            wrapper.in(Attendance::getEmployeeId, empIds);
        }
    }

    private Attendance toEntity(AttendanceVO vo){
        Attendance attendance=new Attendance();
        attendance.setAttendDate(vo.getAttendDate());
        attendance.setEmployeeId(vo.getEmployeeId());
        attendance.setCheckIn(vo.getCheckIn());
        attendance.setCheckOut(vo.getCheckOut());
        attendance.setStatus(vo.getStatus()!=null ? vo.getStatus():1);
        attendance.setWorkHours(vo.getWorkHours());
        attendance.setRemark(vo.getRemark());
        return attendance;
    }

    private AttendanceVO toVO(Attendance attendance,String employeeName,String empNo) {
        AttendanceVO vo=new AttendanceVO();
        vo.setId(attendance.getId());
        vo.setEmployeeId(attendance.getEmployeeId());
        vo.setEmployeeName(employeeName);
        vo.setEmpNo(empNo);
        vo.setAttendDate(attendance.getAttendDate());
        vo.setCheckIn(attendance.getCheckIn());
        vo.setCheckOut(attendance.getCheckOut());
        vo.setWorkHours(attendance.getWorkHours());
        vo.setStatus(attendance.getStatus());
        vo.setRemark(attendance.getRemark());
        return vo;
    }
}
