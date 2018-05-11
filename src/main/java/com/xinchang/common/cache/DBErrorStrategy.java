package com.xinchang.common.cache;

/**
 * 重新加载缓存而数据库异常时，缓存要采用的策略
 *
 * @author lvziqiang
 * @since $Revision:1.0.0, $Date: 2016年2月1日 下午3:54:47 $
 */
public interface DBErrorStrategy {
    /**
     * 数据库异常后重试次数，超过重试次数则不继续重试，0或负数表示不重试
     */
    long retryTimes();

    /**
     * 下次重试的时间间隔，0或负数表示不重试
     */
    long nextRetryInterval();
}
