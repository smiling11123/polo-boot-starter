package com.polo.boot.storage.context;

import com.polo.boot.storage.model.UploadResult;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 当前请求内的上传结果上下文，方便 controller/service 继续处理上传结果。
 */
public final class UploadContext {

    private static final String ATTRIBUTE_KEY = UploadContext.class.getName() + ".RESULTS";
    private static final ThreadLocal<Map<String, List<UploadResult>>> HOLDER = new ThreadLocal<>();

    private UploadContext() {
    }

    public static void add(String fieldName, List<UploadResult> results) {
        if (results == null || results.isEmpty()) {
            return;
        }
        Map<String, List<UploadResult>> allResults = getMutableResults();
        allResults.put(fieldName, new ArrayList<>(results));
        storeResults(allResults);
    }

    public static List<UploadResult> getResults() {
        Map<String, List<UploadResult>> allResults = getResultsByField();
        List<UploadResult> merged = new ArrayList<>();
        allResults.values().forEach(merged::addAll);
        return Collections.unmodifiableList(merged);
    }

    public static Map<String, List<UploadResult>> getResultsByField() {
        Map<String, List<UploadResult>> storedResults = getStoredResults();
        if (storedResults.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, List<UploadResult>> results = new LinkedHashMap<>();
        storedResults.forEach((key, value) -> {
            if (key instanceof String fieldName && value instanceof List<?> list) {
                List<UploadResult> uploadResults = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof UploadResult uploadResult) {
                        uploadResults.add(uploadResult);
                    }
                }
                results.put(fieldName, Collections.unmodifiableList(uploadResults));
            }
        });
        return Collections.unmodifiableMap(results);
    }

    public static List<UploadResult> getResults(String fieldName) {
        return getResultsByField().getOrDefault(fieldName, Collections.emptyList());
    }

    public static Optional<UploadResult> getFirst(String fieldName) {
        return getResults(fieldName).stream().findFirst();
    }

    public static void clear() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            requestAttributes.removeAttribute(ATTRIBUTE_KEY, RequestAttributes.SCOPE_REQUEST);
        }
        HOLDER.remove();
    }

    public static Map<String, List<UploadResult>> snapshot() {
        Map<String, List<UploadResult>> storedResults = getStoredResults();
        if (storedResults.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(copyResults(storedResults));
    }

    public static void restore(Map<String, List<UploadResult>> results) {
        if (results == null || results.isEmpty()) {
            HOLDER.remove();
            return;
        }
        HOLDER.set(copyResults(results));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, List<UploadResult>> getMutableResults() {
        Map<String, List<UploadResult>> storedResults = getStoredResults();
        if (storedResults.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return new LinkedHashMap<>(storedResults);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, List<UploadResult>> getStoredResults() {
        Map<String, List<UploadResult>> threadLocalResults = HOLDER.get();
        if (threadLocalResults != null && !threadLocalResults.isEmpty()) {
            return threadLocalResults;
        }
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            return Collections.emptyMap();
        }
        Object attribute = requestAttributes.getAttribute(ATTRIBUTE_KEY, RequestAttributes.SCOPE_REQUEST);
        if (attribute instanceof Map<?, ?> map) {
            return (Map<String, List<UploadResult>>) map;
        }
        return Collections.emptyMap();
    }

    private static void storeResults(Map<String, List<UploadResult>> results) {
        Map<String, List<UploadResult>> copiedResults = copyResults(results);
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            requestAttributes.setAttribute(ATTRIBUTE_KEY, copiedResults, RequestAttributes.SCOPE_REQUEST);
            return;
        }
        HOLDER.set(copiedResults);
    }

    private static Map<String, List<UploadResult>> copyResults(Map<String, List<UploadResult>> results) {
        Map<String, List<UploadResult>> copied = new LinkedHashMap<>();
        results.forEach((key, value) -> copied.put(key, value == null ? List.of() : new ArrayList<>(value)));
        return copied;
    }
}
