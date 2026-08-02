package org.zzq.qirbac.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class UserDetailResponse {

    private Long id;
    private String username;
    private Boolean enabled;
    private List<Long> roleIds;
    private List<Long> deptIds;
}
