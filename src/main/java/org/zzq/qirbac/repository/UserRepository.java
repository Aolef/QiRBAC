package org.zzq.qirbac.repository;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.zzq.qirbac.entity.User;

import java.util.Optional;

/**
 * 用户表数据库操作。
 *
 * Repository 可以理解成专门访问数据库的对象，
 * Service 层通过它查询 sys_user 表。
 */
public interface UserRepository extends CrudRepository<User, Long> {

    /**
     * 根据用户名查询未删除的用户。
     *
     * deleted = 0 表示用户没有被逻辑删除。
     */
    @Query("SELECT id, username, password, enabled, deleted, super_admin FROM sys_user WHERE username = :username AND deleted = 0 LIMIT 1")
    Optional<User> findAvailableByUsername(@Param("username") String username);
}
