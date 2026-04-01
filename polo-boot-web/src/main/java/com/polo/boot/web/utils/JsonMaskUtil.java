package com.polo.boot.web.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;

import java.util.Arrays;
import java.util.regex.Pattern;

public class JsonMaskUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String MASK = "****";

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
        String lowerKey = key.toLowerCase();
        return Arrays.stream(patterns).anyMatch(pattern ->
                lowerKey.contains(pattern.toLowerCase()) ||
                        Pattern.matches(pattern, key)
        );
    }
}