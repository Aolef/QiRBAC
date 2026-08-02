package org.zzq.qirbac.permission.types;

/**
 * 权限类型枚举。
 *
 * FOLDER：目录/文件夹，纯分组节点，可作为父节点，不参与鉴权。
 * MENU / API / BUTTON：叶子节点，参与鉴权，不可作为父节点。
 *
 * 领域规则（能否作父、是否参与鉴权）内聚到枚举自身，
 * Service 直接调用 canHaveChildren() / isAuthorizable()，
 * 后续新增类型只需修改本枚举一处。
 *
 * Spring Data JDBC 默认用 enum.name() 与 VARCHAR 列互转，
 * 数据库存的是 "FOLDER"/"MENU"/"API"/"BUTTON" 字符串，无需自定义 Converter。
 */
public enum PermissionType {

    FOLDER("目录", true, false),
    MENU("菜单", false, true),
    API("接口", false, true),
    BUTTON("按钮", false, true);

    private final String description;
    private final boolean canHaveChildren;
    private final boolean authorizable;

    PermissionType(String description, boolean canHaveChildren, boolean authorizable) {
        this.description = description;
        this.canHaveChildren = canHaveChildren;
        this.authorizable = authorizable;
    }

    public String getDescription() {
        return description;
    }

    public boolean canHaveChildren() {
        return canHaveChildren;
    }

    public boolean isAuthorizable() {
        return authorizable;
    }
}
