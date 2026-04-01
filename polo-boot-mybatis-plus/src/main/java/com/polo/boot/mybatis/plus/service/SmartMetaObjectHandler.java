package com.polo.boot.mybatis.plus.service;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.polo.boot.core.context.CurrentPrincipal;
import com.polo.boot.core.context.CurrentPrincipalProvider;
import com.polo.boot.core.context.SecurityContextFacade;
import com.polo.boot.mybatis.plus.annotation.AutoFillType;
import com.polo.boot.mybatis.plus.model.Auditable;
import com.polo.boot.mybatis.plus.model.DataScopeAware;
import com.polo.boot.mybatis.plus.model.TenantAware;
import org.apache.ibatis.reflection.MetaObject;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

public class SmartMetaObjectHandler implements MetaObjectHandler {
    private final CurrentPrincipalProvider currentPrincipalProvider;
    private final SecurityContextFacade securityContextFacade;

    public SmartMetaObjectHandler(CurrentPrincipalProvider currentPrincipalProvider,
                                  SecurityContextFacade securityContextFacade) {
        this.currentPrincipalProvider = currentPrincipalProvider;
        this.securityContextFacade = securityContextFacade;
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        Object original = metaObject.getOriginalObject();
        if (original == null) {
            return;
        }

        Map<AutoFillType, AutoFillFieldResolver.FieldBinding> fieldBindings = AutoFillFieldResolver.resolve(original.getClass());
        fillAuditOnInsert(metaObject, original, fieldBindings);
        fillTenantFields(metaObject, original, fieldBindings);
        fillDataScopeFields(metaObject, original, fieldBindings);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        Object original = metaObject.getOriginalObject();
        if (original == null) {
            return;
        }
        fillAuditOnUpdate(metaObject, original, AutoFillFieldResolver.resolve(original.getClass()));
    }

    private void fillAuditOnInsert(MetaObject metaObject,
                                   Object original,
                                   Map<AutoFillType, AutoFillFieldResolver.FieldBinding> fieldBindings) {
        if (!(original instanceof Auditable) && !hasAny(fieldBindings,
                AutoFillType.CREATE_TIME,
                AutoFillType.UPDATE_TIME,
                AutoFillType.CREATE_BY,
                AutoFillType.UPDATE_BY)) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        Long auditUserId = securityContextFacade.getAuditUserId();
        CurrentPrincipal currentPrincipal = currentPrincipalProvider.getCurrentPrincipal();

        if (!fillAnnotatedInsert(metaObject, fieldBindings.get(AutoFillType.CREATE_TIME), now)
                && original instanceof Auditable) {
            insertField(metaObject, "createTime", now);
        }
        if (!fillAnnotatedInsert(metaObject, fieldBindings.get(AutoFillType.UPDATE_TIME), now)
                && original instanceof Auditable) {
            insertField(metaObject, "updateTime", now);
        }

        Object auditValue = resolveActorValue(auditUserId, currentPrincipal);
        if (!fillAnnotatedInsert(metaObject, fieldBindings.get(AutoFillType.CREATE_BY), auditValue)
                && original instanceof Auditable && auditUserId != null) {
            insertField(metaObject, "createBy", auditUserId);
        }
        if (!fillAnnotatedInsert(metaObject, fieldBindings.get(AutoFillType.UPDATE_BY), auditValue)
                && original instanceof Auditable && auditUserId != null) {
            insertField(metaObject, "updateBy", auditUserId);
        }
    }

    private void fillAuditOnUpdate(MetaObject metaObject,
                                   Object original,
                                   Map<AutoFillType, AutoFillFieldResolver.FieldBinding> fieldBindings) {
        if (!(original instanceof Auditable) && !hasAny(fieldBindings, AutoFillType.UPDATE_TIME, AutoFillType.UPDATE_BY)) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        Long auditUserId = securityContextFacade.getAuditUserId();
        CurrentPrincipal currentPrincipal = currentPrincipalProvider.getCurrentPrincipal();

        if (!fillAnnotatedUpdate(metaObject, fieldBindings.get(AutoFillType.UPDATE_TIME), now)
                && original instanceof Auditable) {
            updateField(metaObject, "updateTime", now);
        }
        Object auditValue = resolveActorValue(auditUserId, currentPrincipal);
        if (!fillAnnotatedUpdate(metaObject, fieldBindings.get(AutoFillType.UPDATE_BY), auditValue)
                && original instanceof Auditable && auditUserId != null) {
            updateField(metaObject, "updateBy", auditUserId);
        }
    }

