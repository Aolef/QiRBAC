package org.zzq.qirbac.user.repository;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.zzq.qirbac.user.entity.User;

import java.util.Optional;

/**
 * 用户表数据库操作。
 *
 * Repository 可以理解成专门访问数据库的对象，
 * Service 层通过它查询 sys_user 表。
 */
public interface UserRepository extends CrudRepository<User, Long> {

    @Query("SELECT id, username, password, enabled, deleted, super_admin, created_at, updated_at FROM sys_user WHERE id = :id AND deleted = 0 LIMIT 1")
    Optional<User> findAvailableById(@Param("id") Long id);

    /**
     * 根据用户名查询未删除的用户。
     *
     * deleted = 0 表示用户没有被逻辑删除。
     */
    @Query("SELECT id, username, password, enabled, deleted, super_admin, created_at, updated_at FROM sys_user WHERE username = :username AND deleted = 0 LIMIT 1")
    Optional<User> findAvailableByUsername(@Param("username") String username);

    @Query("SELECT id, username, password, enabled, deleted, super_admin, created_at, updated_at FROM sys_user WHERE username = :username AND deleted = 0 AND id <> :id LIMIT 1")
    Optional<User> findAvailableByUsernameExcludingId(
            @Param("username") String username,
            @Param("id") Long id
    );
}
