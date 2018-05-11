package com.xinchang.common.cache;

import java.util.Collection;
import java.util.List;

public interface IListCache<K, V> {

    /**
     * 1、在redis指定Key所关联的List的头部插入参数中给出的Value。
     * 2、如果该Key不存在，将在插入redis之前创建一个与该Key关联的空链表，之后再将数据从链表的头部插入。
     * 3、返回的当前数组的长度。
     */
    public Long leftPush(K key, V value);

    public Long leftPushAll(K key, Collection<V> values);

    public void delete(K key);

    /**
     * 删除列表中某个value值的所有元素
     */
    public void delete(K key, V value);

    /**
     * 从链表头部offset指定的位置开始，截取limit指定的记录条数
     */
    public List<V> leftRange(K key, long offset, long limit);

    /**
     * 从链表尾部offset指定的位置开始，截取limit指定的记录条数
     */
    public List<V> rightRange(K key, long offset, long limit);

    /**
     * 返回链表长度
     */
    public Long size(K key);

    /**
     * 重新加载缓存而数据库异常时，缓存要采用的策略
     */
    public DBErrorStrategy getDBErrorStrategy();

    /**
     * 重新加载缓存而数据库中没有数据时，缓存要采用的策略
     */
    public NoneDataStrategy getNoneDataStrategy();
}
