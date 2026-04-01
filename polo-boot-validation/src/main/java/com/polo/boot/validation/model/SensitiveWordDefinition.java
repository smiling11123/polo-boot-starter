package com.polo.boot.validation.model;

import com.polo.boot.validation.annotation.InputContent;

/**
 * 敏感词定义，用于描述任意词库来源的一条标准化词条。
 */
public record SensitiveWordDefinition(
        String word,
        String category,
        int level
) {
}
