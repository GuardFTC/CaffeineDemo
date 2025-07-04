package com.ftc.caffeinedemo;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author 冯铁城 [17615007230@163.com]
 * @date 2025-07-01 11:51:46
 * @describe Caffeine测试类
 */
public class CaffeineTest {

    @Test
    public void testCaffeineBase() {

        //1.构建缓存
        Cache<String, String> cache = Caffeine.newBuilder().build();

        //2.手动写入缓存
        cache.put("k1", "v1");

        //3.手动获取缓存
        String value = cache.getIfPresent("k1");
        Assert.isTrue("v1".equals(value));

        //4.手动清除缓存
        cache.invalidate("k1");
        value = cache.getIfPresent("k1");
        Assert.isTrue(StrUtil.isBlank(value));
    }

    @Test
    public void testCaffeineBaseMulti() {

        //1.构建缓存
        Cache<String, String> cache = Caffeine.newBuilder().build();

        //2.手动写入缓存
        for (int i = 1; i <= 10; i++) {
            cache.put("k" + i, "v" + i);
        }

        //3.手动获取缓存
        Map<String, String> values = cache.getAllPresent(List.of("k1", "k2", "k3", "k4", "k5", "k6", "k7", "k8", "k9", "k10"));
        Assert.isTrue(10 == values.size());
        for (int i = 1; i <= 10; i++) {
            Assert.isTrue(("v" + i).equals(values.get(("k" + i))));
        }

        //4.手动清除缓存
        cache.invalidateAll();
        values = cache.getAllPresent(List.of("k1", "k2", "k3", "k4", "k5", "k6", "k7", "k8", "k9", "k10"));
        Assert.isTrue(values.isEmpty());
    }

    @Test
    @SneakyThrows(value = {InterruptedException.class})
    public void testCaffeineMaxSize() {

        //1.构建缓存
        Cache<String, String> cache = Caffeine.newBuilder()
                .maximumSize(1)
                .build();

        //2.写入缓存,验证直接put写入不会立即触发maxSize检查
        cache.put("k1", "v1");
        cache.put("k2", "v2");
        Assert.isTrue(2 == cache.asMap().size());

        //3.清空缓存
        cache.invalidateAll();
        Assert.isTrue(cache.asMap().isEmpty());

        //4.通过get形式再次获取
        String value = cache.get("k1", v -> "v1");
        Assert.isTrue("v1".equals(value));
        Assert.isTrue(1 == cache.asMap().size());
        Assert.isTrue(cache.asMap().containsKey("k1"));

        //5.写入新值
        value = cache.get("k2", v -> "v2");
        Assert.isTrue("v2".equals(value));

        //6.睡1ms，确保清除策略执行成功
        TimeUnit.MILLISECONDS.sleep(1);

        //7.再次验证
        Assert.isTrue(1 == cache.asMap().size());
        Assert.isTrue(cache.asMap().containsKey("k2"));
        Assert.isTrue(!cache.asMap().containsKey("k1"));
    }

    @Test
    @SneakyThrows(value = {InterruptedException.class})
    public void testCaffeineMaxSizeWeight() {

        //1.构建缓存
        Cache<String, String> cache = Caffeine.newBuilder()
                .maximumWeight(2)
                .weigher((key, value) -> value.toString().length())
                .build();

        //2.通过get形式取值
        String value = cache.get("k1", v -> "v1");
        Assert.isTrue("v1".equals(value));
        Assert.isTrue(1 == cache.asMap().size());
        Assert.isTrue(cache.asMap().containsKey("k1"));

        //3.写入新值
        value = cache.get("k2", v -> "v2222");
        Assert.isTrue("v2222".equals(value));

        //6.睡1ms，确保清除策略执行成功
        TimeUnit.MILLISECONDS.sleep(1);

        //7.再次验证
        Assert.isTrue(1 == cache.asMap().size());
        Assert.isTrue(cache.asMap().containsKey("k1"));
        Assert.isTrue(!cache.asMap().containsKey("k2"));
    }

