package org.example.hrmanagement.module.salary.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.hrmanagement.common.dto.PageQuery;
import org.example.hrmanagement.common.exception.BusinessException;
import org.example.hrmanagement.common.result.PageResult;
import org.example.hrmanagement.common.util.SecurityUtil;
import org.example.hrmanagement.module.attendance.entity.Attendance;
import org.example.hrmanagement.module.attendance.mapper.AttendanceMapper;
import org.example.hrmanagement.module.employee.entity.Employee;
import org.example.hrmanagement.module.employee.mapper.EmployeeMapper;
import org.example.hrmanagement.module.position.entity.Position;
import org.example.hrmanagement.module.position.mapper.PositionMapper;
import org.example.hrmanagement.module.salary.dto.AttendanceDeductRuleUpdateDTO;
import org.example.hrmanagement.module.salary.dto.SalaryCreateDTO;
import org.example.hrmanagement.module.salary.dto.SalaryGenerateDTO;
import org.example.hrmanagement.module.salary.dto.SalaryUpdateDTO;
import org.example.hrmanagement.module.salary.entity.AttendanceDeductRule;
import org.example.hrmanagement.module.salary.entity.Salary;
import org.example.hrmanagement.module.salary.entity.SalaryBaseDict;
import org.example.hrmanagement.module.salary.mapper.AttendanceDeductRuleMapper;
import org.example.hrmanagement.module.salary.mapper.SalaryBaseDictMapper;
import org.example.hrmanagement.module.salary.mapper.SalaryMapper;
import org.example.hrmanagement.module.salary.service.SalaryService;
import org.example.hrmanagement.module.salary.vo.AttendanceDeductRuleVO;
import org.example.hrmanagement.module.salary.vo.SalaryPreviewVO;
import org.example.hrmanagement.module.salary.vo.SalaryVO;
import org.example.hrmanagement.module.task.entity.TaskAssignee;
import org.example.hrmanagement.module.task.entity.TaskHallDeduct;
import org.example.hrmanagement.module.task.mapper.TaskAssigneeMapper;
import org.example.hrmanagement.module.task.mapper.TaskHallDeductMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalaryServiceImpl implements SalaryService {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int STATUS_LATE = 2;
    private static final int STATUS_ABSENT = 4;
    private static final int STATUS_LEAVE = 5;
    private static final int STATUS_FIELD = 6;

    private final SalaryMapper salaryMapper;
    private final EmployeeMapper employeeMapper;
    private final PositionMapper positionMapper;
    private final SalaryBaseDictMapper salaryBaseDictMapper;
    private final TaskAssigneeMapper taskAssigneeMapper;
    private final TaskHallDeductMapper taskHallDeductMapper;
    private final AttendanceMapper attendanceMapper;
    private final AttendanceDeductRuleMapper deductRuleMapper;

    @Override
    public PageResult<SalaryVO> getSalaryAll(PageQuery page) {
        IPage<Salary> iPage = salaryMapper.selectPage(
                new Page<>(page.getPageNum(), page.getPageSize()),
                new LambdaQueryWrapper<Salary>().orderByDesc(Salary::getSalaryMonth));
        List<Salary> list = iPage.getRecords();
        if (list == null || list.isEmpty()) {
            return PageResult.empty();
        }

        Set<Long> employeeIds = list.stream()
                .map(Salary::getEmployeeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Employee> employeeMap = employeeIds.isEmpty()
                ? Map.of()
                : employeeMapper.selectBatchIds(employeeIds).stream()
                .collect(Collectors.toMap(Employee::getId, e -> e));

        Set<Long> positionIds = list.stream()
                .map(Salary::getPositionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> positionNames = positionIds.isEmpty()
                ? Map.of()
                : positionMapper.selectBatchIds(positionIds).stream()
                .collect(Collectors.toMap(Position::getId, Position::getPositionName, (a, b) -> a));

        List<SalaryVO> vos = list.stream().map(salary -> {
            Employee emp = salary.getEmployeeId() == null
                    ? null
                    : employeeMap.get(salary.getEmployeeId());
            // Map.of() 不允许 null key；历史薪资 position_id 可能为空
            String positionName = salary.getPositionId() == null
                    ? null
                    : positionNames.get(salary.getPositionId());
            return toSalaryVO(
                    salary,
                    emp != null ? emp.getName() : null,
                    emp != null ? emp.getEmpNo() : null,
                    positionName);
        }).collect(Collectors.toList());

        PageResult<SalaryVO> result = new PageResult<>();
        result.setRecords(vos);
        result.setTotal(iPage.getTotal());
        result.setPageNum(iPage.getCurrent());
        result.setPageSize(iPage.getSize());
        result.setPages(iPage.getPages());
        return result;
    }

    @Override
    public SalaryVO getSalaryById(Long id) {
        if (id == null) {
            throw new BusinessException("id不为空");
        }
        Salary salary = salaryMapper.selectById(id);
        if (salary == null) {
            throw new BusinessException("薪资不存在");
        }
        Employee emp = employeeMapper.selectById(salary.getEmployeeId());
        if (emp == null) {
            throw new BusinessException("员工不存在");
        }
        String positionName = null;
        if (salary.getPositionId() != null) {
            Position p = positionMapper.selectById(salary.getPositionId());
            positionName = p == null ? null : p.getPositionName();
        }
        return toSalaryVO(salary, emp.getName(), emp.getEmpNo(), positionName);
    }

    @Override
    public PageResult<SalaryVO> getMyPaidSalaries(PageQuery page, String salaryMonth) {
        Long employeeId = SecurityUtil.requireEmployeeId();
        LambdaQueryWrapper<Salary> wrapper = new LambdaQueryWrapper<Salary>()
                .eq(Salary::getEmployeeId, employeeId)
                .eq(Salary::getStatus, 1)
                .orderByDesc(Salary::getSalaryMonth);
        if (StringUtils.hasText(salaryMonth)) {
            wrapper.eq(Salary::getSalaryMonth, salaryMonth.trim());
        }
        IPage<Salary> iPage = salaryMapper.selectPage(
                new Page<>(page.getPageNum(), page.getPageSize()), wrapper);
        List<Salary> list = iPage.getRecords();
        if (list == null || list.isEmpty()) {
            return PageResult.empty();
        }
        Employee emp = employeeMapper.selectById(employeeId);
        Set<Long> positionIds = list.stream()
                .map(Salary::getPositionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> positionNames = positionIds.isEmpty()
                ? Map.of()
                : positionMapper.selectBatchIds(positionIds).stream()
                .collect(Collectors.toMap(Position::getId, Position::getPositionName, (a, b) -> a));

        List<SalaryVO> vos = list.stream().map(salary -> toSalaryVO(
                salary,
                emp != null ? emp.getName() : null,
                emp != null ? emp.getEmpNo() : null,
                salary.getPositionId() == null ? null : positionNames.get(salary.getPositionId())
        )).collect(Collectors.toList());

        PageResult<SalaryVO> result = new PageResult<>();
        result.setRecords(vos);
        result.setTotal(iPage.getTotal());
        result.setPageNum(iPage.getCurrent());
        result.setPageSize(iPage.getSize());
        result.setPages(iPage.getPages());
        return result;
    }

    @Override
    public List<SalaryVO> listMyPaidForExport(String salaryMonth) {
        Long employeeId = SecurityUtil.requireEmployeeId();
        LambdaQueryWrapper<Salary> wrapper = new LambdaQueryWrapper<Salary>()
                .eq(Salary::getEmployeeId, employeeId)
                .eq(Salary::getStatus, 1)
                .orderByDesc(Salary::getSalaryMonth);
        if (StringUtils.hasText(salaryMonth)) {
            wrapper.eq(Salary::getSalaryMonth, salaryMonth.trim());
        }
        List<Salary> list = salaryMapper.selectList(wrapper);
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        Employee emp = employeeMapper.selectById(employeeId);
        Set<Long> positionIds = list.stream()
                .map(Salary::getPositionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> positionNames = positionIds.isEmpty()
                ? Map.of()
                : positionMapper.selectBatchIds(positionIds).stream()
                .collect(Collectors.toMap(Position::getId, Position::getPositionName, (a, b) -> a));
        return list.stream().map(salary -> toSalaryVO(
                salary,
                emp != null ? emp.getName() : null,
                emp != null ? emp.getEmpNo() : null,
                salary.getPositionId() == null ? null : positionNames.get(salary.getPositionId())
        )).collect(Collectors.toList());
    }

    @Override
    public SalaryVO getMySalaryById(Long id) {
        Long employeeId = SecurityUtil.requireEmployeeId();
        SalaryVO vo = getSalaryById(id);
        if (!Objects.equals(vo.getEmployeeId(), employeeId)) {
            throw new BusinessException("无权查看该薪资条");
        }
        if (vo.getStatus() == null || vo.getStatus() != 1) {
            throw new BusinessException("仅可查看已发放的薪资条");
        }
        return vo;
    }

    @Override
    public SalaryPreviewVO previewGenerate(SalaryGenerateDTO dto) {
        Employee employee = requireEmployee(dto.getEmployeeId());
        YearMonth month = parseMonth(dto.getSalaryMonth());

        SalaryPreviewVO vo = new SalaryPreviewVO();
        vo.setEmployeeId(employee.getId());
        vo.setEmpNo(employee.getEmpNo());
        vo.setEmployeeName(employee.getName());
        vo.setSalaryMonth(month.format(MONTH_FMT));
        vo.setPositionId(employee.getPositionId());

        String tip = null;
        BigDecimal base = BigDecimal.ZERO;
        if (employee.getBaseSalary() != null) {
            base = employee.getBaseSalary();
            tip = "使用员工个人底薪（调薪生效）";
            if (employee.getPositionId() != null) {
                Position position = positionMapper.selectById(employee.getPositionId());
                vo.setPositionName(position == null ? null : position.getPositionName());
            }
        } else if (employee.getPositionId() == null) {
            tip = "员工未关联岗位，底薪按 0，请先维护岗位与底薪字典";
        } else {
            Position position = positionMapper.selectById(employee.getPositionId());
            vo.setPositionName(position == null ? null : position.getPositionName());
            SalaryBaseDict dict = salaryBaseDictMapper.selectOne(
                    new LambdaQueryWrapper<SalaryBaseDict>()
                            .eq(SalaryBaseDict::getPositionId, employee.getPositionId())
                            .eq(SalaryBaseDict::getStatus, 1)
                            .last("LIMIT 1"));
            if (dict == null || dict.getBaseSalary() == null) {
                tip = "该岗位未配置启用中的底薪字典，底薪按 0，请联系超级管理员在「字典管理-薪资字典」中维护";
            } else {
                base = dict.getBaseSalary();
            }
        }

        BigDecimal taskBonus = sumTaskBonus(employee.getId(), month);
        DeductCalc deductCalc = calcAttendanceDeduction(employee.getId(), month);
        BigDecimal hallDeduct = sumTaskHallDeduct(employee.getId(), month);
        BigDecimal totalDeduct = deductCalc.total.add(hallDeduct);
        String deductDetail = deductCalc.detail;
        if (hallDeduct.compareTo(BigDecimal.ZERO) > 0) {
            String hallPart = "任务大厅扣款 " + hallDeduct.toPlainString() + " 元";
            deductDetail = StringUtils.hasText(deductDetail) ? deductDetail + "；" + hallPart : hallPart;
        }

        vo.setBaseSalary(base);
        vo.setTaskBonus(taskBonus);
        vo.setBonus(taskBonus);
        vo.setDeduction(totalDeduct);
        vo.setDeductDetail(deductDetail);
        vo.setActualSalary(base.add(taskBonus).subtract(totalDeduct));
        vo.setTip(tip);
        return vo;
    }

    @Override
    public List<AttendanceDeductRuleVO> listDeductRules() {
        return deductRuleMapper.selectList(
                new LambdaQueryWrapper<AttendanceDeductRule>().orderByAsc(AttendanceDeductRule::getId)
        ).stream().map(this::toDeductRuleVO).toList();
    }

    @Override
    public void updateDeductRule(Long id, AttendanceDeductRuleUpdateDTO dto) {
        if (id == null) {
            throw new BusinessException("id不能为空");
        }
        AttendanceDeductRule existing = deductRuleMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("扣款规则不存在");
        }
        existing.setRuleCode(dto.getRuleCode());
        existing.setUnitAmount(dto.getUnitAmount());
        existing.setEnabled(dto.getEnabled());
        existing.setRemark(dto.getRemark());
        deductRuleMapper.updateById(existing);
    }

    private DeductCalc calcAttendanceDeduction(Long employeeId, YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();
        List<Attendance> records = attendanceMapper.selectList(
                new LambdaQueryWrapper<Attendance>()
                        .eq(Attendance::getEmployeeId, employeeId)
                        .ge(Attendance::getAttendDate, start)
                        .le(Attendance::getAttendDate, end)
        );

        Map<String, AttendanceDeductRule> rules = deductRuleMapper.selectList(
                new LambdaQueryWrapper<AttendanceDeductRule>().eq(AttendanceDeductRule::getEnabled, 1)
        ).stream().collect(Collectors.toMap(AttendanceDeductRule::getRuleCode, r -> r, (a, b) -> a));

        long lateDays = records.stream().filter(a -> Objects.equals(a.getStatus(), STATUS_LATE)).count();
        long absentDays = records.stream().filter(a -> Objects.equals(a.getStatus(), STATUS_ABSENT)).count();
        long missingDays = records.stream().filter(a -> {
            Integer status = a.getStatus();
            if (Objects.equals(status, STATUS_LEAVE) || Objects.equals(status, STATUS_FIELD)) {
                return false;
            }
            return a.getCheckIn() == null || a.getCheckOut() == null;
        }).count();

        BigDecimal total = BigDecimal.ZERO;
        List<String> parts = new ArrayList<>();

        AttendanceDeductRule lateRule = rules.get("LATE");
        if (lateRule != null && lateDays > 0) {
            BigDecimal amt = nz(lateRule.getUnitAmount()).multiply(BigDecimal.valueOf(lateDays));
            total = total.add(amt);
            parts.add("迟到" + lateDays + "天×" + lateRule.getUnitAmount() + "=" + amt);
        }
        AttendanceDeductRule absentRule = rules.get("ABSENT");
        if (absentRule != null && absentDays > 0) {
            BigDecimal amt = nz(absentRule.getUnitAmount()).multiply(BigDecimal.valueOf(absentDays));
            total = total.add(amt);
            parts.add("缺勤" + absentDays + "天×" + absentRule.getUnitAmount() + "=" + amt);
        }
        AttendanceDeductRule missingRule = rules.get("MISSING_CHECK");
        if (missingRule != null && missingDays > 0) {
            BigDecimal amt = nz(missingRule.getUnitAmount()).multiply(BigDecimal.valueOf(missingDays));
            total = total.add(amt);
            parts.add("缺卡" + missingDays + "天×" + missingRule.getUnitAmount() + "=" + amt);
        }

        DeductCalc calc = new DeductCalc();
        calc.total = total;
        calc.detail = parts.isEmpty() ? "无自动扣款" : String.join("；", parts);
        return calc;
    }

    private AttendanceDeductRuleVO toDeductRuleVO(AttendanceDeductRule rule) {
        AttendanceDeductRuleVO vo = new AttendanceDeductRuleVO();
        vo.setId(rule.getId());
        vo.setRuleCode(rule.getRuleCode());
        vo.setUnitAmount(rule.getUnitAmount());
        vo.setEnabled(rule.getEnabled());
        vo.setRemark(rule.getRemark());
        return vo;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static class DeductCalc {
        private BigDecimal total = BigDecimal.ZERO;
        private String detail;
    }

    @Override
    public void createSalary(SalaryCreateDTO dto) {
        Employee employee = requireEmployee(dto.getEmployeeId());
        assertUniqueMonth(dto.getEmployeeId(), dto.getSalaryMonth(), null);

        BigDecimal base = dto.getBaseSalary();
        BigDecimal taskBonus = dto.getTaskBonus() != null ? dto.getTaskBonus() : BigDecimal.ZERO;
        BigDecimal bonus = dto.getBonus() != null ? dto.getBonus() : taskBonus;
        BigDecimal deduction = dto.getDeduction() != null ? dto.getDeduction() : BigDecimal.ZERO;

        Salary salary = new Salary();
        salary.setEmployeeId(dto.getEmployeeId());
        salary.setSalaryMonth(dto.getSalaryMonth());
        salary.setPositionId(dto.getPositionId() != null ? dto.getPositionId() : employee.getPositionId());
        salary.setBaseSalary(base);
        salary.setTaskBonus(taskBonus);
        salary.setBonus(bonus);
        salary.setDeduction(deduction);
        salary.setActualSalary(base.add(bonus).subtract(deduction));
        salary.setStatus(0);
        salary.setRemark(dto.getRemark());
        salaryMapper.insert(salary);
    }

    @Override
    public void updateSalary(Long id, SalaryUpdateDTO dto) {
        if (id == null) {
            throw new BusinessException("id不为空");
        }
        Salary existing = salaryMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("薪资记录不存在");
        }
        if (existing.getStatus() != null && existing.getStatus() == 1) {
            throw new BusinessException("已发放，无法修改");
        }
        requireEmployee(dto.getEmployeeId());
        assertUniqueMonth(dto.getEmployeeId(), dto.getSalaryMonth(), id);

        BigDecimal bonus = dto.getBonus() != null ? dto.getBonus() : BigDecimal.ZERO;
        BigDecimal deduction = dto.getDeduction() != null ? dto.getDeduction() : BigDecimal.ZERO;
        existing.setEmployeeId(dto.getEmployeeId());
        existing.setSalaryMonth(dto.getSalaryMonth());
        existing.setBaseSalary(dto.getBaseSalary());
        existing.setBonus(bonus);
        existing.setDeduction(deduction);
        existing.setActualSalary(dto.getBaseSalary().add(bonus).subtract(deduction));
        existing.setRemark(dto.getRemark());
        existing.setStatus(0);
        salaryMapper.updateById(existing);
    }

    @Override
    public void paySalary(Long id, LocalDate payDate) {
        if (id == null) {
            throw new BusinessException("id不能为空");
        }
        Salary salary = salaryMapper.selectById(id);
        if (salary == null) {
            throw new BusinessException("薪资不存在");
        }
        if (salary.getStatus() != null && salary.getStatus() == 1) {
            throw new BusinessException("该薪资已发放，请勿重复操作");
        }
        if (salary.getStatus() == null || salary.getStatus() != 0) {
            throw new BusinessException("当前状态无法发放");
        }
        salary.setStatus(1);
        salary.setPayDate(payDate != null ? payDate : LocalDate.now());
        salaryMapper.updateById(salary);
    }

    @Override
    public void deleteSalary(Long id) {
        if (id == null) {
            throw new BusinessException("id不能为空");
        }
        Salary salary = salaryMapper.selectById(id);
        if (salary == null) {
            throw new BusinessException("薪资记录不存在");
        }
        if (salary.getStatus() == null || salary.getStatus() != 0) {
            throw new BusinessException("仅待发放状态的薪资可删除");
        }
        salaryMapper.deleteById(id);
    }

    private BigDecimal sumTaskBonus(Long employeeId, YearMonth month) {
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.atEndOfMonth().atTime(LocalTime.MAX);
        List<TaskAssignee> list = taskAssigneeMapper.selectList(
                new LambdaQueryWrapper<TaskAssignee>()
                        .eq(TaskAssignee::getEmployeeId, employeeId)
                        .eq(TaskAssignee::getStatus, 2)
                        .isNotNull(TaskAssignee::getScoreBonus)
                        .ge(TaskAssignee::getFinishTime, start)
                        .le(TaskAssignee::getFinishTime, end));
        return list.stream()
                .map(TaskAssignee::getScoreBonus)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumTaskHallDeduct(Long employeeId, YearMonth month) {
        String deductMonth = month.format(MONTH_FMT);
        List<TaskHallDeduct> list = taskHallDeductMapper.selectList(
                new LambdaQueryWrapper<TaskHallDeduct>()
                        .eq(TaskHallDeduct::getEmployeeId, employeeId)
                        .eq(TaskHallDeduct::getDeductMonth, deductMonth));
        return list.stream()
                .map(TaskHallDeduct::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Employee requireEmployee(Long employeeId) {
        Employee employee = employeeMapper.selectById(employeeId);
        if (employee == null) {
            throw new BusinessException("员工不存在");
        }
        return employee;
    }

    private void assertUniqueMonth(Long employeeId, String salaryMonth, Long excludeId) {
        LambdaQueryWrapper<Salary> wrapper = new LambdaQueryWrapper<Salary>()
                .eq(Salary::getEmployeeId, employeeId)
                .eq(Salary::getSalaryMonth, salaryMonth);
        if (excludeId != null) {
            wrapper.ne(Salary::getId, excludeId);
        }
        Long count = salaryMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException("该员工该月份已有薪资记录");
        }
    }

    private YearMonth parseMonth(String salaryMonth) {
        if (!StringUtils.hasText(salaryMonth)) {
            throw new BusinessException("薪资月份不能为空");
        }
        try {
            return YearMonth.parse(salaryMonth.trim(), MONTH_FMT);
        } catch (DateTimeParseException e) {
            throw new BusinessException("薪资月份格式应为 yyyy-MM");
        }
    }

    private SalaryVO toSalaryVO(Salary salary, String employeeName, String empNo, String positionName) {
        SalaryVO salaryVO = new SalaryVO();
        salaryVO.setId(salary.getId());
        salaryVO.setEmployeeId(salary.getEmployeeId());
        salaryVO.setBaseSalary(salary.getBaseSalary());
        salaryVO.setTaskBonus(salary.getTaskBonus());
        salaryVO.setActualSalary(salary.getActualSalary());
        salaryVO.setSalaryMonth(salary.getSalaryMonth());
        salaryVO.setPositionId(salary.getPositionId());
        salaryVO.setPositionName(positionName);
        salaryVO.setEmployeeName(employeeName);
        salaryVO.setEmpNo(empNo);
        salaryVO.setStatus(salary.getStatus());
        salaryVO.setPayDate(salary.getPayDate());
        salaryVO.setRemark(salary.getRemark());
        salaryVO.setBonus(salary.getBonus());
        salaryVO.setDeduction(salary.getDeduction());
        return salaryVO;
    }
}
