package org.example.hrmanagement.module.performance.service;

import org.example.hrmanagement.common.result.PageResult;
import org.example.hrmanagement.module.performance.dto.PerformanceQueryDTO;
import org.example.hrmanagement.module.performance.dto.PerformanceSaveDTO;
import org.example.hrmanagement.module.performance.vo.PerformanceReviewVO;
import org.example.hrmanagement.module.performance.vo.PerformanceTaskHintVO;

import java.util.List;

public interface PerformanceService {

    PageResult<PerformanceReviewVO> page(PerformanceQueryDTO query);

    PerformanceReviewVO getDetail(Long id);

    /** 员工档案：最近考核记录 */
    List<PerformanceReviewVO> listByEmployee(Long employeeId, Integer limit);

    PerformanceTaskHintVO taskHint(Long employeeId, Integer periodType, String periodKey);

    Long create(PerformanceSaveDTO dto);

    void update(Long id, PerformanceSaveDTO dto);

    void submit(Long id);

    void confirm(Long id);

    void delete(Long id);
}
