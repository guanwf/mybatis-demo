package com.obee.mybatis.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * @description:
 * @author: Guanwf
 * @date: 2025/12/28 10:34
 */
public class PageResult<T> implements Serializable {
    private List<T> records = Collections.emptyList(); // 数据列表
    private long total = 0;    // 总条数
    private long current = 1;  // 当前页
    private long size = 10;    // 每页条数
    private long pages = 0;    // 总页数

    public PageResult() {
    }

    // 全参构造
    public PageResult(List<T> records, long total, long current, long size, long pages) {
        this.records = records;
        this.total = total;
        this.current = current;
        this.size = size;
        this.pages = pages;
    }

    // Getters & Setters ...
    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getCurrent() {
        return current;
    }

    public void setCurrent(long current) {
        this.current = current;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getPages() {
        return pages;
    }

    public void setPages(long pages) {
        this.pages = pages;
    }
}
