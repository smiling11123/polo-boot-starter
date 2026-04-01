package com.polo.boot.mybatis.plus.service;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.polo.boot.core.context.CurrentPrincipal;
import com.polo.boot.core.context.CurrentPrincipalProvider;
import com.polo.boot.core.context.SecurityContextFacade;
import com.polo.boot.mybatis.plus.annotation.DataScope;
import com.polo.boot.mybatis.plus.config.DataScopeProperties;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DataScopeInnerInterceptor implements InnerInterceptor {
    private static final Field BOUND_SQL_FIELD = locateSqlField();
    private static final String DENY_ALL_CONDITION = "1 = 0";
    private static final List<String> TAIL_CLAUSES = List.of(" group by ", " having ", " order by ", " limit ", " for update");

    private final DataScopeProperties dataScopeProperties;
    private final DeptHierarchyProvider deptHierarchyProvider;
    private final CurrentPrincipalProvider currentPrincipalProvider;
    private final SecurityContextFacade securityContextFacade;
    private final Map<String, Optional<DataScope>> annotationCache = new ConcurrentHashMap<>();

    public DataScopeInnerInterceptor(DataScopeProperties dataScopeProperties,
                                     DeptHierarchyProvider deptHierarchyProvider,
                                     CurrentPrincipalProvider currentPrincipalProvider,
                                     SecurityContextFacade securityContextFacade) {
        this.dataScopeProperties = dataScopeProperties;
        this.deptHierarchyProvider = deptHierarchyProvider;
        this.currentPrincipalProvider = currentPrincipalProvider;
        this.securityContextFacade = securityContextFacade;
    }

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms,
                            Object parameter, RowBounds rowBounds,
                            ResultHandler resultHandler, BoundSql boundSql) {
        DataScopeContext.ScopeDefinition dataScope = resolveDataScope(ms);
        if (dataScope == null) {
            return;
        }

        CurrentPrincipal currentPrincipal = currentPrincipalProvider.getCurrentPrincipal();
        if (currentPrincipal == null) {
            return;
        }
        if (dataScopeProperties.isAdminBypass() && currentPrincipal.admin()) {
            return;
        }

        String scopeCondition = buildScopeCondition(dataScope, currentPrincipal);
        if (!StringUtils.hasText(scopeCondition)) {
            return;
        }

        String originalSql = boundSql.getSql();
        String newSql = appendScopeCondition(originalSql, scopeCondition);
        if (!originalSql.equals(newSql)) {
            replaceBoundSql(boundSql, newSql);
        }
    }

    private DataScopeContext.ScopeDefinition resolveDataScope(MappedStatement ms) {
        return DataScopeContext.current()
                .orElseGet(() -> annotationCache.computeIfAbsent(ms.getId(), this::findDataScopeAnnotation)
                        .map(DataScopeContext.ScopeDefinition::from)
                        .orElse(null));
    }

    private Optional<DataScope> findDataScopeAnnotation(String statementId) {
        int separatorIndex = statementId.lastIndexOf('.');
        if (separatorIndex < 0) {
            return Optional.empty();
        }

        String className = statementId.substring(0, separatorIndex);
        String methodName = statementId.substring(separatorIndex + 1);

        try {
            Class<?> mapperClass = Class.forName(className);
            return List.of(mapperClass.getMethods()).stream()
                    .filter(method -> method.getName().equals(methodName))
                    .map(method -> method.getAnnotation(DataScope.class))
                    .filter(annotation -> annotation != null)
                    .findFirst();
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
    }

    private String buildScopeCondition(DataScopeContext.ScopeDefinition dataScope, CurrentPrincipal currentPrincipal) {
        return switch (dataScope.type()) {
            case ALL -> null;
            case DEPT_ONLY -> buildDeptOnlyCondition(dataScope);
            case DEPT_AND_CHILD -> buildDeptAndChildCondition(dataScope);
            case SELF_ONLY -> buildSelfOnlyCondition(dataScope, currentPrincipal);
            case CUSTOM -> StringUtils.hasText(dataScope.customCondition()) ? dataScope.customCondition() : null;
        };
    }

    private String buildDeptOnlyCondition(DataScopeContext.ScopeDefinition dataScope) {
        Long deptId = securityContextFacade.getDeptId();
        if (deptId == null) {
            return missingAttributeFallback();
        }
        return dataScope.deptColumn() + " = " + deptId;
    }

    private String buildDeptAndChildCondition(DataScopeContext.ScopeDefinition dataScope) {
        Long deptId = securityContextFacade.getDeptId();
        if (deptId == null) {
            return missingAttributeFallback();
        }

        Set<Long> deptIds = deptHierarchyProvider.resolveDeptAndChildren(deptId);
        if (deptIds == null || deptIds.isEmpty()) {
            deptIds = Set.of(deptId);
        }

        String idList = deptIds.stream()
                .sorted(Comparator.naturalOrder())
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        return dataScope.deptColumn() + " IN (" + idList + ")";
    }

    private String buildSelfOnlyCondition(DataScopeContext.ScopeDefinition dataScope, CurrentPrincipal currentPrincipal) {
        Long userId = securityContextFacade.getAuditUserId();
        if (userId == null) {
            userId = currentPrincipal.principalId();
        }
        if (userId == null) {
            return missingAttributeFallback();
        }
        return dataScope.userColumn() + " = " + userId;
    }

    private String missingAttributeFallback() {
        return dataScopeProperties.isDenyWhenAttributeMissing() ? DENY_ALL_CONDITION : null;
    }

    private String appendScopeCondition(String sql, String condition) {
        String normalized = sql.toLowerCase(Locale.ROOT);
        int tailIndex = findTailClauseIndex(normalized);

        String head = tailIndex >= 0 ? sql.substring(0, tailIndex).trim() : sql.trim();
        String tail = tailIndex >= 0 ? sql.substring(tailIndex) : "";

        String scopedHead;
        if (head.toLowerCase(Locale.ROOT).contains(" where ")) {
            scopedHead = head + " AND (" + condition + ")";
        } else {
            scopedHead = head + " WHERE (" + condition + ")";
        }
        return scopedHead + tail;
    }

    private int findTailClauseIndex(String normalizedSql) {
        return TAIL_CLAUSES.stream()
                .map(normalizedSql::indexOf)
                .filter(index -> index >= 0)
                .min(Integer::compareTo)
                .orElse(-1);
    }

    private void replaceBoundSql(BoundSql boundSql, String newSql) {
        try {
            BOUND_SQL_FIELD.set(boundSql, newSql);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("改写数据权限 SQL 失败", e);
        }
    }

    private static Field locateSqlField() {
        try {
            Field field = BoundSql.class.getDeclaredField("sql");
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("无法定位 BoundSql.sql 字段", e);
        }
    }
}
