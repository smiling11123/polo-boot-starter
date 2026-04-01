package com.polo.boot.context.trans.decorator;

import com.polo.boot.context.trans.properties.ContextTransProperties;
import com.polo.boot.mybatis.plus.service.DataScopeContext;
import com.polo.boot.security.context.UserContext;
import com.polo.boot.security.model.UserPrincipal;
import com.polo.boot.storage.context.UploadContext;
import com.polo.boot.storage.model.UploadResult;
import org.slf4j.MDC;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.task.TaskDecorator;

import java.util.List;
import java.util.Map;

public class ComprehensiveTaskDecorator implements TaskDecorator {
    private final ContextTransProperties properties;
    private final boolean uploadContextEnabled;

    public ComprehensiveTaskDecorator(ContextTransProperties properties, boolean uploadContextEnabled) {
        this.properties = properties;
        this.uploadContextEnabled = uploadContextEnabled;
    }

    @Override
    public Runnable decorate(Runnable runnable) {
        if (!properties.isEnabled()) {
            return runnable;
        }

        ContextSnapshot captured = ContextSnapshot.capture(properties, uploadContextEnabled);
        return () -> {
            ContextSnapshot previous = ContextSnapshot.capture(properties, uploadContextEnabled);
            try {
                captured.apply();
                runnable.run();
            } finally {
                ContextSnapshot.clear(properties, uploadContextEnabled);
                previous.apply();
            }
        };
    }

    private record ContextSnapshot(
            Map<String, String> mdc,
            UserPrincipal userPrincipal,
            List<DataScopeContext.ScopeDefinition> dataScopes,
            LocaleContext localeContext,
            Map<String, List<UploadResult>> uploadResults
    ) {
        private static ContextSnapshot capture(ContextTransProperties properties, boolean uploadContextEnabled) {
            return new ContextSnapshot(
                    properties.isPropagateMdc() ? MDC.getCopyOfContextMap() : null,
                    properties.isPropagateUserContext() ? UserContext.get() : null,
                    properties.isPropagateDataScope() ? DataScopeContext.snapshot() : List.of(),
                    properties.isPropagateLocale() ? LocaleContextHolder.getLocaleContext() : null,
                    shouldPropagateUploadContext(properties, uploadContextEnabled) ? UploadContext.snapshot() : Map.of()
            );
        }

        private void apply() {
            if (mdc != null) {
                MDC.setContextMap(mdc);
            }
            if (userPrincipal != null) {
                UserContext.set(userPrincipal);
            }
            if (dataScopes != null && !dataScopes.isEmpty()) {
                DataScopeContext.restore(dataScopes);
            }
            if (localeContext != null) {
                LocaleContextHolder.setLocaleContext(localeContext);
            }
            if (uploadResults != null && !uploadResults.isEmpty()) {
                UploadContext.restore(uploadResults);
            }
        }

        private static void clear(ContextTransProperties properties, boolean uploadContextEnabled) {
            if (properties.isPropagateMdc()) {
                MDC.clear();
            }
            if (properties.isPropagateUserContext()) {
                UserContext.clear();
            }
            if (properties.isPropagateDataScope()) {
                DataScopeContext.clear();
            }
            if (properties.isPropagateLocale()) {
                LocaleContextHolder.resetLocaleContext();
            }
            if (shouldPropagateUploadContext(properties, uploadContextEnabled)) {
                UploadContext.clear();
            }
        }

        private static boolean shouldPropagateUploadContext(ContextTransProperties properties, boolean uploadContextEnabled) {
            return properties.isPropagateUploadContext() && uploadContextEnabled;
        }
    }
}
