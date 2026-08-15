package org.example.hrmanagement.module.leave.service;

import org.example.hrmanagement.common.dto.PageQuery;
import org.example.hrmanagement.common.result.PageResult;
import org.example.hrmanagement.module.leave.dto.LeaveRequestCreateDTO;
import org.example.hrmanagement.module.leave.dto.LeaveTypeCreateDTO;
import org.example.hrmanagement.module.leave.dto.LeaveTypeUpdateDTO;
import org.example.hrmanagement.module.leave.vo.LeaveRequestVO;
import org.example.hrmanagement.module.leave.vo.LeaveTypeVO;

import java.util.List;

public interface LeaveService {
    List<LeaveTypeVO> getLeaveType();
    void insertLeaveType(LeaveTypeCreateDTO dto);
    void updateLeaveType(Long id,LeaveTypeUpdateDTO dto);
    void deleteLeaveType(Long id);
    void requestLeave(LeaveRequestCreateDTO dto);
    LeaveRequestVO getLeaveRequestById(Long id);
    void approveLeave(Long id,String approveRemark);
    void rejectLeave(Long id,String approveRemark);
    void cancelLeave(Long id,String approveRemark);
    PageResult<LeaveRequestVO> getLeaveRequest(PageQuery page);
}
