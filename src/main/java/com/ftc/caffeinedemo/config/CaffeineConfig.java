package com.ftc.caffeinedemo.config;

import cn.hutool.core.date.DateUtil;
import com.ftc.caffeinedemo.entity.User;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author 冯铁城 [17615007230@163.com]
 * @date 2025-07-04 14:14:18
 * @describe Caffeine配置类
 */
@Component
public class CaffeineConfig {

    @Bean
    public LoadingCache<String, User> userCache() {
        return Caffeine.newBuilder()
                .refreshAfterWrite(3, TimeUnit.SECONDS)
                .removalListener((key, value, cause) -> System.out.println("key:" + key + " value:" + value + " cause:" + cause))
                .build(key -> {
                    TimeUnit.MILLISECONDS.sleep(10);
                    return new User("user" + key, DateUtil.now(), "user" + key, "China");
                });
    }
}
