package org.example.hrmanagement.module.employee.service;

import org.example.hrmanagement.module.employee.dto.FaceEnrollDTO;
import org.example.hrmanagement.module.employee.vo.FaceStatusVO;

import java.util.List;

public interface FaceService {
    void enroll(Long employeeId, FaceEnrollDTO dto);
    FaceStatusVO getStatus(Long employeeId);
    void verifyEmployeeFace(Long employeeId, List<Double> liveDescriptor);
    Long matchEmployee(List<Double> liveDescriptor);  // 可选：超管 1:N
}
