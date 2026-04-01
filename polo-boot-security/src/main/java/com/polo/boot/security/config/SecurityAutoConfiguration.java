package com.polo.boot.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.polo.boot.core.context.CurrentPrincipalProvider;
import com.polo.boot.core.context.SecurityContextFacade;
import com.polo.boot.security.aspect.DuplicateSubmitAspect;
import com.polo.boot.security.aspect.RateLimitAspect;
import com.polo.boot.security.interceptor.AuthInterceptor;
import com.polo.boot.security.provider.AuthorizationProvider;
import com.polo.boot.security.provider.SecurityCurrentPrincipalProvider;
import com.polo.boot.security.provider.SecurityOperatorContextProvider;
import com.polo.boot.security.provider.SecuritySecurityContextFacade;
import com.polo.boot.security.properties.JwtProperties;
import com.polo.boot.security.properties.SecurityProperties;
import com.polo.boot.security.resolver.CurrentUserAttributeArgumentResolver;
import com.polo.boot.security.resolver.CurrentUserArgumentResolver;
import com.polo.boot.security.service.DeviceService;
import com.polo.boot.security.service.JwtService;
import com.polo.boot.security.service.QrCodeService;
import com.polo.boot.security.service.TokenService;
import com.polo.boot.security.resolver.RateLimitKeyResolver;
import com.polo.boot.security.support.PermissionMatcher;
import java.util.List;
import com.polo.boot.security.support.RateLimitScripts;
import com.polo.boot.security.support.RedisRateLimiter;
import com.polo.boot.web.spi.OperatorContextProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@AutoConfiguration(after = {RedisAutoConfiguration.class, JacksonAutoConfiguration.class})
@ConditionalOnClass(WebMvcConfigurer.class)
@EnableConfigurationProperties({JwtProperties.class, SecurityProperties.class})
@ConditionalOnProperty(prefix = "polo.security", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "polo.security", name = "auth-enabled", havingValue = "true", matchIfMissing = true)
    public JwtService jwtService(JwtProperties jwtProperties) {
        return new JwtService(jwtProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "polo.security", name = "auth-enabled", havingValue = "true", matchIfMissing = true)
    public DeviceService deviceService() {
        return new DeviceService();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({JwtService.class, DeviceService.class, StringRedisTemplate.class})
    @ConditionalOnProperty(prefix = "polo.security", name = "auth-enabled", havingValue = "true", matchIfMissing = true)
    public TokenService tokenService(JwtService jwtService,
                                     DeviceService deviceService,
                                     StringRedisTemplate stringRedisTemplate,
                                     ObjectMapper objectMapper,
                                     JwtProperties jwtProperties) {
        return new TokenService(jwtService, deviceService, stringRedisTemplate, objectMapper, jwtProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "polo.security", name = "auth-enabled", havingValue = "true", matchIfMissing = true)
    public PermissionMatcher permissionMatcher() {
        return new PermissionMatcher();
    }

    @Bean
    @ConditionalOnMissingBean
    public OperatorContextProvider operatorContextProvider() {
        return new SecurityOperatorContextProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public CurrentPrincipalProvider currentPrincipalProvider() {
        return new SecurityCurrentPrincipalProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityContextFacade securityContextFacade() {
        return new SecuritySecurityContextFacade();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(TokenService.class)
    @ConditionalOnProperty(prefix = "polo.security.jwt", name = "session-enabled", havingValue = "true")
    public AuthInterceptor sessionAuthInterceptor(TokenService tokenService,
                                                  ObjectProvider<AuthorizationProvider> authorizationProvider,
                                                  PermissionMatcher permissionMatcher) {
        return new AuthInterceptor(tokenService, null, authorizationProvider.getIfAvailable(), permissionMatcher);
    }

    @Bean
    @ConditionalOnMissingBean(AuthInterceptor.class)
    @ConditionalOnBean(JwtService.class)
    @ConditionalOnProperty(prefix = "polo.security.jwt", name = "session-enabled", havingValue = "false", matchIfMissing = true)
    public AuthInterceptor statelessAuthInterceptor(JwtService jwtService,
                                                    ObjectProvider<AuthorizationProvider> authorizationProvider,
                                                    PermissionMatcher permissionMatcher) {
        return new AuthInterceptor(null, jwtService, authorizationProvider.getIfAvailable(), permissionMatcher);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "polo.security", name = "current-user-resolver-enabled", havingValue = "true", matchIfMissing = true)
    public CurrentUserArgumentResolver currentUserArgumentResolver() {
        return new CurrentUserArgumentResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "polo.security", name = "current-user-resolver-enabled", havingValue = "true", matchIfMissing = true)
    public CurrentUserAttributeArgumentResolver currentUserAttributeArgumentResolver() {
        return new CurrentUserAttributeArgumentResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(StringRedisTemplate.class)
    @ConditionalOnProperty(prefix = "polo.security", name = "duplicate-submit-enabled", havingValue = "true", matchIfMissing = true)
    public DuplicateSubmitAspect duplicateSubmitAspect(StringRedisTemplate stringRedisTemplate) {
        return new DuplicateSubmitAspect(stringRedisTemplate);
    }

    @Bean(name = "tokenBucketScript")
    @ConditionalOnMissingBean(name = "tokenBucketScript")
    @ConditionalOnProperty(prefix = "polo.security", name = "rate-limit-enabled", havingValue = "true", matchIfMissing = true)
    public DefaultRedisScript<List> tokenBucketScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setScriptText(RateLimitScripts.TOKEN_BUCKET_SCRIPT);
        script.setResultType(List.class);
        return script;
    }

    @Bean(name = "slidingWindowScript")
    @ConditionalOnMissingBean(name = "slidingWindowScript")
    @ConditionalOnProperty(prefix = "polo.security", name = "rate-limit-enabled", havingValue = "true", matchIfMissing = true)
    public DefaultRedisScript<List> slidingWindowScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setScriptText(RateLimitScripts.SLIDING_WINDOW_SCRIPT);
        script.setResultType(List.class);
        return script;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "polo.security", name = "rate-limit-enabled", havingValue = "true", matchIfMissing = true)
    public RateLimitKeyResolver rateLimitKeyResolver() {
        return new RateLimitKeyResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(StringRedisTemplate.class)
    @ConditionalOnProperty(prefix = "polo.security", name = "rate-limit-enabled", havingValue = "true", matchIfMissing = true)
    public RedisRateLimiter redisRateLimiter(StringRedisTemplate stringRedisTemplate,
                                             @Qualifier("tokenBucketScript") DefaultRedisScript<List> tokenBucketScript,
                                             @Qualifier("slidingWindowScript") DefaultRedisScript<List> slidingWindowScript) {
        return new RedisRateLimiter(stringRedisTemplate, tokenBucketScript, slidingWindowScript);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({RedisRateLimiter.class, RateLimitKeyResolver.class})
    @ConditionalOnProperty(prefix = "polo.security", name = "rate-limit-enabled", havingValue = "true", matchIfMissing = true)
    public RateLimitAspect rateLimitAspect(RedisRateLimiter redisRateLimiter,
                                           RateLimitKeyResolver rateLimitKeyResolver) {
        return new RateLimitAspect(redisRateLimiter, rateLimitKeyResolver);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({StringRedisTemplate.class, JwtService.class})
    @ConditionalOnProperty(prefix = "polo.security", name = "qr-code-enabled", havingValue = "true", matchIfMissing = true)
    public QrCodeService qrCodeService(StringRedisTemplate stringRedisTemplate,
                                       ObjectProvider<TokenService> tokenServiceProvider,
                                       JwtService jwtService,
                                       JwtProperties jwtProperties,
                                       ObjectMapper objectMapper) {
        return new QrCodeService(stringRedisTemplate, tokenServiceProvider, jwtService, jwtProperties, objectMapper);
    }

    @Bean
    public WebMvcConfigurer securityWebMvcConfigurer(ObjectProvider<AuthInterceptor> authInterceptorProvider,
                                                     ObjectProvider<CurrentUserArgumentResolver> currentUserArgumentResolverProvider,
                                                     ObjectProvider<CurrentUserAttributeArgumentResolver> currentUserAttributeArgumentResolverProvider) {
        AuthInterceptor authInterceptor = authInterceptorProvider.getIfAvailable();
        CurrentUserArgumentResolver currentUserArgumentResolver = currentUserArgumentResolverProvider.getIfAvailable();
        CurrentUserAttributeArgumentResolver currentUserAttributeArgumentResolver = currentUserAttributeArgumentResolverProvider.getIfAvailable();
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                if (authInterceptor != null) {
                    registry.addInterceptor(authInterceptor).addPathPatterns("/**");
                }
            }

            @Override
            public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
                if (currentUserArgumentResolver != null) {
                    resolvers.add(currentUserArgumentResolver);
                }
                if (currentUserAttributeArgumentResolver != null) {
                    resolvers.add(currentUserAttributeArgumentResolver);
                }
            }
        };
    }
}
