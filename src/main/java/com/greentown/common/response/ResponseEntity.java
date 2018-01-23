package com.greentown.common.response;

import java.io.Serializable;

import com.greentown.common.web.ExceptionConstants.ResultEnums;




public class ResponseEntity implements Serializable {
    private static final long serialVersionUID = 5600732461682124950L;

    private int               errorCode;

    private String            errorMessage;

    private Object            data;

    private transient String  jsonpFunction;

    public ResponseEntity(Object data) {
        this.data = (data == null ? "" : data);
    }

    public ResponseEntity(ResultEnums resultEnum) {
        this.errorCode = Integer.valueOf(resultEnum.name().substring(1));
        this.errorMessage = resultEnum.getError();
        this.data = "";
    }

    public ResponseEntity(ResultEnums resultEnum, Object data) {
        this(resultEnum);
        this.data = (data == null ? "" : data);
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getJsonpFunction() {
        return jsonpFunction;
    }

    public void setJsonpFunction(String jsonpFunction) {
        this.jsonpFunction = jsonpFunction;
    }
}
