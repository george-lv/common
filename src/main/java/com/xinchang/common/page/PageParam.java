package com.xinchang.common.page;

import java.io.Serializable;

/**
 * 
 * 分页参数传递工具类 .
 */
public class PageParam implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6297178964005032338L;
	private static final int PAGE_SHOW_COUNT = 10;
	private int pageNum; // 当前页数
	private int numPerPage; // 每页记录数
	
	public PageParam(int pageNum, int numPerPage) {
		super();
		this.pageNum = pageNum;
		this.numPerPage = numPerPage;
	}

	/** 当前页数 */
	public int getPageNum() {
		return pageNum;
	}

	/** 当前页数 */
	public void setPageNum(int pageNum) {
		this.pageNum = pageNum;
	}

	/** 每页记录数 */
	public int getNumPerPage() {
		return numPerPage > 0 ? numPerPage : PAGE_SHOW_COUNT;
	}

	/** 每页记录数 */
	public void setNumPerPage(int numPerPage) {
		this.numPerPage = numPerPage;
	}
	
	/**
	 * 每页开始记录
	 * @return
	 */
	public int getStartIndex() {
		int pageNum = this.getPageNum() > 0 ? this.getPageNum() - 1 : 0;
		return pageNum * this.getNumPerPage();
	}

}
