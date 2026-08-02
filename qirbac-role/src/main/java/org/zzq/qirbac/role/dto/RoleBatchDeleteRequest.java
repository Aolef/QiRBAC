package org.zzq.qirbac.role.dto;

import lombok.Data;

import java.util.List;

@Data
public class RoleBatchDeleteRequest {

    private List<Long> ids;
}
