package com.polo.boot.cache.service;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.concurrent.TimeUnit;

public interface RedisService {
    /**
     * 记录缓存
     * @param key 键
     * @param value 值
     * @param timeout 缓存时长
     * @param unit 时长单位
     * @param <T> 数据类型
     */
    <T> void set(String key, T value, long timeout, TimeUnit unit);

    /**
     * 简单类型数据获取缓存数据
     * @param key 键
     * @param clazz 数据类型
     * @return 返回缓存数据
     * @param <T> 数据类型
     */
    <T> T get(String key, Class<T> clazz);

    /**
     * 复杂类型数据获取缓存 eg: List<T>
     * @param key 键
     * @param typeReference 数据类型 new TypeReference<数据类型>() {}
     * @return 返回缓存数据
     * @param <T> 数据类型
     */
    <T> T get(String key, TypeReference<T> typeReference);

    /**
     * 删除缓存键
     * @param key 键
     */
    void delete(String key);

    /**
     *  包装缓存值递增
     * @param key 键
     * @return 返回递增后的值
     */
    Long increment(String key);
}
