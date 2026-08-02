-- 现有数据库只执行一次。为已存在的 sys_permission 表补充父级与逻辑删除字段。
-- 新库无需执行，schema.sql 中的 CREATE TABLE 已包含这些字段。
ALTER TABLE sys_permission
    ADD COLUMN parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父级权限ID，0表示顶级权限' AFTER permission_name,
    ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否逻辑删除：1已删除，0未删除' AFTER enabled;

-- 同级重名校验依靠 (parent_id, permission_name) 联合索引加速，不加 UNIQUE 约束：
-- 逻辑删除后需要允许同名重建，约束会冲突。
CREATE INDEX idx_permission_parent_id ON sys_permission (parent_id);
CREATE INDEX idx_permission_parent_name ON sys_permission (parent_id, permission_name);
