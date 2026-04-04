package com.polo.boot.web.aspect;

import com.polo.boot.core.util.IpUtil;
import com.polo.boot.web.annotation.OperationLog;
import com.polo.boot.web.model.LogRecorder;
import com.polo.boot.web.model.OperationLogRecord;
import com.polo.boot.web.spel.SpelExpressionUtil;
import com.polo.boot.web.spi.OperatorContext;
import com.polo.boot.web.spi.OperatorContextProvider;
import com.polo.boot.web.utils.JsonMaskUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.Executor;

@Slf4j
@Aspect
@Order(100)  // 在事务注解之后执行，确保能记录到最终结果
@RequiredArgsConstructor
public class OperationLogAspect {

    private final LogRecorder logRecorder;
    private final OperatorContextProvider operatorContextProvider;
    private final Executor asyncExecutor;

    @Around(value = "@annotation(operationLog)", argNames = "point,operationLog")
    public Object around(ProceedingJoinPoint point, OperationLog operationLog) throws Throwable {
        // 1. 准备阶段
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();

        // 2. 构建 SpEL 上下文（此时还没有返回值）
        EvaluationContext context = SpelExpressionUtil.createContext(method, point.getArgs());
        boolean descReferencesResult = SpelExpressionUtil.referencesResult(operationLog.desc());
        boolean descReferencesError = SpelExpressionUtil.referencesError(operationLog.desc());

        // 3. 解析描述（如果 SpEL 依赖返回值，这里可能解析不完整，需要二次解析）
        String desc = SpelExpressionUtil.parse(operationLog.desc(), context);

        // 4. 准备请求信息
        HttpServletRequest request = getCurrentRequest();
        String uri = request != null ? request.getRequestURI() : "N/A";
        String ip = request != null ? IpUtil.getClientIp(request) : "N/A";

        // 5. 执行目标方法
        Object result = null;
        Throwable error = null;
        long startTime = System.currentTimeMillis();

        try {
            result = point.proceed();

            // 方法成功执行后，将返回值加入上下文，重新解析描述（支持 #result）
            if (descReferencesResult) {
                SpelExpressionUtil.addResult(context, result);
                desc = SpelExpressionUtil.parse(operationLog.desc(), context);
            }

            return result;

        } catch (Throwable e) {
            error = e;
            if (descReferencesError) {
                SpelExpressionUtil.addException(context, e);
                desc = SpelExpressionUtil.parse(operationLog.desc(), context);
            }

            // 判断是否忽略该异常
            if (shouldIgnore(e, operationLog.ignoreExceptions())) {
                throw e;  // 直接抛出，不记录日志
            }
            throw e;
        } finally {
            // 6. 构建并记录日志
            long costTime = System.currentTimeMillis() - startTime;

            OperationLogRecord record = buildRecord(
                    operationLog, desc, point, request, uri, ip,
                    result, error, costTime, context
            );

            // 7. 记录日志（同步或异步）
            doRecord(record, operationLog.async());
            
            if (request != null) {
                request.setAttribute("OP_LOG_RECORDED", true);
            }
        }
    }

    /**
     * 构建日志记录
     */
    private OperationLogRecord buildRecord(
            OperationLog annotation,
            String desc,
            ProceedingJoinPoint point,
            HttpServletRequest request,
            String uri,
            String ip,
            Object result,
            Throwable error,
            long costTime,
            EvaluationContext context) {

        // 处理参数（脱敏）
        String paramsJson = null;
        if (annotation.logParams()) {
            Object[] args = point.getArgs();
            Object[] serializableArgs = Arrays.stream(args)
                    .filter(arg -> !(arg instanceof HttpServletRequest))
                    .filter(arg -> !(arg instanceof HttpServletResponse))
                    .filter(arg -> !(arg instanceof MultipartFile))
                    .toArray();
            paramsJson = JsonMaskUtil.toJsonWithMask(serializableArgs, annotation.paramMaskPatterns());
        }

        // 处理结果（脱敏）
        String resultJson = null;
        if (annotation.logResult() && error == null) {
            resultJson = JsonMaskUtil.toJsonWithMask(result, annotation.resultMaskPatterns());
        }

        OperatorContext operatorContext = operatorContextProvider.getCurrentOperator();
        return OperationLogRecord.builder()
                .traceId(MDC.get("traceId"))
                .module(annotation.module())
                .operationType(annotation.type().getLabel())
                .description(desc)
                .operatorId(operatorContext != null ? operatorContext.operatorId() : null)
                .operatorName(operatorContext != null ? operatorContext.operatorName() : "anonymous")
                .operatorIp(ip)
                .userAgent(request != null ? request.getHeader("User-Agent") : null)
                .requestUri(uri)
                .requestMethod(request != null ? request.getMethod() : "N/A")
                .classMethod(point.getTarget().getClass().getName() + "." + point.getSignature().getName())
                .params(paramsJson)
                .result(resultJson)
                .success(error == null)
                .errorMsg(error != null ? error.getMessage() : null)
                .costTime(costTime)
                .operationTime(LocalDateTime.now())
                .build();
    }

    /**
     * 执行记录
     */
    private void doRecord(OperationLogRecord record, boolean async) {
        if (async) {
            Runnable task = () -> {
                try {
                    logRecorder.record(record);
                } catch (Exception e) {
                    log.error("[OperationLog] 异步记录失败", e);
                }
            };
            if (asyncExecutor != null) {
                asyncExecutor.execute(task);
            } else {
                try {
                    logRecorder.record(record);
                } catch (Exception e) {
                    log.error("[OperationLog] 同步兜底记录失败", e);
                }
            }
        } else {
            try {
                logRecorder.record(record);
            } catch (Exception e) {
                log.error("[OperationLog] 同步记录失败", e);
            }
        }
    }

    /**
     * 判断是否忽略特定异常
     */
    private boolean shouldIgnore(Throwable error, Class<? extends Exception>[] ignoreTypes) {
        if (ignoreTypes == null || ignoreTypes.length == 0) {
            return false;
        }
        return Arrays.stream(ignoreTypes)
                .anyMatch(type -> type.isAssignableFrom(error.getClass()));
    }

    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes)
                    RequestContextHolder.currentRequestAttributes();
            return attrs.getRequest();
        } catch (Exception e) {
            return null;
        }
    }

}
