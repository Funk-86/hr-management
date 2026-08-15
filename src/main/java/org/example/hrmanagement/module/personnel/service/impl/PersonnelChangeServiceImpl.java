package org.example.hrmanagement.module.personnel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.hrmanagement.common.exception.BusinessException;
import org.example.hrmanagement.common.result.PageResult;
import org.example.hrmanagement.common.util.SecurityUtil;
import org.example.hrmanagement.module.department.entity.Department;
import org.example.hrmanagement.module.department.mapper.DepartmentMapper;
import org.example.hrmanagement.module.document.entity.EmployeeDocument;
import org.example.hrmanagement.module.document.mapper.EmployeeDocumentMapper;
import org.example.hrmanagement.module.employee.entity.Employee;
import org.example.hrmanagement.module.employee.mapper.EmployeeMapper;
import org.example.hrmanagement.module.personnel.dto.PersonnelApproveDTO;
import org.example.hrmanagement.module.personnel.dto.PersonnelChangeCreateDTO;
import org.example.hrmanagement.module.personnel.dto.PersonnelChangeQueryDTO;
import org.example.hrmanagement.module.personnel.entity.PersonnelChange;
import org.example.hrmanagement.module.personnel.mapper.PersonnelChangeMapper;
import org.example.hrmanagement.module.personnel.service.PersonnelChangeService;
import org.example.hrmanagement.module.personnel.vo.PersonnelChangeVO;
import org.example.hrmanagement.module.position.entity.Position;
import org.example.hrmanagement.module.position.mapper.PositionMapper;
import org.example.hrmanagement.module.salary.entity.SalaryBaseDict;
import org.example.hrmanagement.module.salary.mapper.SalaryBaseDictMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PersonnelChangeServiceImpl implements PersonnelChangeService {

    private static final Map<Integer, String> TYPE_LABEL = Map.of(
            1, "调岗", 2, "调薪", 3, "离职", 4, "入职完善");
    private static final Map<Integer, String> STATUS_LABEL = Map.of(
            0, "待审批", 1, "已通过", 2, "已拒绝", 3, "已撤销", 4, "已生效");

    private final PersonnelChangeMapper changeMapper;
    private final EmployeeMapper employeeMapper;
    private final DepartmentMapper departmentMapper;
    private final PositionMapper positionMapper;
    private final SalaryBaseDictMapper salaryBaseDictMapper;
    private final EmployeeDocumentMapper documentMapper;

    @Override
    public PageResult<PersonnelChangeVO> page(PersonnelChangeQueryDTO query) {
        Set<Long> scoped = resolveScopedEmployeeIds();
        if (scoped != null && scoped.isEmpty()) {
            return PageResult.empty();
        }
        if (query.getEmployeeId() != null && scoped != null && !scoped.contains(query.getEmployeeId())) {
            return PageResult.empty();
        }

        LambdaQueryWrapper<PersonnelChange> wrapper = new LambdaQueryWrapper<PersonnelChange>()
                .in(scoped != null, PersonnelChange::getEmployeeId, scoped)
                .eq(query.getEmployeeId() != null, PersonnelChange::getEmployeeId, query.getEmployeeId())
                .eq(query.getChangeType() != null, PersonnelChange::getChangeType, query.getChangeType())
                .eq(query.getStatus() != null, PersonnelChange::getStatus, query.getStatus())
                .orderByDesc(PersonnelChange::getCreatedAt);

        IPage<PersonnelChange> iPage = changeMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        if (iPage.getRecords() == null || iPage.getRecords().isEmpty()) {
            return PageResult.empty();
        }
        List<PersonnelChangeVO> vos = toVos(iPage.getRecords());
        PageResult<PersonnelChangeVO> result = new PageResult<>();
        result.setRecords(vos);
        result.setTotal(iPage.getTotal());
        result.setPageNum(iPage.getCurrent());
        result.setPageSize(iPage.getSize());
        result.setPages(iPage.getPages());
        return result;
    }

    @Override
    public PersonnelChangeVO getDetail(Long id) {
        PersonnelChange change = requireChange(id);
        assertCanView(change.getEmployeeId());
        return toVos(List.of(change)).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(PersonnelChangeCreateDTO dto) {
        if (dto.getChangeType() == null || dto.getChangeType() < 1 || dto.getChangeType() > 4) {
            throw new BusinessException("异动类型无效");
        }
        Employee emp = employeeMapper.selectById(dto.getEmployeeId());
        if (emp == null) {
            throw new BusinessException("员工不存在");
        }
        assertCanApply(emp);

        Long pending = changeMapper.selectCount(
                new LambdaQueryWrapper<PersonnelChange>()
                        .eq(PersonnelChange::getEmployeeId, dto.getEmployeeId())
                        .eq(PersonnelChange::getChangeType, dto.getChangeType())
                        .in(PersonnelChange::getStatus, 0, 1));
        if (pending != null && pending > 0) {
            throw new BusinessException("该员工已有同类型待处理异动单");
        }

        PersonnelChange change = new PersonnelChange();
        change.setChangeType(dto.getChangeType());
        change.setEmployeeId(dto.getEmployeeId());
        change.setFromDeptId(emp.getDeptId());
        change.setFromPositionId(emp.getPositionId());
        change.setOldSalary(resolveCurrentSalary(emp));
        change.setReason(dto.getReason());
        change.setEffectiveDate(dto.getEffectiveDate() != null ? dto.getEffectiveDate() : LocalDate.now());
        change.setApplicantId(SecurityUtil.requireEmployeeId());
        change.setStatus(0);

        switch (dto.getChangeType()) {
            case 1 -> {
                if (dto.getToDeptId() == null || dto.getToPositionId() == null) {
                    throw new BusinessException("调岗需指定目标部门与岗位");
                }
                requireDept(dto.getToDeptId());
                requirePosition(dto.getToPositionId(), dto.getToDeptId());
                change.setToDeptId(dto.getToDeptId());
                change.setToPositionId(dto.getToPositionId());
            }
            case 2 -> {
                if (dto.getNewSalary() == null || dto.getNewSalary().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessException("请填写有效的新底薪");
                }
                change.setNewSalary(dto.getNewSalary());
                change.setToDeptId(emp.getDeptId());
                change.setToPositionId(emp.getPositionId());
            }
            case 3 -> {
                if (emp.getStatus() != null && emp.getStatus() == 3) {
                    throw new BusinessException("员工已离职");
                }
                change.setToDeptId(emp.getDeptId());
                change.setToPositionId(emp.getPositionId());
            }
            case 4 -> {
                change.setToDeptId(emp.getDeptId());
                change.setToPositionId(emp.getPositionId());
            }
            default -> throw new BusinessException("异动类型无效");
        }

        changeMapper.insert(change);
        return change.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, PersonnelApproveDTO dto) {
        if (!SecurityUtil.isHrStaff() && !SecurityUtil.isManagerUp()) {
            throw new BusinessException("无权审批");
        }
        PersonnelChange change = requireChange(id);
        if (change.getStatus() == null || change.getStatus() != 0) {
            throw new BusinessException("仅待审批单据可审批");
        }
        assertCanApprove(change);

        boolean approved = Boolean.TRUE.equals(dto.getApproved());
        change.setStatus(approved ? 1 : 2);
        change.setApproverId(SecurityUtil.requireEmployeeId());
        change.setApproveRemark(dto.getApproveRemark());
        change.setApprovedAt(LocalDateTime.now());
        changeMapper.updateById(change);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        PersonnelChange change = requireChange(id);
        Long myId = SecurityUtil.requireEmployeeId();
        boolean canCancel = SecurityUtil.isHrStaff()
                || Objects.equals(change.getApplicantId(), myId);
        if (!canCancel) {
            throw new BusinessException("仅申请人或人事可撤销");
        }
        if (change.getStatus() == null || (change.getStatus() != 0 && change.getStatus() != 1)) {
            throw new BusinessException("当前状态不可撤销");
        }
        change.setStatus(3);
        changeMapper.updateById(change);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void effect(Long id) {
        if (!SecurityUtil.isHrStaff() && !SecurityUtil.isManagerUp()) {
            throw new BusinessException("无权执行生效");
        }
        PersonnelChange change = requireChange(id);
        if (change.getStatus() == null || change.getStatus() != 1) {
            throw new BusinessException("仅审批通过的单据可生效");
        }
        assertCanApprove(change);

        Employee emp = employeeMapper.selectById(change.getEmployeeId());
        if (emp == null) {
            throw new BusinessException("员工不存在");
        }

        switch (change.getChangeType()) {
            case 1 -> {
                emp.setDeptId(change.getToDeptId());
                emp.setPositionId(change.getToPositionId());
                employeeMapper.updateById(emp);
            }
            case 2 -> {
                emp.setBaseSalary(change.getNewSalary());
                employeeMapper.updateById(emp);
            }
            case 3 -> {
                emp.setStatus(3);
                emp.setLeaveDate(change.getEffectiveDate() != null ? change.getEffectiveDate() : LocalDate.now());
                employeeMapper.updateById(emp);
            }
            case 4 -> {
                Long contractCount = documentMapper.selectCount(
                        new LambdaQueryWrapper<EmployeeDocument>()
                                .eq(EmployeeDocument::getEmployeeId, emp.getId())
                                .in(EmployeeDocument::getDocType, 1, 2));
                if (contractCount == null || contractCount == 0) {
                    throw new BusinessException("入职完善需先在文档管理上传劳动合同或保密协议");
                }
                if (emp.getStatus() != null && emp.getStatus() == 2
                        && emp.getProbationEnd() != null
                        && !emp.getProbationEnd().isAfter(LocalDate.now())) {
                    emp.setStatus(1);
                    employeeMapper.updateById(emp);
                } else if (StringUtils.hasText(change.getReason())) {
                    String remark = StringUtils.hasText(emp.getRemark())
                            ? emp.getRemark() + "；入职完善：" + change.getReason()
                            : "入职完善：" + change.getReason();
                    emp.setRemark(remark.length() > 500 ? remark.substring(0, 500) : remark);
                    employeeMapper.updateById(emp);
                }
            }
            default -> throw new BusinessException("异动类型无效");
        }

        change.setStatus(4);
        change.setEffectedAt(LocalDateTime.now());
        change.setEffectedBy(SecurityUtil.requireEmployeeId());
        if (change.getEffectiveDate() == null) {
            change.setEffectiveDate(LocalDate.now());
        }
        changeMapper.updateById(change);
    }

    private Set<Long> resolveScopedEmployeeIds() {
        if (SecurityUtil.isHrStaff()) {
            return null;
        }
        Long myId = SecurityUtil.requireEmployeeId();
        Set<Long> ids = new HashSet<>();
        ids.add(myId);
        if (SecurityUtil.isManagerUp()) {
            Employee me = employeeMapper.selectById(myId);
            if (me != null && me.getDeptId() != null) {
                employeeMapper.selectList(
                                new LambdaQueryWrapper<Employee>().eq(Employee::getDeptId, me.getDeptId()))
                        .forEach(e -> ids.add(e.getId()));
            }
        }
        return ids;
    }

    private void assertCanView(Long employeeId) {
        Set<Long> scoped = resolveScopedEmployeeIds();
        if (scoped != null && !scoped.contains(employeeId)) {
            throw new BusinessException("无权查看该异动单");
        }
    }

    private void assertCanApply(Employee emp) {
        if (SecurityUtil.isHrStaff()) {
            return;
        }
        Long myId = SecurityUtil.requireEmployeeId();
        if (Objects.equals(emp.getId(), myId)) {
            return;
        }
        if (!SecurityUtil.isManagerUp()) {
            throw new BusinessException("无权发起异动");
        }
        Employee me = employeeMapper.selectById(myId);
        if (me == null || !Objects.equals(me.getDeptId(), emp.getDeptId())) {
            throw new BusinessException("只能为本部门员工发起异动");
        }
    }

    private void assertCanApprove(PersonnelChange change) {
        if (SecurityUtil.isHrStaff()) {
            return;
        }
        Employee emp = employeeMapper.selectById(change.getEmployeeId());
        Employee me = employeeMapper.selectById(SecurityUtil.requireEmployeeId());
        if (emp == null || me == null || !Objects.equals(me.getDeptId(), emp.getDeptId())) {
            throw new BusinessException("只能审批本部门异动");
        }
    }

    private PersonnelChange requireChange(Long id) {
        if (id == null) {
            throw new BusinessException("异动单ID不能为空");
        }
        PersonnelChange change = changeMapper.selectById(id);
        if (change == null) {
            throw new BusinessException("异动单不存在");
        }
        return change;
    }

    private void requireDept(Long deptId) {
        if (departmentMapper.selectById(deptId) == null) {
            throw new BusinessException("目标部门不存在");
        }
    }

    private void requirePosition(Long positionId, Long deptId) {
        Position p = positionMapper.selectById(positionId);
        if (p == null) {
            throw new BusinessException("目标岗位不存在");
        }
        if (deptId != null && p.getDeptId() != null && !Objects.equals(p.getDeptId(), deptId)) {
            throw new BusinessException("目标岗位不属于目标部门");
        }
    }

    private BigDecimal resolveCurrentSalary(Employee emp) {
        if (emp.getBaseSalary() != null) {
            return emp.getBaseSalary();
        }
        if (emp.getPositionId() == null) {
            return null;
        }
        SalaryBaseDict dict = salaryBaseDictMapper.selectOne(
                new LambdaQueryWrapper<SalaryBaseDict>()
                        .eq(SalaryBaseDict::getPositionId, emp.getPositionId())
                        .eq(SalaryBaseDict::getStatus, 1)
                        .last("LIMIT 1"));
        return dict == null ? null : dict.getBaseSalary();
    }

    private List<PersonnelChangeVO> toVos(List<PersonnelChange> list) {
        Set<Long> empIds = new HashSet<>();
        Set<Long> deptIds = new HashSet<>();
        Set<Long> posIds = new HashSet<>();
        for (PersonnelChange c : list) {
            if (c.getEmployeeId() != null) empIds.add(c.getEmployeeId());
            if (c.getApplicantId() != null) empIds.add(c.getApplicantId());
            if (c.getApproverId() != null) empIds.add(c.getApproverId());
            if (c.getFromDeptId() != null) deptIds.add(c.getFromDeptId());
            if (c.getToDeptId() != null) deptIds.add(c.getToDeptId());
            if (c.getFromPositionId() != null) posIds.add(c.getFromPositionId());
            if (c.getToPositionId() != null) posIds.add(c.getToPositionId());
        }
        Map<Long, Employee> empMap = empIds.isEmpty() ? Map.of()
                : employeeMapper.selectBatchIds(empIds).stream()
                .collect(Collectors.toMap(Employee::getId, e -> e, (a, b) -> a));
        Map<Long, String> deptNames = deptIds.isEmpty() ? Map.of()
                : departmentMapper.selectBatchIds(deptIds).stream()
                .collect(Collectors.toMap(Department::getId, Department::getDeptName, (a, b) -> a));
        Map<Long, String> posNames = posIds.isEmpty() ? Map.of()
                : positionMapper.selectBatchIds(posIds).stream()
                .collect(Collectors.toMap(Position::getId, Position::getPositionName, (a, b) -> a));

        Set<Long> onboardEmpIds = list.stream()
                .filter(c -> Objects.equals(c.getChangeType(), 4))
                .map(PersonnelChange::getEmployeeId)
                .collect(Collectors.toSet());
        Map<Long, Long> docCountMap = new java.util.HashMap<>();
        if (!onboardEmpIds.isEmpty()) {
            List<EmployeeDocument> docs = documentMapper.selectList(
                    new LambdaQueryWrapper<EmployeeDocument>()
                            .in(EmployeeDocument::getEmployeeId, onboardEmpIds)
                            .in(EmployeeDocument::getDocType, 1, 2));
            for (EmployeeDocument d : docs) {
                docCountMap.merge(d.getEmployeeId(), 1L, Long::sum);
            }
        }

        return list.stream().map(c -> {
            PersonnelChangeVO vo = new PersonnelChangeVO();
            vo.setId(c.getId());
            vo.setChangeType(c.getChangeType());
            vo.setChangeTypeLabel(TYPE_LABEL.get(c.getChangeType()));
            vo.setEmployeeId(c.getEmployeeId());
            Employee emp = empMap.get(c.getEmployeeId());
            if (emp != null) {
                vo.setEmployeeName(emp.getName());
                vo.setEmpNo(emp.getEmpNo());
            }
            vo.setFromDeptId(c.getFromDeptId());
            vo.setFromDeptName(deptNames.get(c.getFromDeptId()));
            vo.setToDeptId(c.getToDeptId());
            vo.setToDeptName(deptNames.get(c.getToDeptId()));
            vo.setFromPositionId(c.getFromPositionId());
            vo.setFromPositionName(posNames.get(c.getFromPositionId()));
            vo.setToPositionId(c.getToPositionId());
            vo.setToPositionName(posNames.get(c.getToPositionId()));
            vo.setOldSalary(c.getOldSalary());
            vo.setNewSalary(c.getNewSalary());
            vo.setEffectiveDate(c.getEffectiveDate());
            vo.setReason(c.getReason());
            vo.setStatus(c.getStatus());
            vo.setStatusLabel(STATUS_LABEL.get(c.getStatus()));
            vo.setApplicantId(c.getApplicantId());
            Employee applicant = empMap.get(c.getApplicantId());
            vo.setApplicantName(applicant != null ? applicant.getName() : null);
            vo.setApproverId(c.getApproverId());
            Employee approver = empMap.get(c.getApproverId());
            vo.setApproverName(approver != null ? approver.getName() : null);
            vo.setApproveRemark(c.getApproveRemark());
            vo.setApprovedAt(c.getApprovedAt());
            vo.setEffectedAt(c.getEffectedAt());
            vo.setCreatedAt(c.getCreatedAt());
            if (Objects.equals(c.getChangeType(), 4)) {
                vo.setContractDocCount(docCountMap.getOrDefault(c.getEmployeeId(), 0L).intValue());
            }
            return vo;
        }).toList();
    }
}
