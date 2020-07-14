package com.imooc.miaoshaproject.service;

/**
 * @description : 适应guava实现热点数据的本地缓存
 * @since : 10.7.0
 */
public interface CommentCacheService {
    /**
     * 获取缓存数据
     * @param key 键
     * @return
     */
    public Object getCache(String key);

    /**
     * 存储缓存数据
     * @param key 对应的key
     * @param value 对应的值
     */
    public void setCache(String key,Object value);
}
