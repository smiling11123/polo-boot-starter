package com.polo.boot.mybatis.plus.service;

import java.util.Set;

public interface DeptHierarchyProvider {
    /**
     * 子部门解析
     * @param deptId 父部门id
     * @return 返回子部门id 集合 id为 Long 类型
     */
    Set<Long> resolveDeptAndChildren(Long deptId);
}
