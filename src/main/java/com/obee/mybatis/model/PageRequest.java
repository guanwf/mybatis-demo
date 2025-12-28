package com.obee.mybatis.model;

import java.io.Serializable;

/**
 * @description:
 * @author: Guanwf
 * @date: 2025/12/28 10:33
 */
public class PageRequest implements Serializable {
    private long current = 1; // 当前页
    private long size = 10;   // 每页条数

    // 默认构造
    public PageRequest() {}

    public PageRequest(long current, long size) {
        this.current = current;
        this.size = size;
    }

    public long getCurrent() { return current; }
    public void setCurrent(long current) { this.current = current; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
}
