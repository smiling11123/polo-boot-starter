package com.polo.boot.validation.provider;

import com.polo.boot.validation.model.SensitiveWordDefinition;

import java.util.List;

public interface SensitiveWordProvider {
    /**
     * 从数据库或其他外部数据源加载敏感词。
     * starter 会在启动、定时刷新、手动刷新时统一调用该方法。
     */
    List<SensitiveWordDefinition> loadWords();

}
