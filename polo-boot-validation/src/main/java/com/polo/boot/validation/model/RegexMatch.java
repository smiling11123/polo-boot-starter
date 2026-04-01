package com.polo.boot.validation.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegexMatch {
    private int start;          // 匹配起始位置
    private int end;            // 匹配结束位置
    private String type;        // 类型：PHONE, ID_CARD, BANK_CARD, EMAIL
    private String content;     // 匹配到的内容
}
