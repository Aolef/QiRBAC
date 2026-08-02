package org.zzq.qirbac.dept.dto;

import lombok.Data;

import java.util.List;

@Data
public class DeptBatchDeleteRequest {

    private List<Long> ids;
}
