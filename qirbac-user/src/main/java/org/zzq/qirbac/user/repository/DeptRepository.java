package org.zzq.qirbac.user.repository;

import org.springframework.data.repository.CrudRepository;
import org.zzq.qirbac.user.entity.Dept;

public interface DeptRepository extends CrudRepository<Dept, Long> {
}
