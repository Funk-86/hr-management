-- V22: 修复权限字典名称乱码（双重 UTF-8 编码）
-- 根因：早期 Flyway/客户端以错误字符集写入；V18+ 新增项编码正确，旧项显示为乱码。
-- 策略：按 id 强制写回正确中文（幂等）；并对仍呈双重编码特征的行做一次解码兜底。

UPDATE sys_permission SET perm_name = '工作台' WHERE id = 1;
UPDATE sys_permission SET perm_name = '组织人事' WHERE id = 2;
UPDATE sys_permission SET perm_name = '考勤假期' WHERE id = 3;
UPDATE sys_permission SET perm_name = '项目协作' WHERE id = 4;
UPDATE sys_permission SET perm_name = '薪酬绩效' WHERE id = 5;
UPDATE sys_permission SET perm_name = '数据分析' WHERE id = 6;
UPDATE sys_permission SET perm_name = '字典管理' WHERE id = 7;
UPDATE sys_permission SET perm_name = '系统审计' WHERE id = 8;

UPDATE sys_permission SET perm_name = '工作台首页' WHERE id = 10;

UPDATE sys_permission SET perm_name = '部门管理' WHERE id = 20;
UPDATE sys_permission SET perm_name = '岗位管理' WHERE id = 21;
UPDATE sys_permission SET perm_name = '员工管理' WHERE id = 22;
UPDATE sys_permission SET perm_name = '入转调离' WHERE id = 23;
UPDATE sys_permission SET perm_name = '文档管理' WHERE id = 24;

UPDATE sys_permission SET perm_name = '考勤管理' WHERE id = 30;
UPDATE sys_permission SET perm_name = '请假管理' WHERE id = 31;

UPDATE sys_permission SET perm_name = '项目管理' WHERE id = 40;
UPDATE sys_permission SET perm_name = '任务管理' WHERE id = 41;

UPDATE sys_permission SET perm_name = '薪资管理' WHERE id = 50;
UPDATE sys_permission SET perm_name = '绩效考核' WHERE id = 51;
UPDATE sys_permission SET perm_name = '我的薪资条' WHERE id = 52;
UPDATE sys_permission SET perm_name = '任务大厅' WHERE id = 53;

UPDATE sys_permission SET perm_name = '统计看板' WHERE id = 60;

UPDATE sys_permission SET perm_name = '薪资字典' WHERE id = 70;
UPDATE sys_permission SET perm_name = '用户权限字典' WHERE id = 71;
UPDATE sys_permission SET perm_name = '页面字典' WHERE id = 72;

UPDATE sys_permission SET perm_name = '操作日志' WHERE id = 80;

UPDATE sys_permission SET perm_name = '员工能力' WHERE id = 100;
UPDATE sys_permission SET perm_name = '本人考勤' WHERE id = 101;
UPDATE sys_permission SET perm_name = '请假申请' WHERE id = 102;
UPDATE sys_permission SET perm_name = '任务执行' WHERE id = 103;
UPDATE sys_permission SET perm_name = '本人文档' WHERE id = 104;
UPDATE sys_permission SET perm_name = '查看绩效' WHERE id = 105;
UPDATE sys_permission SET perm_name = '异动申请' WHERE id = 106;
UPDATE sys_permission SET perm_name = '参与项目' WHERE id = 107;
UPDATE sys_permission SET perm_name = '查看本人薪资条' WHERE id = 108;
UPDATE sys_permission SET perm_name = '大厅接取' WHERE id = 109;

UPDATE sys_permission SET perm_name = '经理能力' WHERE id = 200;
UPDATE sys_permission SET perm_name = '员工管理' WHERE id = 201;
UPDATE sys_permission SET perm_name = '请假审批' WHERE id = 202;
UPDATE sys_permission SET perm_name = '任务下发' WHERE id = 203;
UPDATE sys_permission SET perm_name = '项目管理' WHERE id = 204;
UPDATE sys_permission SET perm_name = '绩效评分' WHERE id = 205;
UPDATE sys_permission SET perm_name = '异动审批' WHERE id = 206;
UPDATE sys_permission SET perm_name = '考勤进阶审批' WHERE id = 207;
UPDATE sys_permission SET perm_name = '大厅发布' WHERE id = 208;

UPDATE sys_permission SET perm_name = 'HR能力' WHERE id = 300;
UPDATE sys_permission SET perm_name = '组织维护' WHERE id = 301;
UPDATE sys_permission SET perm_name = '薪资管理' WHERE id = 302;
UPDATE sys_permission SET perm_name = '统计看板' WHERE id = 303;
UPDATE sys_permission SET perm_name = '操作审计' WHERE id = 304;
UPDATE sys_permission SET perm_name = '异动生效' WHERE id = 305;
UPDATE sys_permission SET perm_name = '假期余额管理' WHERE id = 306;

UPDATE sys_permission SET perm_name = '超管能力' WHERE id = 400;
UPDATE sys_permission SET perm_name = '字典管理' WHERE id = 401;
UPDATE sys_permission SET perm_name = '权限配置' WHERE id = 402;

-- 兜底：首字节为 C2/C3 的双重编码中文名（不伤及已是合法 UTF-8 的 E4–EF 行）
UPDATE sys_permission
SET perm_name = CONVERT(CAST(CONVERT(perm_name USING latin1) AS BINARY) USING utf8mb4)
WHERE HEX(LEFT(perm_name, 1)) IN ('C2', 'C3');