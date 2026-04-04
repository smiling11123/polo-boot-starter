package com.polo.boot.web.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JsonMaskUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String MASK = "****";
    private static final ConcurrentMap<String, List<MaskRule>> MASK_RULE_CACHE = new ConcurrentHashMap<>();

    /**
     * 对象转 JSON 并脱敏
     */
    public static String toJsonWithMask(Object obj, String[] maskPatterns) {
        if (obj == null) return null;

        try {
            // 先转 JSON 树
            ObjectNode root;
            if (obj instanceof Object[]) {
                ArrayNode array = MAPPER.valueToTree(obj);
                for (int i = 0; i < array.size(); i++) {
                    maskNode(array.get(i), maskPatterns);
                }
                return MAPPER.writeValueAsString(array);
            } else {
                root = MAPPER.valueToTree(obj);
                maskNode(root, maskPatterns);
                return MAPPER.writeValueAsString(root);
            }
        } catch (Exception e) {
            return "[序列化失败: " + e.getMessage() + "]";
        }
    }

    /**
     * 递归脱敏节点
     */
    private static void maskNode(Object node, String[] patterns) {
        if (!(node instanceof ObjectNode)) return;

        ObjectNode objNode = (ObjectNode) node;
        objNode.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            Object value = entry.getValue();

            // 匹配脱敏字段
            if (shouldMask(key, patterns)) {
                if (value instanceof ValueNode && !((ValueNode) value).isNull()) {
                    objNode.put(key, MASK);
                }
            } else if (value instanceof ObjectNode) {
                maskNode((ObjectNode) value, patterns);  // 递归
            } else if (value instanceof ArrayNode) {
                ((ArrayNode) value).forEach(item -> maskNode(item, patterns));
            }
        });
    }

    private static boolean shouldMask(String key, String[] patterns) {
        if (patterns == null || patterns.length == 0) {
            return false;
        }
        String lowerKey = key.toLowerCase();
        return resolveRules(patterns).stream().anyMatch(rule -> rule.matches(key, lowerKey));
    }

    private static List<MaskRule> resolveRules(String[] patterns) {
        String cacheKey = Arrays.stream(patterns)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\u0001"));
        return MASK_RULE_CACHE.computeIfAbsent(cacheKey, ignored -> Arrays.stream(patterns)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(pattern -> !pattern.isEmpty())
                .map(MaskRule::new)
                .toList());
    }

    private record MaskRule(String rawPattern, String lowerPattern, Pattern regexPattern) {
        private MaskRule(String rawPattern) {
            this(rawPattern, rawPattern.toLowerCase(), Pattern.compile(rawPattern));
        }

        private boolean matches(String key, String lowerKey) {
            return lowerKey.contains(lowerPattern) || regexPattern.matcher(key).matches();
        }
    }
}
