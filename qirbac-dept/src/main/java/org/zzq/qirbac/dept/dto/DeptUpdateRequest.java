package org.zzq.qirbac.dept.dto;

import lombok.Data;

/**
 * 修改部门请求。
 *
 * parentId 为空时表示顶级部门。
 */
@Data
public class DeptUpdateRequest {

    private String deptName;
    private Long parentId;
    private Integer sortOrder;
}
