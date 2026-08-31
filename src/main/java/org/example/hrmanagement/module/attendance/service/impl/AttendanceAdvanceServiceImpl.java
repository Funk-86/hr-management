package org.example.hrmanagement.module.attendance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.hrmanagement.common.dto.PageQuery;
import org.example.hrmanagement.common.exception.BusinessException;
import org.example.hrmanagement.common.result.PageResult;
import org.example.hrmanagement.common.security.LoginUser;
import org.example.hrmanagement.common.util.SecurityUtil;
import org.example.hrmanagement.module.attendance.dto.AttendanceAppealCreateDTO;
import org.example.hrmanagement.module.attendance.dto.FieldWorkCreateDTO;
import org.example.hrmanagement.module.attendance.dto.OvertimeCreateDTO;
import org.example.hrmanagement.module.attendance.entity.Attendance;
import org.example.hrmanagement.module.attendance.entity.AttendanceAppeal;
import org.example.hrmanagement.module.attendance.entity.FieldWorkRequest;
import org.example.hrmanagement.module.attendance.entity.OvertimeRequest;
import org.example.hrmanagement.module.attendance.mapper.AttendanceAppealMapper;
import org.example.hrmanagement.module.attendance.mapper.AttendanceMapper;
import org.example.hrmanagement.module.attendance.mapper.FieldWorkRequestMapper;
import org.example.hrmanagement.module.attendance.mapper.OvertimeRequestMapper;
import org.example.hrmanagement.module.attendance.service.AttendanceAdvanceService;
import org.example.hrmanagement.module.attendance.vo.AttendanceAppealVO;
import org.example.hrmanagement.module.attendance.vo.FieldWorkRequestVO;
import org.example.hrmanagement.module.attendance.vo.OvertimeRequestVO;
import org.example.hrmanagement.module.employee.entity.Employee;
import org.example.hrmanagement.module.employee.mapper.EmployeeMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceAdvanceServiceImpl implements AttendanceAdvanceService {

    private static final int STATUS_FIELD = 6;

    private final OvertimeRequestMapper overtimeRequestMapper;
    private final AttendanceAppealMapper appealMapper;
    private final FieldWorkRequestMapper fieldWorkRequestMapper;
    private final AttendanceMapper attendanceMapper;
    private final EmployeeMapper employeeMapper;

    @Override
    public void createOvertime(OvertimeCreateDTO dto) {
        Long employeeId = SecurityUtil.requireEmployeeId();
        if (!dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new BusinessException("结束时间必须晚于开始时间");
        }
        if (dto.getHours() == null || dto.getHours().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("加班时长不合法");
        }
        OvertimeRequest req = new OvertimeRequest();
        req.setEmployeeId(employeeId);
        req.setWorkDate(dto.getWorkDate());
        req.setStartTime(dto.getStartTime());
        req.setEndTime(dto.getEndTime());
        req.setHours(dto.getHours());
        req.setReason(dto.getReason());
        req.setStatus(0);
        overtimeRequestMapper.insert(req);
    }

    @Override
    public PageResult<OvertimeRequestVO> listOvertime(PageQuery page) {
        LambdaQueryWrapper<OvertimeRequest> wrapper = new LambdaQueryWrapper<>();
        applyScope(wrapper, OvertimeRequest::getEmployeeId);
        wrapper.orderByDesc(OvertimeRequest::getCreatedAt);
        IPage<OvertimeRequest> iPage = overtimeRequestMapper.selectPage(
                new Page<>(page.getPageNum(), page.getPageSize()), wrapper);
        return toPage(iPage, list -> {
            Map<Long, String> names = loadEmpNames(list.stream().map(OvertimeRequest::getEmployeeId).collect(Collectors.toSet()));
            return list.stream().map(r -> {
                OvertimeRequestVO vo = new OvertimeRequestVO();
                vo.setId(r.getId());
                vo.setEmployeeId(r.getEmployeeId());
                vo.setEmployeeName(names.get(r.getEmployeeId()));
                vo.setWorkDate(r.getWorkDate());
                vo.setStartTime(r.getStartTime());
                vo.setEndTime(r.getEndTime());
                vo.setHours(r.getHours());
                vo.setReason(r.getReason());
                vo.setStatus(r.getStatus());
                vo.setApproveTime(r.getApproveTime());
                vo.setApproveRemark(r.getApproveRemark());
                return vo;
            }).toList();
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveOvertime(Long id, String remark) {
        OvertimeRequest req = loadPending(overtimeRequestMapper.selectById(id));
        fillApprove(req::setApproverId, req::setApproveTime, req::setApproveRemark, req::setStatus, 1, remark);
        overtimeRequestMapper.updateById(req);
    }

    @Override
    public void rejectOvertime(Long id, String remark) {
        OvertimeRequest req = loadPending(overtimeRequestMapper.selectById(id));
        fillApprove(req::setApproverId, req::setApproveTime, req::setApproveRemark, req::setStatus, 2, remark);
        overtimeRequestMapper.updateById(req);
    }

    @Override
    public void cancelOvertime(Long id) {
        OvertimeRequest req = loadPending(overtimeRequestMapper.selectById(id));
        assertOwner(req.getEmployeeId());
        req.setStatus(3);
        req.setApproveTime(LocalDateTime.now());
        overtimeRequestMapper.updateById(req);
    }

    @Override
    public void createAppeal(AttendanceAppealCreateDTO dto) {
        Long employeeId = SecurityUtil.requireEmployeeId();
        AttendanceAppeal appeal = new AttendanceAppeal();
        appeal.setEmployeeId(employeeId);
        appeal.setAttendDate(dto.getAttendDate());
        appeal.setAttendanceId(dto.getAttendanceId());
        appeal.setFromStatus(dto.getFromStatus());
        appeal.setToStatus(dto.getToStatus());
        appeal.setCheckIn(dto.getCheckIn());
        appeal.setCheckOut(dto.getCheckOut());
        appeal.setReason(dto.getReason());
        appeal.setStatus(0);
        appealMapper.insert(appeal);
    }

    @Override
    public PageResult<AttendanceAppealVO> listAppeals(PageQuery page) {
        LambdaQueryWrapper<AttendanceAppeal> wrapper = new LambdaQueryWrapper<>();
        applyScope(wrapper, AttendanceAppeal::getEmployeeId);
        wrapper.orderByDesc(AttendanceAppeal::getCreatedAt);
        IPage<AttendanceAppeal> iPage = appealMapper.selectPage(
                new Page<>(page.getPageNum(), page.getPageSize()), wrapper);
        return toPage(iPage, list -> {
            Map<Long, String> names = loadEmpNames(list.stream().map(AttendanceAppeal::getEmployeeId).collect(Collectors.toSet()));
            return list.stream().map(r -> {
                AttendanceAppealVO vo = new AttendanceAppealVO();
                vo.setId(r.getId());
                vo.setEmployeeId(r.getEmployeeId());
                vo.setEmployeeName(names.get(r.getEmployeeId()));
                vo.setAttendDate(r.getAttendDate());
                vo.setAttendanceId(r.getAttendanceId());
                vo.setFromStatus(r.getFromStatus());
                vo.setToStatus(r.getToStatus());
                vo.setCheckIn(r.getCheckIn());
                vo.setCheckOut(r.getCheckOut());
                vo.setReason(r.getReason());
                vo.setStatus(r.getStatus());
                vo.setApproveTime(r.getApproveTime());
                vo.setApproveRemark(r.getApproveRemark());
                return vo;
            }).toList();
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveAppeal(Long id, String remark) {
        AttendanceAppeal appeal = loadPending(appealMapper.selectById(id));
        fillApprove(appeal::setApproverId, appeal::setApproveTime, appeal::setApproveRemark, appeal::setStatus, 1, remark);
        appealMapper.updateById(appeal);

        Attendance existing = null;
        if (appeal.getAttendanceId() != null) {
            existing = attendanceMapper.selectById(appeal.getAttendanceId());
        }
        if (existing == null) {
            existing = attendanceMapper.selectOne(
                    new LambdaQueryWrapper<Attendance>()
                            .eq(Attendance::getEmployeeId, appeal.getEmployeeId())
                            .eq(Attendance::getAttendDate, appeal.getAttendDate())
            );
        }
        if (existing != null) {
            existing.setStatus(appeal.getToStatus());
            if (appeal.getCheckIn() != null) {
                existing.setCheckIn(appeal.getCheckIn());
            }
            if (appeal.getCheckOut() != null) {
                existing.setCheckOut(appeal.getCheckOut());
            }
            existing.setRemark("申诉单#" + appeal.getId());
            attendanceMapper.updateById(existing);
        } else {
            Attendance attendance = new Attendance();
            attendance.setEmployeeId(appeal.getEmployeeId());
            attendance.setAttendDate(appeal.getAttendDate());
            attendance.setStatus(appeal.getToStatus());
            attendance.setCheckIn(appeal.getCheckIn());
            attendance.setCheckOut(appeal.getCheckOut());
            attendance.setRemark("申诉单#" + appeal.getId());
            attendanceMapper.insert(attendance);
        }
    }

    @Override
    public void rejectAppeal(Long id, String remark) {
        AttendanceAppeal appeal = loadPending(appealMapper.selectById(id));
        fillApprove(appeal::setApproverId, appeal::setApproveTime, appeal::setApproveRemark, appeal::setStatus, 2, remark);
        appealMapper.updateById(appeal);
    }

    @Override
    public void cancelAppeal(Long id) {
        AttendanceAppeal appeal = loadPending(appealMapper.selectById(id));
        assertOwner(appeal.getEmployeeId());
        appeal.setStatus(3);
        appeal.setApproveTime(LocalDateTime.now());
        appealMapper.updateById(appeal);
    }

    @Override
    public void createFieldWork(FieldWorkCreateDTO dto) {
        Long employeeId = SecurityUtil.requireEmployeeId();
        FieldWorkRequest req = new FieldWorkRequest();
        req.setEmployeeId(employeeId);
        req.setWorkDate(dto.getWorkDate());
        req.setLocation(dto.getLocation());
        req.setReason(dto.getReason());
        req.setStatus(0);
        fieldWorkRequestMapper.insert(req);
    }

    @Override
    public PageResult<FieldWorkRequestVO> listFieldWork(PageQuery page) {
        LambdaQueryWrapper<FieldWorkRequest> wrapper = new LambdaQueryWrapper<>();
        applyScope(wrapper, FieldWorkRequest::getEmployeeId);
        wrapper.orderByDesc(FieldWorkRequest::getCreatedAt);
        IPage<FieldWorkRequest> iPage = fieldWorkRequestMapper.selectPage(
                new Page<>(page.getPageNum(), page.getPageSize()), wrapper);
        return toPage(iPage, list -> {
            Map<Long, String> names = loadEmpNames(list.stream().map(FieldWorkRequest::getEmployeeId).collect(Collectors.toSet()));
            return list.stream().map(r -> {
                FieldWorkRequestVO vo = new FieldWorkRequestVO();
                vo.setId(r.getId());
                vo.setEmployeeId(r.getEmployeeId());
                vo.setEmployeeName(names.get(r.getEmployeeId()));
                vo.setWorkDate(r.getWorkDate());
                vo.setLocation(r.getLocation());
                vo.setReason(r.getReason());
                vo.setStatus(r.getStatus());
                vo.setApproveTime(r.getApproveTime());
                vo.setApproveRemark(r.getApproveRemark());
                return vo;
            }).toList();
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveFieldWork(Long id, String remark) {
        FieldWorkRequest req = loadPending(fieldWorkRequestMapper.selectById(id));
        fillApprove(req::setApproverId, req::setApproveTime, req::setApproveRemark, req::setStatus, 1, remark);
        fieldWorkRequestMapper.updateById(req);

        Attendance existing = attendanceMapper.selectOne(
                new LambdaQueryWrapper<Attendance>()
                        .eq(Attendance::getEmployeeId, req.getEmployeeId())
                        .eq(Attendance::getAttendDate, req.getWorkDate())
        );
        String attRemark = "外勤单#" + req.getId() + " " + req.getLocation();
        if (existing != null) {
            existing.setStatus(STATUS_FIELD);
            existing.setRemark(attRemark);
            attendanceMapper.updateById(existing);
        } else {
            Attendance attendance = new Attendance();
            attendance.setEmployeeId(req.getEmployeeId());
            attendance.setAttendDate(req.getWorkDate());
            attendance.setStatus(STATUS_FIELD);
            attendance.setRemark(attRemark);
            attendanceMapper.insert(attendance);
        }
    }

    @Override
    public void rejectFieldWork(Long id, String remark) {
        FieldWorkRequest req = loadPending(fieldWorkRequestMapper.selectById(id));
        fillApprove(req::setApproverId, req::setApproveTime, req::setApproveRemark, req::setStatus, 2, remark);
        fieldWorkRequestMapper.updateById(req);
    }

    @Override
    public void cancelFieldWork(Long id) {
        FieldWorkRequest req = loadPending(fieldWorkRequestMapper.selectById(id));
        assertOwner(req.getEmployeeId());
        req.setStatus(3);
        req.setApproveTime(LocalDateTime.now());
        fieldWorkRequestMapper.updateById(req);
    }

    private <T> T loadPending(T entity) {
        if (entity == null) {
            throw new BusinessException("申请不存在");
        }
        Integer status;
        if (entity instanceof OvertimeRequest r) {
            status = r.getStatus();
        } else if (entity instanceof AttendanceAppeal r) {
            status = r.getStatus();
        } else if (entity instanceof FieldWorkRequest r) {
            status = r.getStatus();
        } else {
            throw new BusinessException("申请不存在");
        }
        if (status == null || status != 0) {
            throw new BusinessException("该申请已处理，无法再次处理");
        }
        return entity;
    }

    private void fillApprove(
            java.util.function.Consumer<Long> setApprover,
            java.util.function.Consumer<LocalDateTime> setTime,
            java.util.function.Consumer<String> setRemark,
            java.util.function.Consumer<Integer> setStatus,
            int status,
            String remark) {
        LoginUser user = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        setApprover.accept(user.getUserId());
        setTime.accept(LocalDateTime.now());
        setRemark.accept(remark);
        setStatus.accept(status);
    }

    private void assertOwner(Long employeeId) {
        if (!Objects.equals(employeeId, SecurityUtil.requireEmployeeId()) && !SecurityUtil.isHrStaff()) {
            throw new BusinessException("只能撤销本人申请");
        }
    }

    private <T> void applyScope(LambdaQueryWrapper<T> wrapper,
                                com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, Long> col) {
        if (SecurityUtil.hasRole("EMPLOYEE") && !SecurityUtil.isManagerUp()) {
            wrapper.eq(col, SecurityUtil.requireEmployeeId());
            return;
        }
        if (SecurityUtil.isHrStaff()) {
            return;
        }
        Long deptId = SecurityUtil.requireDeptId();
        List<Long> empIds = employeeMapper.selectList(
                new LambdaQueryWrapper<Employee>().eq(Employee::getDeptId, deptId)
        ).stream().map(Employee::getId).toList();
        if (empIds.isEmpty()) {
            wrapper.eq(col, -1L);
            return;
        }
        wrapper.in(col, empIds);
    }

    private Map<Long, String> loadEmpNames(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return employeeMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Employee::getId, Employee::getName));
    }

    private <E, V> PageResult<V> toPage(IPage<E> iPage, Function<List<E>, List<V>> mapper) {
        List<E> records = iPage.getRecords();
        if (records == null || records.isEmpty()) {
            return PageResult.empty();
        }
        PageResult<V> result = new PageResult<>();
        result.setRecords(mapper.apply(records));
        result.setTotal(iPage.getTotal());
        result.setPageNum(iPage.getCurrent());
        result.setPageSize(iPage.getSize());
        result.setPages(iPage.getPages());
        return result;
    }
}
