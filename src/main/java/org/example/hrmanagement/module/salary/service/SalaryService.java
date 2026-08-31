package org.example.hrmanagement.module.salary.service;

import org.example.hrmanagement.common.dto.PageQuery;
import org.example.hrmanagement.common.result.PageResult;
import org.example.hrmanagement.module.salary.dto.AttendanceDeductRuleUpdateDTO;
import org.example.hrmanagement.module.salary.dto.SalaryCreateDTO;
import org.example.hrmanagement.module.salary.dto.SalaryGenerateDTO;
import org.example.hrmanagement.module.salary.dto.SalaryUpdateDTO;
import org.example.hrmanagement.module.salary.vo.AttendanceDeductRuleVO;
import org.example.hrmanagement.module.salary.vo.SalaryPreviewVO;
import org.example.hrmanagement.module.salary.vo.SalaryVO;

import java.time.LocalDate;
import java.util.List;

public interface SalaryService {
    PageResult<SalaryVO> getSalaryAll(PageQuery page);

    SalaryVO getSalaryById(Long id);

    PageResult<SalaryVO> getMyPaidSalaries(PageQuery page, String salaryMonth);

    SalaryVO getMySalaryById(Long id);

    /** 按员工+月份预计算底薪与任务奖金 */
    SalaryPreviewVO previewGenerate(SalaryGenerateDTO dto);

    void createSalary(SalaryCreateDTO dto);

    void updateSalary(Long id, SalaryUpdateDTO dto);

    void paySalary(Long id, LocalDate payDate);

    void deleteSalary(Long id);

    List<AttendanceDeductRuleVO> listDeductRules();

    void updateDeductRule(Long id, AttendanceDeductRuleUpdateDTO dto);
}
