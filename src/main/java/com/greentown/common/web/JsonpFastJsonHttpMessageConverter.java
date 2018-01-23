package com.greentown.common.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractGenericHttpMessageConverter;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.serializer.ValueFilter;
import com.greentown.common.response.ResponseEntity;


public class JsonpFastJsonHttpMessageConverter extends AbstractGenericHttpMessageConverter<Object>
                                               implements GenericHttpMessageConverter<Object> {

    public final static Charset UTF8         = Charset.forName("UTF-8");

    private Charset             charset      = UTF8;

    private SerializerFeature[] features     = new SerializerFeature[0];

    private ValueFilter[]       valueFilters = (ValueFilter[]) BaseValueFilter.VALUE_FILTERS
        .toArray(new ValueFilter[] {});

    public JsonpFastJsonHttpMessageConverter() {
        super(new MediaType("application", "json", UTF8),
            new MediaType("application", "*+json", UTF8), new MediaType("text", "html", UTF8),new MediaType("text", "plain", UTF8),
            new MediaType("text", "*+html", UTF8));
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return true;
    }

    public Charset getCharset() {
        return this.charset;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    public SerializerFeature[] getFeatures() {
        return features;
    }

    public void setFeatures(SerializerFeature... features) {
        this.features = features;
    }

    @Override
    protected void writeInternal(Object obj, Type type,
                                 HttpOutputMessage outputMessage) throws IOException,
                                                                  HttpMessageNotWritableException {
        OutputStream out = outputMessage.getBody();
        String text = JSON.toJSONString(obj, SerializeConfig.getGlobalInstance(), valueFilters,
            features);

        if (obj instanceof ResponseEntity) {
            String jsonpFunction = ((ResponseEntity) obj).getJsonpFunction();
            if (jsonpFunction != null) {
                StringBuilder textBuilder = new StringBuilder(
                    text.length() + jsonpFunction.length());
                text = textBuilder.append(jsonpFunction).append("(").append(text).append(");")
                    .toString();
            }
        }

        byte[] bytes = text.getBytes(charset);
        out.write(bytes);
    }

    @Override
    protected Object readInternal(Class<? extends Object> clazz,
                                  HttpInputMessage inputMessage) throws IOException,
                                                                 HttpMessageNotReadableException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        InputStream in = inputMessage.getBody();

        byte[] buf = new byte[1024];
        for (;;) {
            int len = in.read(buf);
            if (len == -1) {
                break;
            }

            if (len > 0) {
                baos.write(buf, 0, len);
            }
        }

        byte[] bytes = baos.toByteArray();
        return JSON.parseObject(bytes, 0, bytes.length, charset.newDecoder(), clazz);
    }

    @Override
    public Object read(Type type, Class<?> contextClass,
                       HttpInputMessage inputMessage) throws IOException,
                                                      HttpMessageNotReadableException {
        Class<?> targetClass = (type instanceof Class<?> ? (Class<?>) type : null);
        return this.readInternal(targetClass, inputMessage);
    }
}
