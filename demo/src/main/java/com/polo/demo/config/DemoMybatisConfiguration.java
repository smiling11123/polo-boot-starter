package com.polo.demo.config;

import com.polo.boot.mybatis.plus.service.DeptHierarchyProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class DemoMybatisConfiguration {
    @Bean
    public DeptHierarchyProvider demoDeptHierarchyProvider() {
        return deptId -> {
            if (deptId == null) {
                return Set.of();
            }
            if (deptId == 10L) {
                return Set.of(10L, 20L, 21L, 30L);
            }
            if (deptId == 20L) {
                return Set.of(20L, 21L);
            }
            return Set.of(deptId);
        };
    }
}
