package com.barn.common.core.page;

import lombok.Data;

/**
 * 分页查询参数
 */
@Data
public class PageQuery {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String orderByColumn;
    private String isAsc = "desc";
}
