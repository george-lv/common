package com.xinchang.common.cache;

import java.util.Set;

public interface ISetCache<K, V> {
    /**
     * 返回set长度
     */
    public Long size(K key);

    /**
     * 往set中添加元素，如果参数中有的成员在Set中已经存在，该成员将被忽略。
     * 如果执行该命令之前，该Key并不存在，该命令将会创建一个新的Set，此后再将参数中的成员插入。
     */
    public void add(K key, V value);

    /**
     * 判断参数中指定成员是否已经存在于与Key相关联的Set集合中。
     */
    public Boolean isMember(K key, V value);

    /**
     * 获取缓存中所有成员
     */
    public Set<V> members(K key);

    /**
     * 返回随机的不重复的元素，返回的元素有可能少于count个数
     */
    public Set<V> distinctRandomMembers(K key, long count);

    /**
     * 从与Key关联的Set中删除参数中指定的成员，不存在的成员将被忽略。
     * 如果该Key并不存在，将视为空Set处理。
     */
    public void remove(K key, V value);

    /**
     * 重新加载缓存而数据库异常时，缓存要采用的策略
     */
    public DBErrorStrategy getDBErrorStrategy();

    /**
     * 重新加载缓存而数据库中没有数据时，缓存要采用的策略
     */
    public NoneDataStrategy getNoneDataStrategy();
}
