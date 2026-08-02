package org.zzq.qirbac.permission.dto;

import lombok.Data;

import java.util.List;

@Data
public class PermissionBatchDeleteRequest {

    private List<Long> ids;
}
