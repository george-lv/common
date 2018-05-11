package com.xinchang.common.vo;

import java.util.Date;

import org.apache.commons.lang3.builder.RecursiveToStringStyle;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.springframework.beans.BeanUtils;

import com.alibaba.fastjson.annotation.JSONField;

public class BaseVO {
	 private Long id;

	    
    @JSONField (format="yyyy-MM-dd HH:mm:ss")
    private Date              gmtCreate;
    
    @JSONField (format="yyyy-MM-dd HH:mm:ss")
    private Date              gmtModify;

    public <T> T copyPropertiesTo(T target, String... ignoreProperties) {
        BeanUtils.copyProperties(this, target, ignoreProperties);
        return target;
    }

	public Date getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    public Date getGmtModify() {
        return gmtModify;
    }

    public void setGmtModify(Date gmtModify) {
        this.gmtModify = gmtModify;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (null == this.getId()) {
			throw new UnsupportedOperationException("vo id is null");
		}

		if (getClass() != obj.getClass()) {
			return false;
		}

		BaseVO other = (BaseVO) obj;
		if (null == other.getId()) {
			throw new UnsupportedOperationException("vo id is null");
		}

		if (this.getId().equals(other.getId())) {
			return true;
		} else {
			return false;
		}
	}

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this, new RecursiveToStringStyle()).toString();
    }
}
