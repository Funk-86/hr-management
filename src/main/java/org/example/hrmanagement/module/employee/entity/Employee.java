package org.example.hrmanagement.module.employee.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.hrmanagement.common.entity.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hr_employee")
public class Employee extends BaseEntity {

    /** 工号 */
    private String empNo;

    /** 姓名 */
    private String name;

    /** 性别：1-男 2-女 */
    private Integer gender;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 身份证号 */
    private String idCard;

    /** 所属部门ID */
    private Long deptId;

    /** 岗位ID */
    private Long positionId;

    /** 个人底薪（调薪生效后覆盖岗位字典） */
    private BigDecimal baseSalary;

    /** 入职日期 */
    private LocalDate hireDate;

    /** 试用期结束日期 */
    private LocalDate probationEnd;

    /** 用工类型：1-全职 2-兼职 3-实习 */
    private Integer employmentType;

    /** 状态：1-在职 2-试用期 3-离职 */
    private Integer status;

    /** 离职日期 */
    private LocalDate leaveDate;

    /** 头像URL */
    private String avatar;

    /** 备注 */
    private String remark;
}
