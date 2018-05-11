package com.xinchang.common.cache;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import com.xinchang.common.lock.DistributedLock;




public abstract class AbstractSetCache<K, V extends Object> implements ISetCache<K, V> {

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource
    private DistributedLock distributedLock;

    /**
     * 缓存key前缀和真实key之间的分隔符
     */
    private String keySeparator = ":";

    /**
     * 获取操作redis缓存的对象，子类实现。
     * @return
     */
    protected abstract RedisTemplate<String, V> getRedisTemplate();

    /**
     * 获取缓存key前缀，子类实现。
     * @return
     */
    protected abstract String getKeyPrefix();

    @SuppressWarnings("unchecked")
    private void tryReloadFromServer(K key, String realKey) {
        RedisTemplate<String, V> redisTemplate = getRedisTemplate();
        if (redisTemplate.hasKey(realKey))
            return;

        SetOperations<String, V> setOps = redisTemplate.opsForSet();
        for (;;) {
            if (distributedLock.tryLock(realKey, 30, TimeUnit.SECONDS)) {
                try {
                    if (redisTemplate.hasKey(realKey)) {
                        return;
                    }

                    V[] reloadedValues = null;
                    try {
                        reloadedValues = reloadFromServer(key, getReloadLimit());
                    }
                    catch (Throwable e) {
                        logger.error("reload data from server error,key = " + realKey);

                        DBErrorStrategy dbErrorStrategy;
                        if ((dbErrorStrategy = getDBErrorStrategy()) != null) {
                            long retryTimes = dbErrorStrategy.retryTimes();
                            boolean repaired = false;
                            for (long i = 0; i < retryTimes; i++) {
                                try {
                                    reloadedValues = reloadFromServer(key, getReloadLimit());
                                    repaired = true;
                                }
                                catch (Throwable e1) {
                                    logger.error(
                                        "reload data from server error,key = {},retryTimes = {}",
                                        realKey, i + 1);
                                }

                                if (repaired)
                                    break;
                            }

                            // 经过重试，数据库未恢复，则在缓存中放入无效参数，到了时间间隔后失效重试
                            if (!repaired) {
                                setOps.add(realKey, newInvalidObject());
                                if (dbErrorStrategy.nextRetryInterval() > 0) {
                                    redisTemplate.expire(realKey,
                                        dbErrorStrategy.nextRetryInterval(), TimeUnit.SECONDS);
                                }
                            }
                        }

                        return;
                    }

                    NoneDataStrategy noneDataStrategy;
                    if (ArrayUtils.isNotEmpty(reloadedValues)) {
                        redisTemplate.opsForSet().add(realKey, reloadedValues);
                        Date expireAt = expireAt();
                        if (expireAt != null)
                            redisTemplate.expireAt(realKey, expireAt);
                    }
                    else if ((noneDataStrategy = getNoneDataStrategy()) != null) {
                        redisTemplate.opsForSet().add(realKey, newInvalidObject());
                        if (noneDataStrategy.nextRetryInterval() > 0) {
                            redisTemplate.expire(realKey, noneDataStrategy.nextRetryInterval(),
                                TimeUnit.SECONDS);
                        }
                    }
                    else {
                        // 没有设置无效参数缓存失效策略，下次还是会读取数据库
                    }

                    return;
                }
                finally {
                    distributedLock.unlock(realKey);
                }
            }
            else {
                try {
                    Thread.sleep(10);
                }
                catch (Throwable e) {
                }

                if (redisTemplate.hasKey(realKey)) {
                    return;
                }
            }
        }
    }

    /**
     * redis缓存失效后，调用该方法从server重新load数据
     * 
     * @return
     */
    protected abstract V[] reloadFromServer(K key, Long reloadLimit);

    /**
     * 设置缓存失效后，从数据库重新load数据到redis的条数限制，默认加载1000条
     */
    protected Long getReloadLimit() {
        return 1000L;
    }

    /**
     * 针对于数据是从数据库中load进缓存，而不是通过leftPush方法进入缓存的情况，一般要重写该方法，告诉父类缓存在哪个时间点失效。
     * 比如用来保存某一段时间内数据库排序结果快照的时候。
     * 默认实现是返回null，表示不失效。
     */
    protected Date expireAt() {
        return null;
    }

    /**
     * 由于父类无法通过泛型创建对象，所以需要子类实现一个返回无效对象的方法，用在数据库中无数据时放入缓存中，减少反复查询数据库。
     */
    protected abstract V newInvalidObject();

    @Override
    public Long size(K key) {
        String realKey = getRealKey(key);
        this.tryReloadFromServer(key, realKey);

        return this.containsInvalidValue(realKey)
            ? (getRedisTemplate().opsForSet().size(realKey) - 1)
            : getRedisTemplate().opsForSet().size(realKey);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void add(K key, V value) {
        String realKey = getRealKey(key);
        this.tryReloadFromServer(key, realKey);

        getRedisTemplate().opsForSet().add(realKey, value);
    }

    @Override
    public Boolean isMember(K key, V value) {
        String realKey = getRealKey(key);
        this.tryReloadFromServer(key, realKey);

        return getRedisTemplate().opsForSet().isMember(realKey, value);
    }

    @Override
    public void remove(K key, V value) {
        String realKey = getRealKey(key);
        this.tryReloadFromServer(key, realKey);

        getRedisTemplate().opsForSet().remove(realKey, value);
    }

    @Override
    public Set<V> distinctRandomMembers(K key, long count) {
        String realKey = getRealKey(key);
        this.tryReloadFromServer(key, realKey);

        Set<V> resultSet = getRedisTemplate().opsForSet().distinctRandomMembers(realKey, count);
        if (resultSet != null) {
            resultSet.remove(this.newInvalidObject());
        }

        return resultSet;
    }

    @Override
    public Set<V> members(K key) {
        String realKey = getRealKey(key);
        this.tryReloadFromServer(key, realKey);
        Set<V> members = getRedisTemplate().opsForSet().members(realKey);
        if (members != null) {
            members.remove(this.newInvalidObject());
        }

        return members;
    }

    private boolean containsInvalidValue(String realKey) {
        return getRedisTemplate().opsForSet().isMember(realKey, newInvalidObject());
    }

    private String getRealKey(K key) {
        return new StringBuilder(64).append(getKeyPrefix()).append(keySeparator).append(key)
            .toString();
    }
}