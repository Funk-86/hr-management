package org.example.hrmanagement.module.leave.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.hrmanagement.common.dto.PageQuery;
import org.example.hrmanagement.common.exception.BusinessException;
import org.example.hrmanagement.common.result.PageResult;
import org.example.hrmanagement.common.security.LoginUser;
import org.example.hrmanagement.common.util.SecurityUtil;
import org.example.hrmanagement.module.attendance.entity.Attendance;
import org.example.hrmanagement.module.employee.entity.Employee;
import org.example.hrmanagement.module.employee.mapper.EmployeeMapper;
import org.example.hrmanagement.module.leave.dto.LeaveRequestCreateDTO;
import org.example.hrmanagement.module.leave.dto.LeaveTypeCreateDTO;
import org.example.hrmanagement.module.leave.dto.LeaveTypeUpdateDTO;
import org.example.hrmanagement.module.leave.entity.LeaveRequest;
import org.example.hrmanagement.module.leave.entity.LeaveType;
import org.example.hrmanagement.module.leave.mapper.LeaveRequestMapper;
import org.example.hrmanagement.module.leave.mapper.LeaveTypeMapper;
import org.example.hrmanagement.module.leave.service.LeaveService;
import org.example.hrmanagement.module.leave.vo.LeaveRequestVO;
import org.example.hrmanagement.module.leave.vo.LeaveTypeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LeaveServiceImpl implements LeaveService {
    @Autowired
    private LeaveRequestMapper leaveRequestMapper;
    @Autowired
    private LeaveTypeMapper leaveTypeMapper;
    @Autowired
    private EmployeeMapper employeeMapper;

    @Override
    public List<LeaveTypeVO> getLeaveType() {
        List<LeaveType> list = leaveTypeMapper.selectList(new LambdaQueryWrapper<LeaveType>().eq(LeaveType::getStatus, 1));
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        return list.stream()
                .map(this::toLeaveTypeVO)
                .toList();
    }

    @Override
    public void insertLeaveType(LeaveTypeCreateDTO dto) {
        Long count = leaveTypeMapper.selectCount(new LambdaQueryWrapper<LeaveType>().eq(LeaveType::getTypeCode, dto.getTypeCode()));
        if (count > 0) {
            throw new BusinessException("类型编码已存在");
        }
        leaveTypeMapper.insert(this.toLeaveType(dto));
    }

    @Override
    public void updateLeaveType(Long id,LeaveTypeUpdateDTO dto) {
        if (id == null) {
            throw new BusinessException("id不能为空");
        }
        LeaveType existing = leaveTypeMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("请假类型不存在");
        }
        Long count = leaveTypeMapper.selectCount(
                new LambdaQueryWrapper<LeaveType>()
                        .eq(LeaveType::getTypeCode, dto.getTypeCode())
                        .ne(LeaveType::getId, id)
        );
        if (count > 0) {
            throw new BusinessException("类型编码已存在");
        }

        existing.setTypeName(dto.getTypeName());
        existing.setTypeCode(dto.getTypeCode());
        if (dto.getStatus() != null) {
            existing.setStatus(dto.getStatus());
        }
        if (dto.getMaxDays() != null) {
            existing.setMaxDays(dto.getMaxDays());
        }
        leaveTypeMapper.updateById(existing);
    }

    @Override
    public void deleteLeaveType(Long id) {
        if(id==null){
            throw new BusinessException("id不能为空");
        }
        LeaveType existing = leaveTypeMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("请假类型不存在");
        }
        Long count = leaveRequestMapper.selectCount(
                new LambdaQueryWrapper<LeaveRequest>().eq(LeaveRequest::getLeaveTypeId, id)
        );
        if (count > 0) {
            throw new BusinessException("该假期类型下存在请假记录，无法删除");
        }
        existing.setStatus(0);
        leaveTypeMapper.updateById(existing);
    }

    @Override
    public void requestLeave(LeaveRequestCreateDTO dto) {
        Long employeeId = SecurityUtil.requireEmployeeId();
        if (!dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new BusinessException("结束时间必须晚于开始时间");
        }
        if (dto.getDays() == null || dto.getDays().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("请假天数不合法");
        }
        Employee employee=employeeMapper.selectById(employeeId);
        if(employee==null){
            throw new BusinessException("该员工不存在");
        }
        LeaveType leaveType = leaveTypeMapper.selectById(dto.getLeaveTypeId());
        if (leaveType == null) {
            throw new BusinessException("请假类型不存在");
        }
        if (leaveType.getStatus() == null || leaveType.getStatus() != 1) {
            throw new BusinessException("请假类型不可用");
        }
        if (leaveType.getMaxDays() != null
                && dto.getDays().compareTo(BigDecimal.valueOf(leaveType.getMaxDays())) > 0) {
            throw new BusinessException("请假天数超过该类型的最大天数");
        }
        Long conflictCount = leaveRequestMapper.selectCount(
                new LambdaQueryWrapper<LeaveRequest>()
                        .eq(LeaveRequest::getEmployeeId, employeeId)
                        .in(LeaveRequest::getStatus, 0, 1)
                        .lt(LeaveRequest::getStartTime, dto.getEndTime())
                        .gt(LeaveRequest::getEndTime, dto.getStartTime())
        );
        if (conflictCount > 0) {
            throw new BusinessException("该时间段已有请假申请，请调整时间");
        }
        leaveRequestMapper.insert(toLeaveRequest(dto,employeeId));
    }

    @Override
    public LeaveRequestVO getLeaveRequestById(Long id) {
        if(id==null){
            throw new BusinessException("id不能为空");
        }
        LeaveRequest leaveRequest = leaveRequestMapper.selectById(id);
        if(leaveRequest==null){
            throw new BusinessException("请假记录不存在");
        }
        Employee employee = employeeMapper.selectById(leaveRequest.getEmployeeId());
        if(employee==null){
            throw new BusinessException("员工不存在");
        }
        LeaveType leaveType = leaveTypeMapper.selectById(leaveRequest.getLeaveTypeId());
        if(leaveType==null){
            throw new BusinessException("请假类型不存在");
        }
        return toLeaveRequestVO(leaveRequest,leaveType.getTypeName(),employee.getName());
    }

    @Override
    public void approveLeave(Long id,String approveRemark) {
        processLeaveStatus(id, 1, approveRemark);
    }

    @Override
    public void rejectLeave(Long id,String approveRemark) {
        processLeaveStatus(id, 2, approveRemark);
    }

    private void processLeaveStatus(Long id, Integer status, String approveRemark) {
        if(id==null){
            throw new BusinessException("id不能为空");
        }
        LeaveRequest leaveRequest = leaveRequestMapper.selectById(id);
        if(leaveRequest==null){
            throw new BusinessException("请假不存在");
        }
        if(leaveRequest.getStatus()!=0){
            throw new BusinessException("该申请已处理，无法再次处理");
        }
        LoginUser user=(LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long approverId=user.getUserId();

        leaveRequest.setApproverId(approverId);
        leaveRequest.setStatus(status);
        leaveRequest.setApproveTime(LocalDateTime.now());
        leaveRequest.setApproveRemark(approveRemark);
        leaveRequestMapper.updateById(leaveRequest);
    }

    @Override
    public void cancelLeave(Long id, String approveRemark) {
        if(id==null){
            throw new BusinessException("id不能为空");
        }
        LeaveRequest leaveRequest = leaveRequestMapper.selectById(id);
        if(leaveRequest==null){
            throw new BusinessException("请假不存在");
        }
        if(leaveRequest.getStatus()!=0){
            throw new BusinessException("该申请已处理，无法再次处理");
        }

        leaveRequest.setStatus(3);
        leaveRequest.setApproveTime(LocalDateTime.now());
        leaveRequestMapper.updateById(leaveRequest);
    }

    @Override
    public PageResult<LeaveRequestVO> getLeaveRequest(PageQuery page) {
        LambdaQueryWrapper<LeaveRequest> wrapper = new LambdaQueryWrapper<>();
        applyRequestScop(wrapper);
        wrapper.orderByDesc(LeaveRequest::getCreatedAt);

        IPage<LeaveRequest> iPage = leaveRequestMapper.selectPage(
                new Page<>(page.getPageNum(), page.getPageSize()), wrapper);

        List<LeaveRequest> leaveRequests = iPage.getRecords();
        if(leaveRequests==null || leaveRequests.size()==0){
            return PageResult.empty();
        }
        Set<Long> employeeId=leaveRequests.stream().map(LeaveRequest::getEmployeeId).filter(id->id!=null).collect(Collectors.toSet());
        Set<Long> leaveTypeId=leaveRequests.stream().map(LeaveRequest::getLeaveTypeId).filter(id->id!=null).collect(Collectors.toSet());

        Map<Long,String> employeeName=employeeId.isEmpty()
                ? Map.of()
                : employeeMapper.selectBatchIds(employeeId).stream().collect(Collectors.toMap(Employee::getId, Employee::getName));
        Map<Long,String> leaveType=leaveTypeId.isEmpty()
                ? Map.of()
                : leaveTypeMapper.selectBatchIds(leaveTypeId).stream().collect(Collectors.toMap(LeaveType::getId, LeaveType::getTypeName));
        List<LeaveRequestVO> vos = leaveRequests.stream().map(leaveRequest -> toLeaveRequestVO(leaveRequest,leaveType.get(leaveRequest.getLeaveTypeId()),employeeName.get(leaveRequest.getEmployeeId()))).collect(Collectors.toList());

        PageResult<LeaveRequestVO> result = new PageResult<>();
        result.setRecords(vos);
        result.setTotal(iPage.getTotal());
        result.setPageNum(iPage.getCurrent());
        result.setPageSize(iPage.getSize());
        result.setPages(iPage.getPages());
        return result;
    }

    private void applyRequestScop(LambdaQueryWrapper<LeaveRequest> wrapper) {
        if (SecurityUtil.hasRole("EMPLOYEE") && !SecurityUtil.isManagerUp()) {
            wrapper.eq(LeaveRequest::getEmployeeId, SecurityUtil.requireEmployeeId());
            return;
        }
        // HR / 超管：查看全部请假，不做部门过滤
        if (SecurityUtil.isHrStaff()) {
            return;
        }
        // 部门经理：本部门员工
        Long deptId = SecurityUtil.requireDeptId();
        List<Long> empIds = employeeMapper.selectList(
                new LambdaQueryWrapper<Employee>().eq(Employee::getDeptId, deptId)
        ).stream().map(Employee::getId).toList();
        if (empIds.isEmpty()) {
            wrapper.eq(LeaveRequest::getEmployeeId, -1L);
            return;
        }
        wrapper.in(LeaveRequest::getEmployeeId, empIds);
    }


    private LeaveRequestVO toLeaveRequestVO(LeaveRequest leaveRequest,String leaveType,String employeeName) {
        LeaveRequestVO leaveRequestVO = new LeaveRequestVO();
        leaveRequestVO.setLeaveType(leaveType);
        leaveRequestVO.setEmployeeName(employeeName);
        leaveRequestVO.setStartTime(leaveRequest.getStartTime());
        leaveRequestVO.setEndTime(leaveRequest.getEndTime());
        leaveRequestVO.setDays(leaveRequest.getDays());
        leaveRequestVO.setStatus(leaveRequest.getStatus());
        leaveRequestVO.setReason(leaveRequest.getReason());
        leaveRequestVO.setId(leaveRequest.getId());
        return leaveRequestVO;
    }

    private LeaveRequest toLeaveRequest(LeaveRequestCreateDTO dto,Long employeeId) {
        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setEmployeeId(employeeId);
        leaveRequest.setLeaveTypeId(dto.getLeaveTypeId());
        leaveRequest.setStartTime(dto.getStartTime());
        leaveRequest.setEndTime(dto.getEndTime());
        leaveRequest.setDays(dto.getDays());
        leaveRequest.setReason(dto.getReason());
        leaveRequest.setStatus(0);  // 待审批
        return leaveRequest;
    }

    private LeaveType toLeaveType(LeaveTypeCreateDTO dto) {
        LeaveType leaveType = new LeaveType();
        leaveType.setTypeCode(dto.getTypeCode());
        leaveType.setTypeName(dto.getTypeName());
        leaveType.setStatus(dto.getStatus()!=null ? dto.getStatus() : 1);
        leaveType.setMaxDays(dto.getMaxDays());
        return leaveType;
    }

    private LeaveTypeVO toLeaveTypeVO(LeaveType leaveType) {
        LeaveTypeVO vo = new LeaveTypeVO();
        vo.setId(leaveType.getId());
        vo.setTypeCode(leaveType.getTypeCode());
        vo.setTypeName(leaveType.getTypeName());
        vo.setStatus(leaveType.getStatus());
        vo.setMaxDays(leaveType.getMaxDays());
        return vo;
    }
}
