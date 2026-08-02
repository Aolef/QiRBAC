package org.zzq.qirbac.common;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PageResult<T> {

    private List<T> records;
    private Long total;
    private Integer page;
    private Integer pageSize;
    private Long totalPages;
}
