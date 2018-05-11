package com.xinchang.common.web;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 * 使用fastjson作为redis中对象的序列化方式
 *
 * @author chenxiang
 * @since $Revision:1.0.0, $Date: 2016年1月9日 上午11:19:00 $
 */
public class FastJsonRedisSerializer<T> implements RedisSerializer<T> {

    @Override
    public byte[] serialize(T t) throws SerializationException {
        if (t == null)
            return null;

        return JSON.toJSONBytes(new ObjectWrapper(JSON.toJSONBytes(t), t.getClass()));
    }

    @Override
    public T deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null)
            return null;

        Object data = JSON.parse(bytes);
        if (data instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) data;
            try {
                return JSON.parseObject(jsonObject.getBytes("object"),
                    Class.forName(jsonObject.getString("clazz")));
            }
            catch (ClassNotFoundException e) {
                return null;
            }
        }
        else if (data instanceof Integer) {
            return JSON.parseObject(bytes, Long.class);
        }
        else {
            return JSON.parseObject(bytes, data.getClass());
        }
    }
}
