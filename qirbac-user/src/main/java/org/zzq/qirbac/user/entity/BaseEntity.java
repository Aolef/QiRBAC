package org.zzq.qirbac.user.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;

import java.time.LocalDateTime;

/**
 * 实体基类。
 *
 * 所有业务表都有自己的 id、创建时间、更新时间，
 * 所以把这些公共字段抽到这里，避免每个实体重复写一遍。
 */
@Data
public class BaseEntity {

    /**
     * 主键 ID。
     *
     * 每张表都有自己的唯一 id，关系表也一样。
     */
    @Id
    private Long id;

    /**
     * 创建时间。
     *
     * 新增数据时，Spring Data JDBC 审计功能会自动填充。
     */
    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     *
     * 新增和修改数据时，Spring Data JDBC 审计功能会自动填充。
     */
    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;
}
