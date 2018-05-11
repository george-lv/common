package com.xinchang.common.cache;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public abstract class AbstractCache<K, V> implements ICache<K, V> {
    private long UNSET_INT = 0;

    @Resource
    private RedisConnectionFactory jedisConnectionFactory;

    /**
     * 缓存key前缀和真实key之间的分隔符
     */
    private String keySeparator = ":";

    private Map<String, RedisAtomicLong> redisCounterMap = new ConcurrentHashMap<>();

    protected RedisAtomicLong getRedisCounter(K key) {
        String realKey = this.getKeyPrefix() + "_COUNTER" + keySeparator + key;
        RedisAtomicLong redisCounter = redisCounterMap.get(realKey);
        if (redisCounter == null) {
            synchronized (redisCounterMap) {
                redisCounter = redisCounterMap.get(realKey);
                if (redisCounter == null) {
                    redisCounter = new RedisAtomicLong(realKey, jedisConnectionFactory);
                    redisCounterMap.put(realKey, redisCounter);
                }
            }
        }

        return redisCounter;
    }
    //重载方法,根据不同key值存储
    protected RedisAtomicLong getRedisCounter(K key,String keyFlag) {
        String realKey = this.getKeyPrefix() + keyFlag + keySeparator + key;
        RedisAtomicLong redisCounter = redisCounterMap.get(realKey);
        if (redisCounter == null) {
            synchronized (redisCounterMap) {
                redisCounter = redisCounterMap.get(realKey);
                if (redisCounter == null) {
                    redisCounter = new RedisAtomicLong(realKey, jedisConnectionFactory);
                    redisCounterMap.put(realKey, redisCounter);
                }
            }
        }
        return redisCounter;
    }

    private Transformer<K, String> OriginalKeyToRealKeyTransformer = new Transformer<K, String>() {
        public String transform(K originalKey) {
            return new StringBuilder(32).append(getKeyPrefix()).append(keySeparator)
                .append(originalKey).toString();
        }
    };

    private Transformer<String, K> RealKeyTOOriginalKeyTransformer = new Transformer<String, K>() {
        public K transform(String realKey) {
            return restoreToOrigKey(realKey);
        }
    };

    /* ==================================对外提供的api=============================================== */
    @Override
    public V getIfPresent(K key) {
        if (key == null) {
            return null;
        }

        V value = null;
        String realKey = getRealKey(key);
        if (isUseLocalCache()) {
            value = cache.getIfPresent(realKey);
        }

        if (value == null && isUseRedisCache()) {
            value = getRedisTemplate().opsForValue().get(realKey);
        }

        return value;
    }

    @Override
    public boolean isExistent(K key) {
        if (key == null) {
            return false;
        }

        if (isUseLocalCache()) {
            try {
                return cache.get(getRealKey(key)) != null;
            }
            catch (Exception e) {
                return false;
            }
        }
        else {
            String realKey = getRealKey(key);
            boolean result = getRedisTemplate().hasKey(realKey);
            if (!result) {
                V value = doLoadFromServer(realKey);
                if (value != null) {
                    setToRedis(realKey, value);
                    result = true;
                }
            }

            return result;
        }
    }

    @Override
    public boolean isNonExistent(K key) {
        return !isExistent(key);
    }

    @Override
    public V get(K key) {
        if (key == null) {
            return null;
        }

        V value = null;
        if (isUseLocalCache()) {
            try {
                value = cache.getUnchecked(getRealKey(key));
                return value;
            }
            catch (Exception e) {
            }
        }
        else {
            String realKey = getRealKey(key);
            value = getRedisTemplate().opsForValue().get(realKey);
            if (value == null) {
                value = doLoadFromServer(realKey);
                if (value != null) {
                    setToRedis(realKey, value);
                }
                else {
                    value = getRedisTemplate().opsForValue().get(realKey);
                }
            }
        }

        return value;
    }

    @Override
    public void delete(K key) {
        if (key == null) {
            return;
        }

        String realKey = getRealKey(key);
        if (isUseLocalCache()) {
            cache.invalidate(realKey);
        }

        if (isUseRedisCache()) {
            getRedisTemplate().delete(realKey);
        }
    }

    @Override
    public void deleteAll(Collection<K> keys) {
        if (CollectionUtils.isEmpty(keys))
            return;

        List<String> realKeys = (List<String>) CollectionUtils.collect(keys,
            OriginalKeyToRealKeyTransformer);

        if (isUseLocalCache()) {
            cache.invalidateAll(realKeys);
        }

        if (isUseRedisCache()) {
            getRedisTemplate().delete(realKeys);
        }
    }

    @Override
    public void put(K key, V value) {
        if (key == null) {
            return;
        }

        String realKey = getRealKey(key);

        if (isUseLocalCache()) {
            cache.put(realKey, value);
        }

        if (isUseRedisCache()) {
            setToRedis(realKey, value);
        }
    }

    @Override
    public List<V> getAllForList(Collection<K> keys) {
        Map<K, V> returnMap = getAll(keys);
        if (MapUtils.isNotEmpty(returnMap)) {
            List<V> returnList = new ArrayList<>(returnMap.size());
            for (K key : keys) {
                returnList.add(returnMap.get(key));
            }

            return returnList;
        }
        else {
            return new ArrayList<>();
        }
    }

    @Override
    public Map<K, V> getAll(Collection<K> keys) {
        if (CollectionUtils.isNotEmpty(keys)) {
            List<String> realKeys = (List<String>) CollectionUtils.collect(keys,
                OriginalKeyToRealKeyTransformer);

            Map<K, V> retMap = new HashMap<>(keys.size());
            if (isUseLocalCache()) {
                try {
                    Map<String, V> tempMap = cache.getAll(realKeys);
                    if (MapUtils.isNotEmpty(tempMap)) {
                        for (K key : keys) {
                            V value = tempMap.get(getRealKey(key));
                            if (value != null) {
                                retMap.put(key, value);
                            }
                        }
                    }

                    return retMap;
                }
                catch (Exception e) {
                }
            }
            else {
                List<V> values = getRedisTemplate().opsForValue().multiGet(realKeys);
                if (CollectionUtils.isNotEmpty(values)) {
                    for (int index = (realKeys.size() - 1); index >= 0; index--) {
                        String realKey = realKeys.get(index);
                        V value = values.get(index);
                        if (value != null) {
                            realKeys.remove(index);
                            retMap.put(restoreToOrigKey(realKey), value);
                        }
                    }
                }

                if (CollectionUtils.isNotEmpty(realKeys)) {
                    List<K> originalKeys = (ArrayList<K>) CollectionUtils.collect(realKeys,
                        RealKeyTOOriginalKeyTransformer);

                    Map<String, V> tempMap = doBatchLoadFromServer(originalKeys);
                    if (MapUtils.isNotEmpty(tempMap)) {
                        for (Entry<String, V> entry : tempMap.entrySet()) {
                            K key = restoreToOrigKey(entry.getKey());
                            retMap.put(key, entry.getValue());
                        }

                        setToRedis(tempMap);
                    }
                    else if (tempMap == null) {
                        values = getRedisTemplate().opsForValue().multiGet(realKeys);
                        if (CollectionUtils.isNotEmpty(values)) {
                            for (int index = (realKeys.size() - 1); index >= 0; index--) {
                                retMap.put(restoreToOrigKey(realKeys.get(index)),
                                    values.get(index));
                            }
                        }
                    }
                }

                return retMap;
            }
        }

        return new HashMap<>();
    }

    /**
     * 原子性自增，使用redis实现，可以用来做一些全局的数量限制，每次加1
     * 
     * @param key
     * @param delta
     * @return
     */
    @Override
    public Long atomicIncrement(K key) {
        if (!isUseRedisCache()) {
            throw new UnsupportedOperationException();
        }

        return getRedisTemplate().opsForValue().increment(getRealKey(key), 1L);
    }

    
    /**
     * 原子性自增，使用redis实现，可以用来做一些全局的数量限制，每次增加指定的值。
     * 
     * @param key
     * @param delta
     * @return
     */
    @Override
    public Long atomicIncrement(K key, long delta) {
        if (!isUseRedisCache()) {
            throw new UnsupportedOperationException();
        }

        return getRedisTemplate().opsForValue().increment(getRealKey(key), delta);
    }
    /* ==================================对外提供的api=============================================== */

    /**
     * 获取操作redis缓存的对象，子类实现，如果不使用redis缓存，返回null即可。
     * @return
     */
    protected abstract RedisTemplate<String, V> getRedisTemplate();

    /**
     * 获取缓存key前缀，子类实现。
     * @return
     */
    protected abstract String getKeyPrefix();

    /**
     * 获取本地缓存最大容量，子类实现，如果不想使用本地缓存，返回0即可。
     * @return
     */
    protected abstract int getMaximumSize();

    /**
     * 将string类型的key转回原始类型的key，子类实现
     * @param strKey
     * @return
     */
    protected abstract K restoreToOriginalKey(String strKey);

    /**
     * 可以通过重写这个方法来设置redis缓存的失效时间，单位是秒
     * 
     * @param strKey
     * @return
     */
    protected long redisExpireDuration() {
        return UNSET_INT;
    }

    /**
     * 可以通过重写这个方法来设置redis失效时间点
     */
    protected Date redisExpireAt() {
        return null;
    }

    /**
     * 从server读取单个对象，子类实现，沒有就返回null
     * @param key
     * @return
     */
    protected V loadFromServer(K key) {
        return null;
    }

    /**
     * 从server读取多个对象，子类实现，沒有就返回null
     * @param keys
     * @return
     */
    protected Map<K, V> batchLoadFromServer(List<K> keys) {
        return null;
    }

    /**
     * 可以通过重写这个方法来设置数据在写入本地缓存后多久需要做一次刷新。
     * 刷新是异步执行的。
     * 
     * @return
     */
    public long refreshAfterWriteDuration() {
        return UNSET_INT;
    }

    private K restoreToOrigKey(String realKey) {
        return restoreToOriginalKey(StringUtils.substringAfterLast(realKey, keySeparator));
    }

    protected V doLoadFromServer(String key) {
        return loadFromServer(restoreToOrigKey(key));
    }

    protected Map<String, V> doBatchLoadFromServer(List<K> keys) {
        Map<K, V> tempMap = batchLoadFromServer(keys);
        Map<String, V> retMap = null;
        if (MapUtils.isNotEmpty(tempMap)) {
            retMap = new HashMap<>(tempMap.size());
            for (Entry<K, V> entry : tempMap.entrySet()) {
                if (entry.getValue() != null)
                    retMap.put(getRealKey(entry.getKey()), entry.getValue());
            }
        }

        return retMap;
    }

    protected LoadingCache<String, V> cache;

    protected String getRealKey(K key) {
        return getKeyPrefix() + keySeparator + key;
    }

    private boolean isUseLocalCache() {
        return getMaximumSize() > 0;
    }

    private boolean isUseRedisCache() {
        return getRedisTemplate() != null;
    }

    @PostConstruct
    private void initCache() {
        if (!isUseLocalCache())
            return;

        CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder();
        if (refreshAfterWriteDuration() > UNSET_INT) {
            cacheBuilder.refreshAfterWrite(refreshAfterWriteDuration(), TimeUnit.SECONDS);
        }

        cache = cacheBuilder.maximumSize(getMaximumSize()).build(new CacheLoader<String, V>() {
            @Override
            public V load(String key) throws Exception {
                V value = null;
                if (isUseRedisCache()) {
                    value = getRedisTemplate().opsForValue().get(key);
                }

                if (value == null) {
                    value = doLoadFromServer(key);

                    if (value != null && isUseRedisCache()) {
                        setToRedis(key, value);
                    }
                }

                return value;
            }

            @Override
            public Map<String, V> loadAll(Iterable<? extends String> keys) throws Exception {
                List<String> realKeys = new ArrayList<>();
                CollectionUtils.addAll(realKeys, keys);

                Map<String, V> retMap = new HashMap<>(realKeys.size());

                if (isUseRedisCache()) {
                    List<V> values = getRedisTemplate().opsForValue().multiGet(realKeys);
                    if (CollectionUtils.isNotEmpty(values)) {
                        for (int index = (realKeys.size() - 1); index >= 0; index--) {
                            String key = realKeys.get(index);
                            V value = values.get(index);
                            if (value != null) {
                                realKeys.remove(index);
                                retMap.put(key, value);
                            }
                        }
                    }
                }

                if (CollectionUtils.isNotEmpty(realKeys)) {
                    List<K> originalKeys = (ArrayList<K>) CollectionUtils.collect(realKeys,
                        RealKeyTOOriginalKeyTransformer);

                    Map<String, V> tempMap = doBatchLoadFromServer(originalKeys);
                    if (MapUtils.isNotEmpty(tempMap)) {
                        retMap.putAll(tempMap);

                        if (isUseRedisCache()) {
                            setToRedis(tempMap);
                        }
                    }
                }

                return retMap;
            }
        });
    }

    private void setToRedis(String key, V value) {
        if (redisExpireDuration() > UNSET_INT) {
            getRedisTemplate().opsForValue().set(key, value, redisExpireDuration(),
                TimeUnit.SECONDS);
        }
        else {
            getRedisTemplate().opsForValue().set(key, value);
        }
    }

    private void setToRedis(Map<String, V> entryMap) {
        if (redisExpireDuration() > UNSET_INT) {
            for (Entry<String, V> entry : entryMap.entrySet()) {
                getRedisTemplate().opsForValue().set(entry.getKey(), entry.getValue(),
                    redisExpireDuration(), TimeUnit.SECONDS);
            }
        }
        else if (redisExpireAt() != null) {
            for (Entry<String, V> entry : entryMap.entrySet()) {
                getRedisTemplate().opsForValue().set(entry.getKey(), entry.getValue(),
                    redisExpireAt().getTime() - new Date().getTime(), TimeUnit.MILLISECONDS);
            }
        }
        else {
            getRedisTemplate().opsForValue().multiSet(entryMap);
        }
    }
}