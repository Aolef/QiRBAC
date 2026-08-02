-- 用户表。
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（加密存储）',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用：1启用，0禁用',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否逻辑删除：1已删除，0未删除',
    super_admin TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否超级管理员：1是，0否',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_username (username)
) COMMENT='用户表';

-- 角色表。
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_role_name (role_name)
) COMMENT='角色表';

-- 部门表。
-- sort_order：同级排序，越小越靠前。
-- deleted：逻辑删除标记，和 sys_user 保持一致，避免误删后用户关联断链。
CREATE TABLE IF NOT EXISTS sys_dept (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '部门ID',
    dept_name VARCHAR(50) NOT NULL COMMENT '部门名称',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父级部门ID，0表示顶级部门',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '同级排序，越小越靠前',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否逻辑删除：1已删除，0未删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_dept_parent_id (parent_id),
    INDEX idx_dept_parent_name (parent_id, dept_name)
) COMMENT='部门表';

-- 权限表。
-- parent_id：父级权限ID，0表示顶级权限。权限按树形结构归拢分类。
-- permission_type：FOLDER目录（纯分组，可作父，不参与鉴权）/ MENU菜单 / API接口 / BUTTON按钮（叶子，参与鉴权）。
-- deleted：逻辑删除标记，和 sys_dept 保持一致，级联删除子孙时统一置位。
CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '权限ID',
    permission_name VARCHAR(50) NOT NULL COMMENT '权限名称',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父级权限ID，0表示顶级权限',
    route_path VARCHAR(255) COMMENT '路由地址（FOLDER类型可空）',
    permission_type VARCHAR(20) NOT NULL COMMENT '权限类型：FOLDER目录，MENU菜单，API接口，BUTTON按钮',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '同级排序，越小越靠前',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否可用：1可用，0不可用',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否逻辑删除：1已删除，0未删除',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_permission_parent_id (parent_id),
    INDEX idx_permission_parent_name (parent_id, permission_name),
    INDEX idx_permission_type (permission_type)
) COMMENT='权限表';

-- 用户角色关系表。
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户角色关系ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_role (user_id, role_id),
    INDEX idx_user_role_user_id (user_id),
    INDEX idx_user_role_role_id (role_id)
) COMMENT='用户角色关系表';

-- 角色权限关系表。
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色权限关系ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    INDEX idx_role_permission_role_id (role_id),
    INDEX idx_role_permission_permission_id (permission_id)
) COMMENT='角色权限关系表';

-- 用户部门关系表。
CREATE TABLE IF NOT EXISTS sys_user_dept (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户部门关系ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    dept_id BIGINT NOT NULL COMMENT '部门ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_dept (user_id, dept_id),
    INDEX idx_user_dept_user_id (user_id),
    INDEX idx_user_dept_dept_id (dept_id)
) COMMENT='用户部门关系表';
