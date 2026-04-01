package com.polo.boot.mybatis.plus.config;

import com.polo.boot.core.context.CurrentPrincipalProvider;
import com.polo.boot.core.context.SecurityContextFacade;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.polo.boot.mybatis.plus.service.CustomTenantLineHandler;
import com.polo.boot.mybatis.plus.service.DataScopeAspect;
import com.polo.boot.mybatis.plus.service.DataScopeInnerInterceptor;
import com.polo.boot.mybatis.plus.service.DeptHierarchyProvider;
import com.polo.boot.mybatis.plus.service.SmartMetaObjectHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.Set;

@AutoConfiguration
@ConditionalOnClass(MybatisPlusInterceptor.class)
@EnableConfigurationProperties({MybatisPlusProperties.class, DataScopeProperties.class, TenantProperties.class})
@ConditionalOnProperty(prefix = "polo.mybatis-plus", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MybatisPlusAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "polo.mybatis-plus.data-scope", name = "enabled", havingValue = "true")
    public DeptHierarchyProvider deptHierarchyProvider() {
        return deptId -> deptId == null ? Set.of() : Set.of(deptId);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "polo.mybatis-plus.data-scope", name = "enabled", havingValue = "true")
    public DataScopeAspect dataScopeAspect() {
        return new DataScopeAspect();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "polo.mybatis-plus.data-scope", name = "enabled", havingValue = "true")
    public DataScopeInnerInterceptor dataScopeInnerInterceptor(DataScopeProperties dataScopeProperties,
                                                               DeptHierarchyProvider deptHierarchyProvider,
                                                               ObjectProvider<CurrentPrincipalProvider> currentPrincipalProvider,
                                                               ObjectProvider<SecurityContextFacade> securityContextFacade) {
        return new DataScopeInnerInterceptor(
                dataScopeProperties,
                deptHierarchyProvider,
                currentPrincipalProvider.getIfAvailable(CurrentPrincipalProvider::none),
                securityContextFacade.getIfAvailable(SecurityContextFacade::none)
        );
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "polo.mybatis-plus.tenant", name = "enabled", havingValue = "true")
    public CustomTenantLineHandler customTenantLineHandler(TenantProperties tenantProperties,
                                                           ObjectProvider<SecurityContextFacade> securityContextFacade) {
        return new CustomTenantLineHandler(tenantProperties, securityContextFacade.getIfAvailable(SecurityContextFacade::none));
    }

    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusInterceptor mybatisPlusInterceptor(ObjectProvider<DataScopeInnerInterceptor> dataScopeInnerInterceptorProvider,
                                                         ObjectProvider<CustomTenantLineHandler> customTenantLineHandlerProvider,
                                                         MybatisPlusProperties mybatisPlusProperties) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        CustomTenantLineHandler tenantLineHandler = customTenantLineHandlerProvider.getIfAvailable();
        if (tenantLineHandler != null) {
            interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(tenantLineHandler));
        }

        DataScopeInnerInterceptor dataScopeInnerInterceptor = dataScopeInnerInterceptorProvider.getIfAvailable();
        if (dataScopeInnerInterceptor != null) {
            interceptor.addInnerInterceptor(dataScopeInnerInterceptor);
        }

        if (mybatisPlusProperties.isOptimisticLockerEnabled()) {
            interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        }
        if (mybatisPlusProperties.isPaginationEnabled()) {
            interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        }
        return interceptor;
    }

    @Bean
    @ConditionalOnMissingBean(MetaObjectHandler.class)
    @ConditionalOnProperty(prefix = "polo.mybatis-plus", name = "meta-object-handler-enabled", havingValue = "true", matchIfMissing = true)
    public MetaObjectHandler defaultMetaObjectHandler(ObjectProvider<CurrentPrincipalProvider> currentPrincipalProvider,
                                                      ObjectProvider<SecurityContextFacade> securityContextFacade) {
        return new SmartMetaObjectHandler(
                currentPrincipalProvider.getIfAvailable(CurrentPrincipalProvider::none),
                securityContextFacade.getIfAvailable(SecurityContextFacade::none)
        );
    }
}
