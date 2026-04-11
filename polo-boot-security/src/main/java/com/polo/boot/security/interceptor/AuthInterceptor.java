package com.polo.boot.security.interceptor;

import com.polo.boot.core.constant.ErrorCode;
import com.polo.boot.core.exception.BizException;
import com.polo.boot.security.annotation.RequirePermission;
import com.polo.boot.security.annotation.RequireRole;
import com.polo.boot.security.context.UserContext;
import com.polo.boot.security.model.PermissionProfile;
import com.polo.boot.security.model.UserPrincipal;
import com.polo.boot.security.provider.AuthorizationProvider;
import com.polo.boot.security.service.JwtService;
import com.polo.boot.security.service.TokenService;
import com.polo.boot.security.support.PermissionMatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.Set;

public class AuthInterceptor implements HandlerInterceptor {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private final TokenService tokenService;
    private final JwtService jwtService;
    private final AuthorizationProvider authorizationProvider;
    private final PermissionMatcher permissionMatcher;

    public AuthInterceptor(TokenService tokenService) {
        this(tokenService, null, null, new PermissionMatcher());
    }

    public AuthInterceptor(JwtService jwtService) {
        this(null, jwtService, null, new PermissionMatcher());
    }

    public AuthInterceptor(TokenService tokenService, JwtService jwtService) {
        this(tokenService, jwtService, null, new PermissionMatcher());
    }

    public AuthInterceptor(TokenService tokenService,
                           JwtService jwtService,
                           AuthorizationProvider authorizationProvider,
                           PermissionMatcher permissionMatcher) {
        this.tokenService = tokenService;
        this.jwtService = jwtService;
        this.authorizationProvider = authorizationProvider;
        this.permissionMatcher = permissionMatcher == null ? new PermissionMatcher() : permissionMatcher;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        String token = resolveToken(request.getHeader(AUTHORIZATION_HEADER));
        UserPrincipal userPrincipal = null;
        if (StringUtils.hasText(token)) {
            try {
                userPrincipal = authenticate(token);
                UserContext.set(userPrincipal);
            } catch (BizException e) {
                // 如果是登录过期或无效 Token，对于公开接口，仅记录上下文不失败
                // 这样可以解决 Swagger/前端 带着旧 Token 访问登录等公开接口报错的问题
                if (findRequireRole(handlerMethod) != null || findRequirePermission(handlerMethod) != null) {
                    throw e;
                }
            }
        }

        RequireRole requireRole = findRequireRole(handlerMethod);
        RequirePermission requirePermission = findRequirePermission(handlerMethod);
        if (requireRole == null && requirePermission == null) {
            return true;
        }

        if (userPrincipal == null) {
            throw new BizException(ErrorCode.TOKEN_MISSING);
        }

        if (requireRole != null && !hasRequiredRole(userPrincipal, requireRole.value())) {
            throw ErrorCode.ROLE_DENIED.exception(String.join(", ", requireRole.value()));
        }

        if (requirePermission != null && !hasRequiredPermission(userPrincipal, requirePermission)) {
            throw ErrorCode.PERMISSION_DENIED.exception(String.join(", ", requirePermission.value()));
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private boolean hasRequiredRole(UserPrincipal userPrincipal, String[] requiredRoles) {
        if (userPrincipal.isAdmin()) {
            return true;
        }
        return Arrays.asList(requiredRoles).contains(userPrincipal.getPrincipalRole());
    }

    private boolean hasRequiredPermission(UserPrincipal userPrincipal, RequirePermission requirePermission) {
        Set<String> permissions = resolvePermissions(userPrincipal);
        return permissionMatcher.matches(permissions, requirePermission.value(), requirePermission.logical());
    }

    private Set<String> resolvePermissions(UserPrincipal userPrincipal) {
        if (authorizationProvider != null) {
            PermissionProfile profile = authorizationProvider.loadByUserId(userPrincipal.getPrincipalId());
            if (profile != null && profile.permissions() != null && !profile.permissions().isEmpty()) {
                return profile.permissions();
            }
        }
        return userPrincipal.getPrincipalPermissions();
    }

    private RequireRole findRequireRole(HandlerMethod handlerMethod) {
        RequireRole requireRole = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), RequireRole.class);
        if (requireRole == null) {
            requireRole = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RequireRole.class);
        }
        return requireRole;
    }

    private RequirePermission findRequirePermission(HandlerMethod handlerMethod) {
        RequirePermission requirePermission = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), RequirePermission.class);
        if (requirePermission == null) {
            requirePermission = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RequirePermission.class);
        }
        return requirePermission;
    }

    private String resolveToken(String authorizationHeader) {
        return TokenResolver.resolveBearerToken(authorizationHeader);
    }

    private UserPrincipal authenticate(String token) {
        if (tokenService != null) {
            return tokenService.authenticate(token);
        }
//        if (jwtService != null) {
//            return jwtService.parse(token);
//        }
        throw new IllegalStateException("认证拦截器未配置可用的认证服务");
    }
}
