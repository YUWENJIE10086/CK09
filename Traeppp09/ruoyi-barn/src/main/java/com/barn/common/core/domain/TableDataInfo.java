package com.barn.common.core.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 表格分页数据
 */
@Data
public class TableDataInfo<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 总记录数 */
    private long total;

    /** 列表数据 */
    private List<T> rows;

    /** 当前页 */
    private long pageNum;

    /** 每页大小 */
    private long pageSize;

    public TableDataInfo() {
        this.rows = Collections.emptyList();
    }

    public TableDataInfo(List<T> list, long total) {
        this.rows = list;
        this.total = total;
    }

    public static <T> TableDataInfo<T> build(List<T> list, long total, long pageNum, long pageSize) {
        TableDataInfo<T> info = new TableDataInfo<>();
        info.setRows(list);
        info.setTotal(total);
        info.setPageNum(pageNum);
        info.setPageSize(pageSize);
        return info;
    }
}
