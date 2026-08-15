package org.example.hrmanagement.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 分页响应包装
 */
@Data
public class PageResult<T> {
    /** 数据列表 */
    private List<T> records;
    /** 总记录数 */
    private long total;
    /** 当前页码 */
    private long pageNum;
    /** 每页条数 */
    private long pageSize;
    /** 总页数 */
    private long pages;

    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(page.getRecords());
        result.setTotal(page.getTotal());
        result.setPageNum(page.getCurrent());
        result.setPageSize(page.getSize());
        result.setPages(page.getPages());
        return result;
    }

    public static <T> PageResult<T> empty() {
        PageResult<T> result = new PageResult<>();
        result.setRecords(Collections.emptyList());
        result.setTotal(0);
        result.setPageNum(1);
        result.setPageSize(10);
        result.setPages(0);
        return result;
    }
}
