package org.example.hrmanagement.module.attendance.service;

import org.example.hrmanagement.common.dto.PageQuery;
import org.example.hrmanagement.common.result.PageResult;
import org.example.hrmanagement.module.attendance.dto.AttendanceCheckDTO;
import org.example.hrmanagement.module.attendance.dto.FaceCheckDTO;
import org.example.hrmanagement.module.attendance.vo.AttendanceVO;

import java.util.List;

public interface AttendanceService {
    void checkIn(AttendanceCheckDTO dto);
    void checkOut(AttendanceCheckDTO dto);
    PageResult<AttendanceVO> getAll(PageQuery page);

    /** 导出用列表（复用数据权限，最多 5000 条） */
    List<AttendanceVO> listForExport();

    AttendanceVO getOne(Long id);
    void create(AttendanceVO vo);
    void update(Long id,AttendanceVO vo);
    void delete(Long id);
    Long resolveCheckEmployeeId(Long requestedEmployeeId);
    void checkInByFace(FaceCheckDTO dto);
    void checkOutByFace(FaceCheckDTO dto);
}
