package com.xinchang.common.exception;

import com.xinchang.common.web.ExceptionConstants.ResultEnums;

public class BusinessLogicException extends RuntimeException {
    private static final long serialVersionUID = -1658132880061605029L;

    private ResultEnums       resultEnum;

    private Object            extra;

    public BusinessLogicException(ResultEnums resultEnum) {
        this.resultEnum = resultEnum;
    }

    public BusinessLogicException(ResultEnums resultEnum, Object extra) {
        this.resultEnum = resultEnum;
        this.extra = extra;
    }

    public ResultEnums getResultEnum() {
        return resultEnum;
    }

    public Object getExtra() {
        return extra;
    }
}