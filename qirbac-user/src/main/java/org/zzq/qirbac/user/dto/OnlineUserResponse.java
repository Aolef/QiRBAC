package org.zzq.qirbac.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class OnlineUserResponse {

    private Long userId;
    private String username;
    private Boolean enabled;
    private List<RoleResponse> roles;
    private List<DeptResponse> depts;
}
