package com.imooc.miaoshaproject.config;

import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.stereotype.Component;

/**
 * @description : 配置redis的个性化参数
 * @since : 10.7.0
 */
@Component
/* 会话时长默认是1800（30分钟），可更改为3600（一个小时） */
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 3600)
public class RedisConfig {
}
