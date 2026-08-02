package org.zzq.qirbac.user.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserBatchDeleteRequest {

    private List<Long> ids;
}
