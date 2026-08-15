package org.example.hrmanagement.module.salary.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.hrmanagement.common.dto.PageQuery;
import org.example.hrmanagement.common.exception.BusinessException;
import org.example.hrmanagement.common.result.PageResult;
import org.example.hrmanagement.module.employee.entity.Employee;
import org.example.hrmanagement.module.employee.mapper.EmployeeMapper;
import org.example.hrmanagement.module.position.entity.Position;
import org.example.hrmanagement.module.position.mapper.PositionMapper;
import org.example.hrmanagement.module.salary.dto.SalaryCreateDTO;
import org.example.hrmanagement.module.salary.dto.SalaryGenerateDTO;
import org.example.hrmanagement.module.salary.dto.SalaryUpdateDTO;
import org.example.hrmanagement.module.salary.entity.Salary;
import org.example.hrmanagement.module.salary.entity.SalaryBaseDict;
import org.example.hrmanagement.module.salary.mapper.SalaryBaseDictMapper;
import org.example.hrmanagement.module.salary.mapper.SalaryMapper;
import org.example.hrmanagement.module.salary.service.SalaryService;
import org.example.hrmanagement.module.salary.vo.SalaryPreviewVO;
import org.example.hrmanagement.module.salary.vo.SalaryVO;
import org.example.hrmanagement.module.task.entity.TaskAssignee;
import org.example.hrmanagement.module.task.mapper.TaskAssigneeMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalaryServiceImpl implements SalaryService {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final SalaryMapper salaryMapper;
    private final EmployeeMapper employeeMapper;
    private final PositionMapper positionMapper;
    private final SalaryBaseDictMapper salaryBaseDictMapper;
    private final TaskAssigneeMapper taskAssigneeMapper;

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
    public SalaryPreviewVO previewGenerate(SalaryGenerateDTO dto) {
        Employee employee = requireEmployee(dto.getEmployeeId());
        YearMonth month = parseMonth(dto.getSalaryMonth());

        SalaryPreviewVO vo = new SalaryPreviewVO();
        vo.setEmployeeId(employee.getId());
        vo.setEmpNo(employee.getEmpNo());
        vo.setEmployeeName(employee.getName());
        vo.setSalaryMonth(month.format(MONTH_FMT));
        vo.setPositionId(employee.getPositionId());
        vo.setDeduction(BigDecimal.ZERO);

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
        vo.setBaseSalary(base);
        vo.setTaskBonus(taskBonus);
        vo.setBonus(taskBonus);
        vo.setActualSalary(base.add(taskBonus));
        vo.setTip(tip);
        return vo;
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
