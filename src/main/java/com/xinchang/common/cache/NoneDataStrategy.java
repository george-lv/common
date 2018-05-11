package com.xinchang.common.cache;

/**
 * 重新加载缓存而数据库中没有数据时，缓存要采用的策略
 *
 * @author lvziqiang
 */
public interface NoneDataStrategy {
    /**
     * 下次重试的时间间隔，0或负数表示不重试
     */
    long nextRetryInterval();
}