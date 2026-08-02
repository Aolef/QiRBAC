package org.zzq.qirbac.user.repository;

import org.springframework.data.repository.CrudRepository;
import org.zzq.qirbac.user.entity.Role;

public interface RoleRepository extends CrudRepository<Role, Long> {
}
