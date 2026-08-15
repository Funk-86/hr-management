package org.example.hrmanagement.module.employee.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.example.hrmanagement.common.constant.FaceConstants;
import org.example.hrmanagement.common.exception.BusinessException;
import org.example.hrmanagement.common.util.SecurityUtil;
import org.example.hrmanagement.module.employee.dto.FaceEnrollDTO;
import org.example.hrmanagement.module.employee.entity.Employee;
import org.example.hrmanagement.module.employee.entity.EmployeeFace;
import org.example.hrmanagement.module.employee.mapper.EmployeeFaceMapper;
import org.example.hrmanagement.module.employee.mapper.EmployeeMapper;
import org.example.hrmanagement.module.employee.service.FaceService;
import org.example.hrmanagement.module.employee.vo.FaceStatusVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FaceServiceImpl implements FaceService {
    @Autowired
    private EmployeeMapper employeeMapper;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private EmployeeFaceMapper employeeFaceMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enroll(Long employeeId, FaceEnrollDTO dto) {
        if (employeeId == null) {
            throw new BusinessException("员工 ID 不能为空");
        }
        Employee employee = employeeMapper.selectById(employeeId);
        if (employee == null) {
            throw new BusinessException("员工不存在");
        }
        assertCanManageEmployee(employee);
        validateDescriptor(dto.getDescriptor());
        int sampleCount = dto.getSampleCount() != null && dto.getSampleCount() > 0
                ? dto.getSampleCount()
                : 1;
        String descriptorJson = toDescriptorJson(dto.getDescriptor());
        Long enrolledBy = SecurityUtil.getUserId();
        LocalDateTime now = LocalDateTime.now();
        String modelVersion = FaceConstants.MODEL_VERSION;
        EmployeeFace existing = employeeFaceMapper.selectOne(
                new LambdaQueryWrapper<EmployeeFace>()
                        .eq(EmployeeFace::getEmployeeId, employeeId)
        );
        if (existing != null) {
            employeeFaceMapper.update(null, new LambdaUpdateWrapper<EmployeeFace>()
                    .eq(EmployeeFace::getEmployeeId, employeeId)
                    .set(EmployeeFace::getDescriptor, descriptorJson)
                    .set(EmployeeFace::getSampleCount, sampleCount)
                    .set(EmployeeFace::getEnrolledBy, enrolledBy)
                    .set(EmployeeFace::getEnrolledAt, now)
                    .set(EmployeeFace::getModelVersion, modelVersion)
                    .set(EmployeeFace::getStatus, 1)
                    .set(EmployeeFace::getUpdatedAt, now)
            );
            return;
        }
        EmployeeFace face = new EmployeeFace();
        face.setEmployeeId(employeeId);
        face.setDescriptor(descriptorJson);
        face.setSampleCount(sampleCount);
        face.setEnrolledBy(enrolledBy);
        face.setEnrolledAt(now);
        face.setModelVersion(modelVersion);
        face.setStatus(1);
        employeeFaceMapper.insert(face);
    }

    private String toDescriptorJson(List<Double> descriptor) {
        try {
            return objectMapper.writeValueAsString(descriptor);
        } catch (Exception e) {
            throw new BusinessException("人脸特征序列化失败");
        }
    }

    private void validateDescriptor(List<Double> descriptor) {
        if (descriptor == null || descriptor.isEmpty()) {
            throw new BusinessException("人脸特征不能为空");
        }
        if (descriptor.size() != FaceConstants.DESCRIPTOR_SIZE) {
            throw new BusinessException("人脸特征维度必须为 128");
        }
        for (Double value : descriptor) {
            if (value == null) {
                throw new BusinessException("人脸特征存在空值");
            }
            double v = value;
            if (Double.isNaN(v) || Double.isInfinite(v)) {
                throw new BusinessException("人脸特征数据非法");
            }
        }
    }

    private void assertCanManageEmployee(Employee employee) {
        if (SecurityUtil.isHrStaff()) {
            return;
        }
        if (SecurityUtil.hasRole("DEPT_MANAGER")) {
            Long deptId = SecurityUtil.requireDeptId();
            if (employee.getDeptId() != null && employee.getDeptId().equals(deptId)) {
                return;
            }
        }
        Long myEmployeeId = SecurityUtil.getEmployeeId();
        if (myEmployeeId != null && myEmployeeId.equals(employee.getId())) {
            return;
        }
        throw new BusinessException("无权录入该员工人脸");
    }

    private float[] parseDescriptor(String json) {
        try {
            List<Double> list = objectMapper.readValue(
                    json,
                    new TypeReference<List<Double>>() {}
            );
            if (list.size() != FaceConstants.DESCRIPTOR_SIZE) {
                throw new BusinessException("底库人脸数据损坏");
            }
            float[] arr = new float[FaceConstants.DESCRIPTOR_SIZE];
            for (int i = 0; i < FaceConstants.DESCRIPTOR_SIZE; i++) {
                arr[i] = list.get(i).floatValue();
            }
            return arr;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("底库人脸数据解析失败");
        }
    }

    @Override
    public FaceStatusVO getStatus(Long employeeId) {
        if (employeeId == null) {
            throw new BusinessException("员工 ID 不能为空");
        }
        Employee employee = employeeMapper.selectById(employeeId);
        if (employee == null) {
            throw new BusinessException("员工不存在");
        }
        assertCanManageEmployee(employee);
        EmployeeFace employeeFace = employeeFaceMapper.selectOne(
                new LambdaQueryWrapper<EmployeeFace>()
                        .eq(EmployeeFace::getEmployeeId, employeeId)
                        .eq(EmployeeFace::getStatus, 1)
        );
        FaceStatusVO vo = new FaceStatusVO();
        if (employeeFace == null) {
            vo.setEnrolled(false);
            return vo;
        }
        vo.setEnrolled(true);
        vo.setEnrolledAt(employeeFace.getEnrolledAt());
        vo.setModelVersion(employeeFace.getModelVersion());
        return vo;
    }

    @Override
    public void verifyEmployeeFace(Long employeeId, List<Double> liveDescriptor) {
        if (employeeId == null) {
            throw new BusinessException("员工 ID 不能为空");
        }
        validateDescriptor(liveDescriptor);
        Employee employee = employeeMapper.selectById(employeeId);
        if (employee == null) {
            throw new BusinessException("员工不存在");
        }
        EmployeeFace face = employeeFaceMapper.selectOne(
                new LambdaQueryWrapper<EmployeeFace>()
                        .eq(EmployeeFace::getEmployeeId, employeeId)
                        .eq(EmployeeFace::getStatus, 1)
        );
        if (face == null) {
            throw new BusinessException("请先录入人脸");
        }
        float[] stored = parseDescriptor(face.getDescriptor());
        float[] live = toFloatArray(liveDescriptor);
        double distance = euclideanDistance(stored, live);
        if (distance >= FaceConstants.THRESHOLD) {
            throw new BusinessException("人脸识别失败，请重试");
        }
    }

    private double euclideanDistance(float[] a, float[] b) {
        double sum = 0;
        for (int i = 0; i < FaceConstants.DESCRIPTOR_SIZE; i++) {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    private float[] toFloatArray(List<Double> descriptor) {
        float[] arr = new float[FaceConstants.DESCRIPTOR_SIZE];
        for (int i = 0; i < FaceConstants.DESCRIPTOR_SIZE; i++) {
            arr[i] = descriptor.get(i).floatValue();
        }
        return arr;
    }

    @Override
    public Long matchEmployee(List<Double> liveDescriptor) {
        throw new BusinessException("1:N 人脸匹配尚未开放");
    }
}