    @Test
    @SneakyThrows(value = {InterruptedException.class})
    public void testCaffeineExpire() {

        //1.构建缓存
        Cache<String, String> cache = Caffeine.newBuilder()
                .expireAfterWrite(4, TimeUnit.SECONDS)                      // 写入后 4s过期
                .expireAfterAccess(2, TimeUnit.SECONDS)                     // 最后访问后 2s过期
                .removalListener((key, value, cause) ->   // 移除监听器
                        System.out.println("被移除 -> " + key + ", 原因: " + cause + ", 时间为:" + DateUtil.now())
                )
                .build();

        //2.写入缓存
        cache.put("k1", "v1");
        System.out.println("写入缓存:k1=v1" + "时间为:" + DateUtil.now());

        //3.查询一次缓存
        String value = cache.getIfPresent("k1");
        System.out.println("从缓存获取:" + value);
        Assert.isTrue("v1".equals(value));

        //4.睡2s，确保触发expireAfterAccess
        TimeUnit.SECONDS.sleep(2);

        //5.再次查询
        value = cache.getIfPresent("k1");
        System.out.println("从缓存获取:" + value);
        Assert.isTrue(value == null);

        //6.再次写入缓存
        cache.put("k2", "v2");
        System.out.println("写入缓存:k2=v2" + "时间为:" + DateUtil.now());

        //7.创建线程循环查询,确保触发expireAfterWrite
        new Thread(() -> {
            while (true) {
                cache.getIfPresent("k2");
            }
        }).start();

        //8.睡4s
        TimeUnit.SECONDS.sleep(4);

        //9.再次查询
        value = cache.getIfPresent("k2");
        System.out.println("从缓存获取:" + value);
        Assert.isTrue(value == null);
    }

    @Test
    @SneakyThrows(value = {InterruptedException.class})
    public void testCaffeineLoad() {

        //1.构建缓存
        LoadingCache<Object, String> loadingCache = Caffeine.newBuilder()
                .removalListener((key, value, cause) -> System.out.println("被移除 -> " + key + ", 原因: " + cause))
                .refreshAfterWrite(2, TimeUnit.SECONDS)     //被查询命中时，如果距离上一次刷新/初次加载的时间超过2s，触发build的刷新逻辑
                .build(key -> {
                    TimeUnit.MILLISECONDS.sleep(10);
                    return "value:" + key + ":" + DateUtil.now();
                });

        //2.查询不存在的key
        String value = loadingCache.get("k2");
        System.out.println("从缓存获取:" + value);
        Assert.isTrue(value.startsWith("value:k2"));

        //3.睡3s
        TimeUnit.SECONDS.sleep(3);

        //4.再次查询,触发刷新逻辑，refreshAfterWrite为非阻塞式刷新，因此本次会返回旧值
        String value2 = loadingCache.get("k2");
        System.out.println("从缓存获取:" + value2);
        Assert.isTrue(value.startsWith("value:k2"));
        Assert.isTrue(value.equals(value2));

        //5.睡100ms,等待刷新完成
        TimeUnit.MILLISECONDS.sleep(100);

        //6.再次查询，因为刷新已经结束，因此会返回新的值
        String value3 = loadingCache.get("k2");
        System.out.println("从缓存获取:" + value3);
        Assert.isTrue(value3.startsWith("value:k2"));
        Assert.isTrue(!value.equals(value3));
    }

    @Test
    @SneakyThrows(value = {InterruptedException.class})
    public void testCaffeineLoadAsync() {

        //1.构建缓存
        AsyncLoadingCache<Object, String> asyncLoadingCache = Caffeine.newBuilder()
                .removalListener((key, value, cause) -> System.out.println("被移除 -> " + key + ", 原因: " + cause))
                .refreshAfterWrite(2, TimeUnit.SECONDS)
                .buildAsync((key, executor) -> CompletableFuture.supplyAsync(() -> "value:" + key + ":" + DateUtil.now(), executor));

        //2.查询不存在的key
        CompletableFuture<String> k2Future = asyncLoadingCache.get("k2");

        //3.睡3s
        TimeUnit.SECONDS.sleep(3);

        //4.再次查询
        CompletableFuture<String> newK2Future = asyncLoadingCache.get("k2");

        //5.串联两个CompletableFuture，同步回调
        newK2Future.thenAcceptBoth(k2Future, (newK2Value, k2Value) -> {
            System.out.println("从缓存获取:" + k2Value);
            System.out.println("从缓存获取:" + newK2Value);
            Assert.isTrue(k2Value.startsWith("value:k2"));
            Assert.isTrue(newK2Value.startsWith("value:k2"));
            Assert.isTrue(!k2Value.equals(newK2Value));
        }).join();
    }

    @Test
    public void testCaffeineRecordStats() {

        //1.构建缓存
        LoadingCache<String, Object> cache = Caffeine.newBuilder()
                .recordStats()
                .build(k -> k + "v");

        //2.写入数据
        cache.put("k1", "v1");

        //3.读取10次数据
        for (int i = 0; i < 10; i++) {
            cache.getIfPresent("k1");
        }

        //4.通过load形式再次获取数据
        String k2 = Objects.requireNonNull(cache.get("k2")).toString();
        Assert.isTrue(k2.equals("k2v"));

        //5.输出统计信息
        System.out.println("----------------------------------------------");
        System.out.println(cache.stats());
        Assert.isTrue(cache.stats().hitCount() == 10);
        Assert.isTrue(cache.stats().missCount() == 1);
        Assert.isTrue(cache.stats().loadSuccessCount() == 1);
    }
}