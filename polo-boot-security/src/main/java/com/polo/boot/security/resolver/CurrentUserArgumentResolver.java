package com.polo.boot.security.resolver;

import com.polo.boot.core.constant.ErrorCode;
import com.polo.boot.core.exception.BizException;
import com.polo.boot.security.annotation.CurrentUser;
import com.polo.boot.security.context.SecurityPrincipalSupport;
import com.polo.boot.security.context.UserContext;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && SecurityPrincipalSupport.supportsType(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        var userPrincipal = UserContext.get();
        if (userPrincipal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return SecurityPrincipalSupport.convert(userPrincipal, parameter.getParameterType());
    }
}
