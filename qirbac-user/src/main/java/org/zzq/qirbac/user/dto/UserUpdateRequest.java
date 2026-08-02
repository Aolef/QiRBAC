package org.zzq.qirbac.user.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserUpdateRequest {

    private String username;
    private String password;
    private Boolean enabled;
    private List<Long> roleIds;
    private List<Long> deptIds;
}
