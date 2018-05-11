package com.xinchang.common.web;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import com.alibaba.fastjson.serializer.ValueFilter;

public abstract class BaseValueFilter implements ValueFilter {
    public static final List<ValueFilter> VALUE_FILTERS = new ArrayList<>();

    @PostConstruct
    public void init() {
        VALUE_FILTERS.add(this);
    }
}