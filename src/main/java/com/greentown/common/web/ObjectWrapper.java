package com.greentown.common.web;

public class ObjectWrapper {
    private byte[]   object;

    private Class<?> clazz;

    public ObjectWrapper() {
    }

    public ObjectWrapper(byte[] object, Class<?> clazz) {
        this.object = object;
        this.clazz = clazz;
    }

    public byte[] getObject() {
        return object;
    }

    public void setObject(byte[] object) {
        this.object = object;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }
}
