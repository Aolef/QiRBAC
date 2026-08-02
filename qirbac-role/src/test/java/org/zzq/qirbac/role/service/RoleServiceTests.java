package org.zzq.qirbac.role.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zzq.qirbac.common.BusinessException;
import org.zzq.qirbac.common.PageResult;
import org.zzq.qirbac.common.ResultCode;
import org.zzq.qirbac.role.dto.RoleCreateRequest;
import org.zzq.qirbac.role.dto.RoleResponse;
import org.zzq.qirbac.role.entity.Role;
import org.zzq.qirbac.role.repository.RoleQueryRepository;
import org.zzq.qirbac.role.repository.RoleRelationRepository;
import org.zzq.qirbac.role.repository.RoleRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceTests {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RoleQueryRepository roleQueryRepository;

    @Mock
    private RoleRelationRepository roleRelationRepository;

    @InjectMocks
    private RoleService roleService;

    @Test
    void createRoleTrimsRoleName() {
        RoleCreateRequest request = new RoleCreateRequest();
        request.setRoleName(" 管理员 ");
        when(roleRepository.findByRoleName("管理员")).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> {
            Role role = invocation.getArgument(0);
            role.setId(1L);
            return role;
        });

        RoleResponse response = roleService.createRole(request);

        assertEquals(1L, response.getId());
        assertEquals("管理员", response.getRoleName());
    }

    @Test
    void createRoleRejectsDuplicateName() {
        RoleCreateRequest request = new RoleCreateRequest();
        request.setRoleName("管理员");
        when(roleRepository.findByRoleName("管理员")).thenReturn(Optional.of(role(1L, "管理员")));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> roleService.createRole(request)
        );

        assertEquals(ResultCode.ROLE_NAME_ALREADY_EXISTS.getCode(), exception.getCode());
        verify(roleRepository, never()).save(any());
    }

    @Test
    void deleteRoleCleansRelationsBeforeRole() {
        when(roleRepository.findById(3L)).thenReturn(Optional.of(role(3L, "审核员")));

        roleService.deleteRole(3L);

        InOrder inOrder = inOrder(roleRelationRepository, roleRepository);
        inOrder.verify(roleRelationRepository).deleteByRoleId(3L);
        inOrder.verify(roleRepository).deleteById(3L);
    }

    @Test
    void batchDeleteRejectsWholeRequestWhenAnyRoleIsMissing() {
        when(roleRepository.findAllById(List.of(1L, 2L)))
                .thenReturn(List.of(role(1L, "管理员")));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> roleService.batchDeleteRoles(List.of(1L, 2L))
        );

        assertEquals(ResultCode.ROLE_NOT_FOUND.getCode(), exception.getCode());
        verify(roleRelationRepository, never()).deleteByRoleIds(any());
        verify(roleRepository, never()).deleteAllById(any());
    }

    @Test
    void getRolePageCalculatesOffsetAndTotalPages() {
        when(roleQueryRepository.count("管理")).thenReturn(21L);
        when(roleQueryRepository.findPage("管理", 10, 10L))
                .thenReturn(List.of(role(2L, "管理员")));

        PageResult<RoleResponse> result = roleService.getRolePage(2, 10, " 管理 ");

        assertEquals(21L, result.getTotal());
        assertEquals(3L, result.getTotalPages());
        assertEquals(2, result.getPage());
        assertEquals("管理员", result.getRecords().get(0).getRoleName());
    }

    private Role role(Long id, String roleName) {
        Role role = new Role(roleName);
        role.setId(id);
        return role;
    }
}
