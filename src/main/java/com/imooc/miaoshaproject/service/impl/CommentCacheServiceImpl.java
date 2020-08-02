package com.imooc.miaoshaproject.service.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.imooc.miaoshaproject.service.CommentCacheService;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * @description :
 * @since : 10.7.0
 */
@Service
public class CommentCacheServiceImpl implements CommentCacheService {
    private Cache<String,Object> cache=null;

@PostConstruct
public void init(){
        //定义缓存容器
    cache = CacheBuilder.newBuilder()
                //设置容器初始化大小
                .initialCapacity(10)
                //设置容器最大容量为100，超过100则会按照LRU策略进行移除缓存项
                .maximumSize(100)
                //设置过期策略（比较点分为读和写）
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .build();
    }
    /**
     * 获取缓存数据
     *
     * @param key 键
     * @return
     */
    @Override
    public Object getCache(String key) {
        return cache.getIfPresent(key);
    }
    /**
     * 存储缓存数据
     *
     * @param key   对应的key
     * @param value 对应的值
     */
    @Override
    public void setCache(String key, Object value) {
        cache.put(key, value);
    }
}
