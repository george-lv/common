package com.xinchang.common.cache;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ICache<K, V> {
    /**
     * 从缓存中获取一个对象，如果对象不存在则只返回null，不从server重新加载数据。
     * 
     * @param key
     * @return
     */
    public V getIfPresent(K key);

    /**
     * 判断一个key值是否存在
     */
    public boolean isExistent(K key);

    /**
     * 判断一个key值是否不存在
     */
    public boolean isNonExistent(K key);

    /**
     * 从缓存中获取一个value，如果不存在会返回null
     * 
     * @param key
     * @return
     */
    public V get(K key);

    public void delete(K key);

    public void deleteAll(Collection<K> keys);

    public void put(K key, V value);

    public Map<K, V> getAll(Collection<K> keys);

    public List<V> getAllForList(Collection<K> keys);

    /**
     * 原子性自增，使用redis实现，可以用来做一些全局的数量限制，每次加1
     * 注意：自增操作和get操作不能一起使用！
     * 
     * @param key
     * @param delta
     * @return
     */
    public Long atomicIncrement(K key);

    /**
     * 原子性自增，使用redis实现，可以用来做一些全局的数量限制，每次增加指定的值。
     * 注意：自增操作和get操作不能一起使用！
     * 
     * @param key
     * @param delta
     * @return
     */
    public Long atomicIncrement(K key, long delta);
}