package com.polo.boot.security.resolver;

import com.polo.boot.core.constant.ErrorCode;
import com.polo.boot.core.exception.BizException;
import com.polo.boot.security.annotation.CurrentUserAttribute;
import com.polo.boot.security.context.UserContext;
import com.polo.boot.security.model.UserPrincipal;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class CurrentUserAttributeArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserAttribute.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        UserPrincipal userPrincipal = UserContext.get();
        if (userPrincipal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        CurrentUserAttribute annotation = parameter.getParameterAnnotation(CurrentUserAttribute.class);
        if (annotation == null) {
            return null;
        }

        Object value = UserContext.getAttribute(annotation.value(), parameter.getParameterType());
        if (value == null && annotation.required()) {
            throw new BizException(ErrorCode.SYSTEM_ERROR.getCode(), "当前登录上下文缺少属性: " + annotation.value());
        }
        return value;
    }
}
