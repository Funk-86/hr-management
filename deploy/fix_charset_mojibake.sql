-- 修复「UTF-8 被当成 latin1 写入」导致的中文乱码
-- 先备份：演示库可直接执行；执行后刷新页面

UPDATE hr_employee SET
  name = CONVERT(BINARY CONVERT(name USING latin1) USING utf8mb4),
  remark = IF(remark IS NULL, NULL, CONVERT(BINARY CONVERT(remark USING latin1) USING utf8mb4));

UPDATE sys_department SET
  dept_name = CONVERT(BINARY CONVERT(dept_name USING latin1) USING utf8mb4);

UPDATE sys_position SET
  position_name = CONVERT(BINARY CONVERT(position_name USING latin1) USING utf8mb4);

UPDATE sys_role SET
  role_name = CONVERT(BINARY CONVERT(role_name USING latin1) USING utf8mb4),
  description = IF(description IS NULL, NULL, CONVERT(BINARY CONVERT(description USING latin1) USING utf8mb4));

UPDATE hr_leave_type SET
  type_name = CONVERT(BINARY CONVERT(type_name USING latin1) USING utf8mb4);

UPDATE hr_salary_base_dict SET
  remark = IF(remark IS NULL, NULL, CONVERT(BINARY CONVERT(remark USING latin1) USING utf8mb4));

UPDATE hr_task_score_bonus_dict SET
  grade_label = CONVERT(BINARY CONVERT(grade_label USING latin1) USING utf8mb4);
