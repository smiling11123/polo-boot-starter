package com.polo.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.polo.boot.core.context.CurrentPrincipal;
import com.polo.boot.core.context.CurrentPrincipalProvider;
import com.polo.boot.core.context.SecurityContextFacade;
import com.polo.boot.core.constant.ErrorCode;
import com.polo.boot.core.exception.BizException;
import com.polo.boot.mybatis.plus.annotation.DataScope;
import com.polo.boot.mybatis.plus.annotation.DataScopeType;
import com.polo.demo.entity.DemoRecordEntity;
import com.polo.demo.mapper.DemoRecordMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MybatisDemoService {
    private final DemoRecordMapper demoRecordMapper;
    private final JdbcTemplate jdbcTemplate;
    private final CurrentPrincipalProvider currentPrincipalProvider;
    private final SecurityContextFacade securityContextFacade;

    public MybatisDemoService(DemoRecordMapper demoRecordMapper,
                              JdbcTemplate jdbcTemplate,
                              CurrentPrincipalProvider currentPrincipalProvider,
                              SecurityContextFacade securityContextFacade) {
        this.demoRecordMapper = demoRecordMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.currentPrincipalProvider = currentPrincipalProvider;
        this.securityContextFacade = securityContextFacade;
    }

    public Map<String, Object> overview() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("currentPrincipal", currentPrincipalProvider.getCurrentPrincipal());
        result.put("tenantId", securityContextFacade.getTenantId());
        result.put("deptId", securityContextFacade.getDeptId());
        result.put("dataScope", securityContextFacade.getDataScope());
        result.put("rawTotalCount", jdbcTemplate.queryForObject("select count(*) from demo_record", Long.class));
        result.put("rawTenantCount", jdbcTemplate.queryForObject(
                "select count(*) from demo_record where tenant_id = ?",
                Long.class,
                securityContextFacade.getTenantId()
        ));
        result.put("tenantVisibleCount", demoRecordMapper.selectCount(baseQuery()));
        return result;
    }

    public List<Map<String, Object>> listAllRawRecords() {
        return jdbcTemplate.queryForList("select * from demo_record order by id");
    }

    public List<DemoRecordEntity> listTenantVisibleRecords() {
        return demoRecordMapper.selectList(baseQuery());
    }

    public Page<DemoRecordEntity> pageTenantVisibleRecords(long current, long size) {
        Page<DemoRecordEntity> page = new Page<>(current, size);
        return demoRecordMapper.selectPage(page, baseQuery());
    }

    @DataScope(type = DataScopeType.DEPT_AND_CHILD, deptColumn = "dept_id")
    public List<DemoRecordEntity> listDeptScopedRecords() {
        return demoRecordMapper.selectList(baseQuery());
    }

    @DataScope(type = DataScopeType.SELF_ONLY, userColumn = "create_by")
    public List<DemoRecordEntity> listSelfScopedRecords() {
        return demoRecordMapper.selectList(baseQuery());
    }

    @DataScope(type = DataScopeType.CUSTOM, customCondition = "status = 'APPROVED'")
    public List<DemoRecordEntity> listApprovedRecords() {
        return demoRecordMapper.selectList(baseQuery());
    }

    public DemoRecordEntity getVisibleRecord(Long id) {
        DemoRecordEntity entity = demoRecordMapper.selectById(id);
        if (entity == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND.getCode(), "记录不存在或当前租户不可见");
        }
        return entity;
    }

    public Map<String, Object> getAuditDetail(Long id) {
        return Map.of(
                "visibleRecord", getVisibleRecord(id),
                "rawRecord", getRawRecord(id),
                "currentPrincipal", currentPrincipalProvider.getCurrentPrincipal()
        );
    }

    @Transactional
    public Map<String, Object> createRecord(String title, String content, String status) {
        if (!StringUtils.hasText(title) || !StringUtils.hasText(status)) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "title 和 status 不能为空");
        }

        DemoRecordEntity entity = new DemoRecordEntity();
        entity.setTitle(title);
        entity.setContent(content);
        entity.setStatus(status);
        entity.setVersion(1);
        demoRecordMapper.insert(entity);

        return Map.of(
                "created", true,
                "record", demoRecordMapper.selectById(entity.getId()),
                "rawRecord", getRawRecord(entity.getId()),
                "currentPrincipal", currentPrincipalProvider.getCurrentPrincipal()
        );
    }

    @Transactional
    public Map<String, Object> updateRecord(Long id,
                                            Integer version,
                                            String title,
                                            String content,
                                            String status) {
        DemoRecordEntity current = getVisibleRecord(id);
        int updatedRows = doUpdate(id, version != null ? version : current.getVersion(), title, content, status);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("updatedRows", updatedRows);
        result.put("optimisticLockSuccess", updatedRows == 1);
        result.put("record", updatedRows == 1 ? demoRecordMapper.selectById(id) : current);
        result.put("rawRecord", getRawRecord(id));
        return result;
    }

    @Transactional
    @DataScope(type = DataScopeType.SELF_ONLY, userColumn = "create_by")
    public Map<String, Object> updateOwnRecord(Long id,
                                               Integer version,
                                               String title,
                                               String content,
                                               String status) {
        DemoRecordEntity current = demoRecordMapper.selectById(id);
        if (current == null) {
            throw new BizException(ErrorCode.PERMISSION_DENIED.getCode(), "当前用户只能更新自己创建的记录");
        }
        int updatedRows = doUpdate(id, version != null ? version : current.getVersion(), title, content, status);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("updatedRows", updatedRows);
        result.put("selfScopePassed", true);
        result.put("optimisticLockSuccess", updatedRows == 1);
        result.put("record", updatedRows == 1 ? demoRecordMapper.selectById(id) : current);
        result.put("rawRecord", getRawRecord(id));
        return result;
    }

    @Transactional
    public Map<String, Object> simulateOptimisticLock(Long id) {
        DemoRecordEntity original = getVisibleRecord(id);
        Integer originalVersion = original.getVersion();

        int firstRows = doUpdate(id, originalVersion, original.getTitle() + " [first]", original.getContent(), original.getStatus());
        DemoRecordEntity afterFirst = demoRecordMapper.selectById(id);
        int secondRows = doUpdate(id, originalVersion, original.getTitle() + " [stale]", original.getContent(), original.getStatus());

        return Map.of(
                "id", id,
                "originalVersion", originalVersion,
                "firstUpdateRows", firstRows,
                "secondUpdateRows", secondRows,
                "optimisticLockWorked", firstRows == 1 && secondRows == 0,
                "recordAfterFirstUpdate", afterFirst,
                "rawRecord", getRawRecord(id)
        );
    }

    private int doUpdate(Long id,
                         Integer version,
                         String title,
                         String content,
                         String status) {
        DemoRecordEntity update = new DemoRecordEntity();
        update.setId(id);
        update.setVersion(version);
        if (StringUtils.hasText(title)) {
            update.setTitle(title);
        }
        update.setContent(content);
        if (StringUtils.hasText(status)) {
            update.setStatus(status);
        }
        return demoRecordMapper.updateById(update);
    }

    private Map<String, Object> getRawRecord(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("select * from demo_record where id = ?", id);
        if (rows.isEmpty()) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND.getCode(), "记录不存在");
        }
        return rows.getFirst();
    }

    private LambdaQueryWrapper<DemoRecordEntity> baseQuery() {
        return new LambdaQueryWrapper<DemoRecordEntity>()
                .orderByAsc(DemoRecordEntity::getId);
    }
}
