package com.polo.boot.mybatis.plus.service;

import com.polo.boot.mybatis.plus.annotation.DataScope;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;

@Aspect
public class DataScopeAspect {
    @Around("@annotation(com.polo.boot.mybatis.plus.annotation.DataScope) || @within(com.polo.boot.mybatis.plus.annotation.DataScope)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        DataScope dataScope = resolveDataScope(joinPoint);
        if (dataScope == null) {
            return joinPoint.proceed();
        }

        DataScopeContext.push(dataScope);
        try {
            return joinPoint.proceed();
        } finally {
            DataScopeContext.pop();
        }
    }

    private DataScope resolveDataScope(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method interfaceMethod = signature.getMethod();
        Method specificMethod = AopUtils.getMostSpecificMethod(interfaceMethod, joinPoint.getTarget().getClass());
        DataScope dataScope = AnnotatedElementUtils.findMergedAnnotation(specificMethod, DataScope.class);
        if (dataScope != null) {
            return dataScope;
        }
        dataScope = AnnotatedElementUtils.findMergedAnnotation(interfaceMethod, DataScope.class);
        if (dataScope != null) {
            return dataScope;
        }
        return AnnotatedElementUtils.findMergedAnnotation(joinPoint.getTarget().getClass(), DataScope.class);
    }
}
