package org.zzq.qirbac.role.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class RoleResponse {

    private Long id;
    private String roleName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
