package org.zzq.qirbac.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DeptResponse {

    private Long id;
    private String deptName;
    private Long parentId;
}
