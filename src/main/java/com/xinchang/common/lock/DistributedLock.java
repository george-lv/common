package com.xinchang.common.lock;

import java.util.concurrent.TimeUnit;

public interface DistributedLock {
    public static final String CACHE_PREFIX = "DISTRIBUTED_LOCK_";

    /**
     * 尝试获取一个分布式锁，获取失败后立即返回false。
     * 一般只能用来做幂等性校验，比如对ons的消息做去重。
     * 
     * @param key
     * @param lockTimeout 锁的失效时间
     * @param unit
     * @return
     */
    public boolean tryLock(String key, long lockTimeout, TimeUnit unit);

    /**
     * 释放锁
     * 
     * @param key
     */
    public void unlock(String key);
}