    private void fillTenantFields(MetaObject metaObject,
                                  Object original,
                                  Map<AutoFillType, AutoFillFieldResolver.FieldBinding> fieldBindings) {
        Long tenantId = securityContextFacade.getTenantId();
        if (fillAnnotatedInsert(metaObject, fieldBindings.get(AutoFillType.TENANT_ID), tenantId)) {
            return;
        }
        if (tenantId != null && original instanceof TenantAware) {
            insertField(metaObject, "tenantId", tenantId);
        }
    }

    private void fillDataScopeFields(MetaObject metaObject,
                                     Object original,
                                     Map<AutoFillType, AutoFillFieldResolver.FieldBinding> fieldBindings) {
        Long deptId = securityContextFacade.getDeptId();
        if (fillAnnotatedInsert(metaObject, fieldBindings.get(AutoFillType.DEPT_ID), deptId)) {
            return;
        }
        if (deptId != null && original instanceof DataScopeAware) {
            insertField(metaObject, "deptId", deptId);
        }
    }

    private boolean fillAnnotatedInsert(MetaObject metaObject,
                                        AutoFillFieldResolver.FieldBinding binding,
                                        Object rawValue) {
        return fillAnnotated(metaObject, binding, rawValue, true);
    }

    private boolean fillAnnotatedUpdate(MetaObject metaObject,
                                        AutoFillFieldResolver.FieldBinding binding,
                                        Object rawValue) {
        return fillAnnotated(metaObject, binding, rawValue, false);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean fillAnnotated(MetaObject metaObject,
                                  AutoFillFieldResolver.FieldBinding binding,
                                  Object rawValue,
                                  boolean insertPhase) {
        if (binding == null || rawValue == null) {
            return false;
        }

        Object convertedValue = convertFieldValue(rawValue, binding.fieldType());
        if (convertedValue == null) {
            return false;
        }

        if (insertPhase) {
            return insertField(metaObject, binding.propertyName(), convertedValue);
        } else {
            return updateField(metaObject, binding.propertyName(), convertedValue);
        }
    }

    private boolean insertField(MetaObject metaObject, String propertyName, Object value) {
        if (value == null || propertyName == null || !metaObject.hasSetter(propertyName)) {
            return false;
        }
        if (metaObject.getValue(propertyName) != null) {
            return false;
        }
        metaObject.setValue(propertyName, value);
        return true;
    }

    private boolean updateField(MetaObject metaObject, String propertyName, Object value) {
        if (value == null || propertyName == null || !metaObject.hasSetter(propertyName)) {
            return false;
        }
        metaObject.setValue(propertyName, value);
        return true;
    }

    private Object resolveActorValue(Long auditUserId, CurrentPrincipal currentPrincipal) {
        return new ActorValue(
                auditUserId,
                currentPrincipal != null ? currentPrincipal.principalName() : null
        );
    }

    private Object convertFieldValue(Object rawValue, Class<?> targetType) {
        if (rawValue == null || targetType == null) {
            return null;
        }
        if (rawValue instanceof LocalDateTime dateTime) {
            return convertDateTime(dateTime, targetType);
        }
        if (rawValue instanceof ActorValue actorValue) {
            return convertActorValue(actorValue, targetType);
        }
        return ContextValueConverter.convert(rawValue, targetType);
    }

    private Object convertDateTime(LocalDateTime dateTime, Class<?> targetType) {
        Class<?> actualType = wrapPrimitiveType(targetType);
        if (actualType == LocalDateTime.class) {
            return dateTime;
        }
        if (actualType == LocalDate.class) {
            return dateTime.toLocalDate();
        }
        if (actualType == Date.class) {
            Instant instant = dateTime.atZone(ZoneId.systemDefault()).toInstant();
            return Date.from(instant);
        }
        if (actualType == Long.class) {
            return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
        if (actualType == String.class) {
            return dateTime.toString();
        }
        return null;
    }

    private Object convertActorValue(ActorValue actorValue, Class<?> targetType) {
        Class<?> actualType = wrapPrimitiveType(targetType);
        if (actualType == String.class) {
            return actorValue.username();
        }
        return ContextValueConverter.convert(actorValue.userId(), targetType);
    }

    private boolean hasAny(Map<AutoFillType, AutoFillFieldResolver.FieldBinding> fieldBindings, AutoFillType... types) {
        for (AutoFillType type : types) {
            if (fieldBindings.containsKey(type)) {
                return true;
            }
        }
        return false;
    }

    private Class<?> wrapPrimitiveType(Class<?> type) {
        if (type == long.class) {
            return Long.class;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        return type;
    }

    private record ActorValue(Long userId, String username) {
    }
}
