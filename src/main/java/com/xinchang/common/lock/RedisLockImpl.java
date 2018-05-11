package com.xinchang.common.lock;

import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.springframework.data.redis.core.RedisTemplate;

/**
 * 用redis实现的一个简单分布式锁
 *
 * @author lvziqiang
 * @since $Revision:1.0.0, $Date: 2016年1月24日 下午3:44:48 $
 */
public class RedisLockImpl implements DistributedLock {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private Object                        lockValue = new Object();


    @Override
    public boolean tryLock(String key, long lockTimeout, TimeUnit unit) {
        String realKey = CACHE_PREFIX + key;
        return redisTemplate.opsForValue().setIfAbsent(realKey, lockValue)
               && redisTemplate.expire(realKey, lockTimeout, unit);
    }

    @Override
    public void unlock(String key) {
        redisTemplate.delete(CACHE_PREFIX + key);
    }
}
