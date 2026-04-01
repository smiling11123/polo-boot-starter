package com.polo.boot.web.interceptor;

import com.polo.boot.core.util.IpUtil;
import com.polo.boot.web.annotation.OperationLog;
import com.polo.boot.web.model.LogRecorder;
import com.polo.boot.web.model.OperationLogRecord;
import com.polo.boot.web.spi.OperatorContext;
import com.polo.boot.web.spi.OperatorContextProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
public class OperationLogInterceptor implements HandlerInterceptor {

    private final LogRecorder logRecorder;
    private final OperatorContextProvider operatorContextProvider;

    private static final String OP_LOG_START_TIME = "OP_LOG_START_TIME";
    private static final String OP_LOG_RECORDED = "OP_LOG_RECORDED";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(OP_LOG_START_TIME, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 1. 检查是否已经被 OperationLogAspect 记录过，如果是，则直接放行，避免冲突/重复记录
        if (request.getAttribute(OP_LOG_RECORDED) != null) {
            return;
        }

        // 2. 检查是否是请求到具体的 Controller 方法
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return;
        }

        // 3. 检查方法或类上是否有 @OperationLog 注解
        OperationLog operationLog = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), OperationLog.class);
        if (operationLog == null) {
            operationLog = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), OperationLog.class);
        }
        
        if (operationLog == null) {
            return;
        }

        // 4. 进入此阶段说明请求在拦截器（如 AuthInterceptor）中被驳回或抛出异常，AOP 并没有被触发
        long startTime = request.getAttribute(OP_LOG_START_TIME) != null 
                ? (long) request.getAttribute(OP_LOG_START_TIME) 
                : System.currentTimeMillis();
        long costTime = System.currentTimeMillis() - startTime;
        
        String uri = request.getRequestURI();
        String ip = IpUtil.getClientIp(request);
        OperatorContext operatorContext = operatorContextProvider.getCurrentOperator();

        // 尝试获取异常信息，因为在 preHandle 中抛出的异常可能在 DispatcherServlet 中已经被转化为全局异常处理
        // 此处无法还原完整的异常栈，但记录拦截失败状态即可
        String errorMsg = ex != null ? ex.getMessage() : "请求被拦截器(如鉴权)拦截或校验不通过";

        OperationLogRecord record = OperationLogRecord.builder()
                .traceId(MDC.get("traceId"))
                .module(operationLog.module())
                .operationType(operationLog.type().getLabel())
                .description(operationLog.desc()) // 由于无法拿到方法参数，这里暂时只存原始 desc 模板
                .operatorId(operatorContext != null ? operatorContext.operatorId() : null)
                .operatorName(operatorContext != null ? operatorContext.operatorName() : "anonymous(拦截)")
                .operatorIp(ip)
                .userAgent(request.getHeader("User-Agent"))
                .requestUri(uri)
                .requestMethod(request.getMethod())
                .classMethod(handlerMethod.getBeanType().getName() + "." + handlerMethod.getMethod().getName())
                .params(null) // 拦截器阶段未绑定参数，直接不记录
                .result(null)
                .success(false)
                .errorMsg(errorMsg)
                .costTime(costTime)
                .operationTime(LocalDateTime.now())
                .build();

        try {
            logRecorder.record(record);
        } catch (Exception e) {
            log.error("[OperationLogInterceptor] 记录失败日志异常", e);
        }
    }
}
