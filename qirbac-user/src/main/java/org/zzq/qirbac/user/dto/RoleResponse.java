package org.zzq.qirbac.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RoleResponse {

    private Long id;
    private String roleName;
}
