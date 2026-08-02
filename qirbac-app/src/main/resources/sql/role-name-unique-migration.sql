-- 现有数据库只执行一次。执行前应先处理重复的角色名称。
ALTER TABLE sys_role
    ADD CONSTRAINT uk_role_name UNIQUE (role_name);
