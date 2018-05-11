package com.xinchang.common.cache;

import java.util.Set;

import org.springframework.data.redis.core.ZSetOperations.TypedTuple;

public interface ISortedSetCache<K, V> {
    /**
     * 返回set长度
     */
    public Long size(K key);

    /**
     * 删除某一个元素
     */
    Long remove(K key, Object... values);

    /**
     * 向有序集合zset中增加一个元素。
     */
    Boolean add(K key, V value, double score);

    /**
     * 如果在名称为key的zset中已经存在元素value，则该元素的score增加delta；否则向集合中添加该元素，其score的值为delta
     */
    Double incrementScore(K key, V value, double delta);

    /**
     * 返回名称为key的zset（元素已按score从小到大排序）中的index从offset开始的limit个元素
     */
    Set<V> range(K key, long offset, long limit);

    /**
     * 返回名称为key的zset（元素已按score从大到小排序）中的index从offset开始的limit个元素
     */
    Set<V> reverseRange(K key, long offset, long limit);

    /**
     * 返回名称为key的zset（元素已按score从小到大排序）中的index从offset开始的limit个元素
     */
    Set<TypedTuple<V>> rangeWithScores(K key, long offset, long limit);

    /**
     * 返回名称为key的zset（元素已按score从大到小排序）中的index从offset开始的limit个元素
     */
    Set<TypedTuple<V>> reverseRangeWithScores(K key, long offset, long limit);

    /**
     * 删除整个set
     */
    void delete(K key);
}
