package org.zzq.qirbac.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 当前登录用户信息。
 *
 * 用于 /users/me 接口：根据当前 token 返回当前用户的基础信息 + 角色 + 部门。
 */
@Data
@AllArgsConstructor
public class CurrentUserResponse {

    private Long userId;
    private String username;
    private Boolean enabled;
    private List<RoleResponse> roles;
    private List<DeptResponse> depts;
}
