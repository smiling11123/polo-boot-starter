insert into demo_record (id, title, content, status, version, tenant_id, dept_id, create_by, update_by, create_time, update_time)
values
    (1, '总部概览', 'tenant1001-dept10-admin', 'APPROVED', 1, 1001, 10, 1, 1, timestamp '2026-03-01 09:00:00', timestamp '2026-03-01 09:00:00'),
    (2, '经理待办', 'tenant1001-dept20-manager', 'DRAFT', 1, 1001, 20, 2, 2, timestamp '2026-03-02 10:00:00', timestamp '2026-03-02 10:00:00'),
    (3, '子部门已审', 'tenant1001-dept21-user', 'APPROVED', 1, 1001, 21, 4, 4, timestamp '2026-03-03 11:00:00', timestamp '2026-03-03 11:00:00'),
    (4, '子部门草稿', 'tenant1001-dept21-user', 'DRAFT', 1, 1001, 21, 4, 4, timestamp '2026-03-04 12:00:00', timestamp '2026-03-04 12:00:00'),
    (5, '审计归档', 'tenant1001-dept30-auditor', 'APPROVED', 1, 1001, 30, 3, 3, timestamp '2026-03-05 13:00:00', timestamp '2026-03-05 13:00:00'),
    (6, '其他租户数据', 'tenant1002-hidden', 'APPROVED', 1, 1002, 40, 9, 9, timestamp '2026-03-06 14:00:00', timestamp '2026-03-06 14:00:00');

alter table demo_record alter column id restart with 100;
