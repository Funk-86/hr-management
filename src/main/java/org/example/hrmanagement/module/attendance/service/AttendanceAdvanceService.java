package org.example.hrmanagement.module.attendance.service;

import org.example.hrmanagement.common.dto.PageQuery;
import org.example.hrmanagement.common.result.PageResult;
import org.example.hrmanagement.module.attendance.dto.AttendanceAppealCreateDTO;
import org.example.hrmanagement.module.attendance.dto.FieldWorkCreateDTO;
import org.example.hrmanagement.module.attendance.dto.OvertimeCreateDTO;
import org.example.hrmanagement.module.attendance.vo.AttendanceAppealVO;
import org.example.hrmanagement.module.attendance.vo.FieldWorkRequestVO;
import org.example.hrmanagement.module.attendance.vo.OvertimeRequestVO;

public interface AttendanceAdvanceService {
    void createOvertime(OvertimeCreateDTO dto);
    PageResult<OvertimeRequestVO> listOvertime(PageQuery page);
    void approveOvertime(Long id, String remark);
    void rejectOvertime(Long id, String remark);
    void cancelOvertime(Long id);

    void createAppeal(AttendanceAppealCreateDTO dto);
    PageResult<AttendanceAppealVO> listAppeals(PageQuery page);
    void approveAppeal(Long id, String remark);
    void rejectAppeal(Long id, String remark);
    void cancelAppeal(Long id);

    void createFieldWork(FieldWorkCreateDTO dto);
    PageResult<FieldWorkRequestVO> listFieldWork(PageQuery page);
    void approveFieldWork(Long id, String remark);
    void rejectFieldWork(Long id, String remark);
    void cancelFieldWork(Long id);
}
