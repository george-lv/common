package com.xinchang.common.cache;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;

import com.xinchang.common.lock.DistributedLock;


public abstract class AbstractSortedSetCache<K, V extends Object> implements ISortedSetCache<K, V> {

    protected Logger        logger       = LoggerFactory.getLogger(this.getClass());

    @Resource
    private DistributedLock distributedLock;

    /**
     * 缓存key前缀和真实key之间的分隔符
     */
    private String          keySeparator = ":";

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

    private void tryReloadFromServer(K key, String realKey) {
        RedisTemplate<String, V> redisTemplate = getRedisTemplate();
        if (redisTemplate.hasKey(realKey))
            return;

        ZSetOperations<String, V> zsetOps = redisTemplate.opsForZSet();
        for (;;) {
            if (distributedLock.tryLock(realKey, 30, TimeUnit.SECONDS)) {
                try {
                    if (redisTemplate.hasKey(realKey)) {
                        return;
                    }

                    try {
                        Set<TypedTuple<V>> tuples = reloadFromServer(key, getReloadLimit());
                        if (CollectionUtils.isNotEmpty(tuples)) {
                            zsetOps.add(realKey, tuples);
                        }
                        else {
                            NoneDataStrategy noneDataStrategy = getNoneDataStrategy();
                            if (noneDataStrategy != null) {
                                zsetOps.add(realKey, newInvalidObject(), Integer.MIN_VALUE);
                                if (noneDataStrategy.nextRetryInterval() > 0L) {
                                    redisTemplate.expire(realKey,
                                        noneDataStrategy.nextRetryInterval(), TimeUnit.SECONDS);
                                }
                            }
                        }
                    }
                    catch (Throwable e) {
                        logger.error("reload data from server error,key = " + realKey);
                        return;
                    }
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
    protected abstract Set<TypedTuple<V>> reloadFromServer(K key, Long reloadLimit);

    /**
     * 设置缓存失效后，从数据库重新load数据到redis的条数限制，默认加载1000条
     */
    protected Long getReloadLimit() {
        return 1000L;
    }

    /**
     * 删除整个set
     */
    @Override
    public void delete(K key) {
        getRedisTemplate().delete(getRealKey(key));
    }

    /**
     * 返回zset长度
     */
    @Override
    public Long size(K key) {
        String realKey = getRealKey(key);
        this.tryReloadFromServer(key, realKey);
        return this.containsInvalidValue(realKey)
            ? getRedisTemplate().opsForZSet().size(realKey) - 1
            : getRedisTemplate().opsForZSet().size(realKey);
    }

    /**
     * 删除元素，返回实际删除的元素数量
     */
    @Override
    public Long remove(K key, Object... values) {
        String realKey = getRealKey(key);
        this.tryReloadFromServer(key, realKey);
        return getRedisTemplate().opsForZSet().remove(realKey, values);
    }

    /**
     * 向有序集合zset中增加一个元素，会覆盖元素的原分数。
     */
    @Override
    public Boolean add(K key, V value, double score) {
        String realKey = getRealKey(key);
        this.tryReloadFromServer(key, realKey);
        return getRedisTemplate().opsForZSet().add(realKey, value, score);
    }

    /**
     * 如果在名称为key的zset中已经存在元素value，则该元素的score增加delta；否则向集合中添加该元素，其score的值为delta
     */
    @Override
    public Double incrementScore(K key, V value, double delta) {
        String realKey = getRealKey(key);
        this.tryReloadFromServer(key, realKey);
        return getRedisTemplate().opsForZSet().incrementScore(realKey, value, delta);
    }

    /**
     * 返回名称为key的zset（元素已按score从小到大排序）中的index从start到end的所有元素
     */
    @Override
    public Set<V> range(K key, long offset, long limit) {
        long start = offset;
        long end = start + limit - 1;

        String realKey = getRealKey(key);
        this.tryReloadFromServer(key, realKey);
        if (this.containsInvalidValue(realKey)) {
            start = start + 1;
            end = end + 1;
        }

        return getRedisTemplate().opsForZSet().range(realKey, start, end);
    }

    /**
     * 返回名称为key的zset（元素已按score从大到小排序）中的index从start到end的所有元素
     */
    @Override
    public Set<V> reverseRange(K key, long offset, long limit) {
        long start = offset;
        long end = start + limit - 1;

        String realKey = getRealKey(key);
        this.tryReloadFromServer(key, realKey);
        Set<V> linkedHashSet = getRedisTemplate().opsForZSet().reverseRange(realKey, start, end);
        if (CollectionUtils.isNotEmpty(linkedHashSet)) {
            Iterator<V> iterator = linkedHashSet.iterator();
            while (iterator.hasNext()) {
                V value = iterator.next();
                if (isInvalidObject(value)) {
                    iterator.remove();
                }
            }
        }

        return linkedHashSet;
    }

    /**
     * 返回名称为key的zset（元素已按score从小到大排序）中的index从start到end的所有元素
     */
    @Override
    public Set<TypedTuple<V>> rangeWithScores(K key, long offset, long limit) {
        long start = offset;
        long end = start + limit - 1;

        String realKey = getRealKey(key);
        this.tryReloadFromServer(key, realKey);
        if (this.containsInvalidValue(realKey)) {
            start = start + 1;
            end = end + 1;
        }

        return getRedisTemplate().opsForZSet().rangeWithScores(realKey, start, end);
    }

    /**
     * 返回名称为key的zset（元素已按score从大到小排序）中的index从start到end的所有元素
     */
    @Override
    public Set<TypedTuple<V>> reverseRangeWithScores(K key, long offset, long limit) {
        long start = offset;
        long end = start + limit - 1;

        String realKey = getRealKey(key);
        this.tryReloadFromServer(key, realKey);

        Set<TypedTuple<V>> linkedHashSet = getRedisTemplate().opsForZSet()
            .reverseRangeWithScores(realKey, start, end);
        if (CollectionUtils.isNotEmpty(linkedHashSet)) {
            Iterator<TypedTuple<V>> iterator = linkedHashSet.iterator();
            while (iterator.hasNext()) {
                TypedTuple<V> typedTuple = iterator.next();
                if (isInvalidObject(typedTuple.getValue())) {
                    iterator.remove();
                }
            }
        }

        return linkedHashSet;
    }

    /**
     * 由于父类无法通过泛型创建对象，所以需要子类实现一个返回无效对象的方法，用在数据库中无数据时放入缓存中，减少反复查询数据库。
     * 该方法需要和isInvalidObject方法一起实现，告诉父类哪个特殊值被作为无效对象。
     */
    protected abstract V newInvalidObject();

    /**
     * 该方法需要和newInvalidObject方法一起实现，告诉父类哪个特殊值被作为无效对象。
     */
    protected abstract boolean isInvalidObject(V value);

    protected String getRealKey(K key) {
        return new StringBuilder(64).append(getKeyPrefix()).append(keySeparator).append(key)
            .toString();
    }

    protected NoneDataStrategy getNoneDataStrategy() {
        return null;
    }

    private boolean containsInvalidValue(String realKey) {
        Set<V> set = getRedisTemplate().opsForZSet().range(realKey, 0, 0);
        return CollectionUtils.isNotEmpty(set) && isInvalidObject(set.iterator().next());
    }
}