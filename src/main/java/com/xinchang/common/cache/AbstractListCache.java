package com.xinchang.common.cache;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import com.xinchang.common.lock.DistributedLock;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public abstract class AbstractListCache<K, V> implements IListCache<K, V> {

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

    /**
     * redis缓存失效后，调用该方法从server重新load数据
     * 
     * @return
     */
    protected abstract Collection<V> reloadFromServer(K key, Long reloadLimit);

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
     * 是否异步从server加载数据
     */
    protected boolean loadAsync() {
        return false;
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

    /**
     * 1、在redis指定Key所关联的List的头部插入参数中给出的Value。
     * 2、如果该Key不存在，将在插入redis之前创建一个与该Key关联的空链表，之后再将数据从链表的头部插入。
     * 3、返回的当前数组的长度。
     */
    @Override
    public Long leftPush(K key, V value) {
        String realKey = getRealKey(key);
        tryReloadFromServer(key, realKey);

        return getRedisTemplate().opsForList().leftPush(realKey, value);
    }

    @Override
    public Long leftPushAll(K key, Collection<V> values) {
        String realKey = getRealKey(key);
        return getRedisTemplate().opsForList().leftPushAll(realKey, values);
    }

    @Override
    public void delete(K key) {
        getRedisTemplate().delete(getRealKey(key));
    }

    @Override
    public void delete(K key, V value) {
        getRedisTemplate().opsForList().remove(getRealKey(key), 0, value);
    }

    @Override
    public Long size(K key) {
        String realKey = getRealKey(key);
        tryReloadFromServer(key, realKey);

        Long size = getRedisTemplate().opsForList().size(realKey);
        return containsInvalidValue(realKey) ? (size - 1) : size;
    }

    /**
     * 从链表头部offset指定的位置开始，截取limit指定的记录条数。
     * 如果子类确定不会包含无效参数，可以通过重写这个方法提高性能。
     */
    @Override
    public List<V> leftRange(K key, long offset, long limit) {
        if (offset < 0 || limit < 1) {
            throw new IllegalArgumentException();
        }

        long start = offset;
        long end = start + limit - 1;

        String realKey = getRealKey(key);
        tryReloadFromServer(key, realKey);

        List<V> resultList = getRedisTemplate().opsForList().range(realKey, start, end);
        if (CollectionUtils.isNotEmpty(resultList)) {
            int lastIndex = resultList.size() - 1;
            V lastValue = resultList.get(lastIndex);
            if (isInvalidObject(lastValue)) {
                resultList.remove(lastIndex);
            }
        }

        return resultList;
    }

    /**
     * 从链表尾部offset指定的位置开始，截取limit指定的记录条数
     */
    @Override
    public List<V> rightRange(K key, long offset, long limit) {
        if (offset < 0 || limit < 1) {
            throw new IllegalArgumentException();
        }

        long start = -(offset + limit);
        long end = -offset - 1;

        String realKey = getRealKey(key);
        tryReloadFromServer(key, realKey);

        if (containsInvalidValue(realKey)) {
            start -= 1;
            end -= 1;
        }

        List<V> resultList = getRedisTemplate().opsForList().range(realKey, start, end);
        if (CollectionUtils.isNotEmpty(resultList)) {
            Collections.reverse(resultList);
        }

        return resultList;
    }

    private boolean containsInvalidValue(String realKey) {
        return isInvalidObject(getRedisTemplate().opsForList().index(realKey, -1));//取最后一个元素进行无效参数校验
    }

    private void tryReloadFromServer(final K key, final String realKey) {
        final RedisTemplate<String, V> redisTemplate = getRedisTemplate();
        if (redisTemplate.hasKey(realKey))
            return;

        if (this.loadAsync()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    ListOperations<String, V> listOps = redisTemplate.opsForList();
                    if (distributedLock.tryLock(realKey, 30, TimeUnit.SECONDS)) {
                        if (redisTemplate.hasKey(realKey)) {
                            return;
                        }

                        try {
                            Collection<V> reloadedValues = null;
                            try {
                                reloadedValues = reloadFromServer(key, getReloadLimit());
                            }
                            catch (Throwable e) {
                                logger.error("reload data from server error,key = " + realKey, e);

                                DBErrorStrategy dbErrorStrategy;
                                if ((dbErrorStrategy = getDBErrorStrategy()) != null) {
                                    long retryTimes = dbErrorStrategy.retryTimes();
                                    boolean repaired = false;
                                    for (long i = 0; i < retryTimes; i++) {
                                        try {
                                            reloadedValues = reloadFromServer(key,
                                                getReloadLimit());
                                            repaired = true;
                                        }
                                        catch (Throwable e1) {
                                            logger.error(
                                                "reload data from server error,key = {},retryTimes = {}",
                                                realKey, i + 1);
                                            logger.error(e1.getMessage(), e1);
                                        }

                                        if (repaired)
                                            break;
                                    }

                                    // 经过重试，数据库未恢复，则在缓存中放入无效参数，到了时间间隔后失效重试
                                    if (!repaired) {
                                        listOps.rightPush(realKey, newInvalidObject());
                                        if (dbErrorStrategy.nextRetryInterval() > 0) {
                                            redisTemplate.expire(realKey,
                                                dbErrorStrategy.nextRetryInterval(),
                                                TimeUnit.SECONDS);
                                        }
                                    }
                                }

                                return;
                            }

                            NoneDataStrategy noneDataStrategy;
                            if (CollectionUtils.isNotEmpty(reloadedValues)) {
                                listOps.rightPushAll(realKey, reloadedValues);
                                Date expireAt = expireAt();
                                if (expireAt != null)
                                    redisTemplate.expireAt(realKey, expireAt);
                                afterPutValueInCacheHook(realKey, key);
                            }
                            else if ((noneDataStrategy = getNoneDataStrategy()) != null) {
                                // 数据库中无数据，则在缓存中放入无效参数，再根据策略设置失效时间
                                listOps.rightPush(realKey, newInvalidObject());
                                if (noneDataStrategy.nextRetryInterval() > 0) {
                                    redisTemplate.expire(realKey,
                                        noneDataStrategy.nextRetryInterval(), TimeUnit.SECONDS);
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
                }
            }).start();
        }
        else {
            ListOperations<String, V> listOps = redisTemplate.opsForList();
            for (;;) {
                if (distributedLock.tryLock(realKey, 30, TimeUnit.SECONDS)) {
                    if (redisTemplate.hasKey(realKey)) {
                        return;
                    }

                    try {
                        Collection<V> reloadedValues = null;
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
                                    listOps.rightPush(realKey, newInvalidObject());
                                    if (dbErrorStrategy.nextRetryInterval() > 0) {
                                        redisTemplate.expire(realKey,
                                            dbErrorStrategy.nextRetryInterval(), TimeUnit.SECONDS);
                                    }
                                }
                            }

                            return;
                        }

                        NoneDataStrategy noneDataStrategy;
                        if (CollectionUtils.isNotEmpty(reloadedValues)) {
                            listOps.rightPushAll(realKey, reloadedValues);
                            Date expireAt = expireAt();
                            if (expireAt != null)
                                redisTemplate.expireAt(realKey, expireAt);
                            afterPutValueInCacheHook(realKey, key);
                        }
                        else if ((noneDataStrategy = getNoneDataStrategy()) != null) {
                            // 数据库中无数据，则在缓存中放入无效参数，再根据策略设置失效时间
                            listOps.rightPush(realKey, newInvalidObject());
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
    }

    /**
     * 该方法会在对象被放入缓存后执行，用来进行一些特殊处理
     * @param key
     */
    protected void afterPutValueInCacheHook(String realKey, K key) {
    }

    protected String getRealKey(K key) {
        return new StringBuilder(64).append(getKeyPrefix()).append(keySeparator).append(key)
            .toString();
    }
}