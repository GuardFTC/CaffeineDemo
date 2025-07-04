package com.ftc.caffeinedemo.config;

import com.ftc.caffeinedemo.entity.User;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

@SpringBootTest
class CaffeineConfigTest {

    @Autowired
    private LoadingCache<String, User> userCache;

    @Test
    @SneakyThrows(value = {InterruptedException.class})
    public void test() {

        //1.首次查询
        User user1 = userCache.get("1");
        System.out.println(user1);

        //2.睡3s，再次查询，触发刷新逻辑
        TimeUnit.SECONDS.sleep(3);

        //3.获取缓存数据,此时返回旧数据,但是后台会进行刷新逻辑更新数据
        User user2 = userCache.get("1");
        System.out.println(user2);

        //4.睡100ms，等待刷新结束，数据更新成功
        TimeUnit.MILLISECONDS.sleep(100);

        //5.再次查询数据，此时返回新数据
        User user3 = userCache.get("1");
        System.out.println(user3);
    }
}