package org.zzq.qirbac.user.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zzq.qirbac.common.BusinessException;
import org.zzq.qirbac.common.ResultCode;
import org.zzq.qirbac.role.dto.RoleSummary;
import org.zzq.qirbac.role.service.RoleQueryService;
import org.zzq.qirbac.security.context.LoginUser;
import org.zzq.qirbac.security.context.LoginUserContext;
import org.zzq.qirbac.security.token.LoginTokenService;
import org.zzq.qirbac.user.dto.OnlineUserResponse;
import org.zzq.qirbac.user.dto.UserCreateRequest;
import org.zzq.qirbac.user.dto.UserResponse;
import org.zzq.qirbac.user.dto.UserUpdateRequest;
import org.zzq.qirbac.user.entity.Dept;
import org.zzq.qirbac.user.entity.User;
import org.zzq.qirbac.user.repository.DeptRepository;
import org.zzq.qirbac.user.repository.UserRelationRepository;
import org.zzq.qirbac.user.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleQueryService roleQueryService;

    @Mock
    private DeptRepository deptRepository;

    @Mock
    private UserRelationRepository userRelationRepository;

    @Mock
    private LoginTokenService loginTokenService;

    @InjectMocks
    private UserService userService;

    @AfterEach
    void clearLoginUser() {
        LoginUserContext.clear();
    }

    @Test
    void createUserAlwaysCreatesNonSuperAdminAndDeduplicatesRelations() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername(" zhangsan ");
        request.setPassword("123456");
        request.setRoleIds(List.of(1L, 1L));
        request.setDeptIds(List.of(10L));

        when(userRepository.findAvailableByUsername("zhangsan")).thenReturn(Optional.empty());
        when(deptRepository.findAllById(List.of(10L))).thenReturn(List.of(dept(10L, "技术部", 0L)));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(100L);
            return user;
        });

        UserResponse response = userService.createUser(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("zhangsan", userCaptor.getValue().getUsername());
        assertFalse(userCaptor.getValue().getSuperAdmin());
        assertEquals(100L, response.getId());
        verify(userRelationRepository).replaceRoles(100L, List.of(1L));
        verify(userRelationRepository).replaceDepts(100L, List.of(10L));
        verify(roleQueryService).validateRoleIds(List.of(1L));
    }

    @Test
    void updateUserKeepsBlankPasswordAndRemovesTokensWhenDisabled() {
        User existingUser = user(2L, "old-name", "old-password", true, false);
        UserUpdateRequest request = new UserUpdateRequest();
        request.setUsername("new-name");
        request.setPassword("  ");
        request.setEnabled(false);
        request.setRoleIds(List.of());
        request.setDeptIds(List.of());

        when(userRepository.findAvailableById(2L)).thenReturn(Optional.of(existingUser));
        when(userRepository.findAvailableByUsernameExcludingId("new-name", 2L))
                .thenReturn(Optional.empty());
        when(deptRepository.findAllById(List.of())).thenReturn(List.of());
        when(userRepository.save(existingUser)).thenReturn(existingUser);

        userService.updateUser(2L, request);

        assertEquals("old-password", existingUser.getPassword());
        assertFalse(existingUser.getEnabled());
        verify(loginTokenService).removeLoginTokensByUserId(2L);
    }

    @Test
    void batchDeleteRejectsWholeRequestWhenItContainsCurrentUser() {
        LoginUserContext.set(new LoginUser(7L, "current", false));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.batchDeleteUsers(List.of(6L, 7L, 8L))
        );

        assertEquals(ResultCode.CANNOT_DELETE_SELF.getCode(), exception.getCode());
        verify(userRepository, never()).findAllById(any());
        verify(userRepository, never()).saveAll(any());
    }

    @Test
    void onlineUsersAreDeduplicatedAndIncludeRoleAndDeptObjects() {
        User onlineUser = user(1L, "zhangsan", "password", true, false);
        User disabledUser = user(2L, "lisi", "password", false, false);

        when(loginTokenService.findOnlineLoginUsers()).thenReturn(List.of(
                new LoginUser(1L, "old-name", false),
                new LoginUser(1L, "old-name", false),
                new LoginUser(2L, "lisi", false)
        ));
        when(userRepository.findAllById(any()))
                .thenReturn(List.of(onlineUser, disabledUser));
        when(userRelationRepository.findRoleIdsByUserIds(List.of(1L)))
                .thenReturn(Map.of(1L, List.of(3L)));
        when(userRelationRepository.findDeptIdsByUserIds(List.of(1L)))
                .thenReturn(Map.of(1L, List.of(4L)));
        when(roleQueryService.findRoleSummaries(any()))
                .thenReturn(Map.of(3L, new RoleSummary(3L, "审核员")));
        when(deptRepository.findAllById(any())).thenReturn(List.of(dept(4L, "运营部", 0L)));

        List<OnlineUserResponse> responses = userService.getOnlineUsers();

        assertEquals(1, responses.size());
        OnlineUserResponse response = responses.get(0);
        assertEquals("zhangsan", response.getUsername());
        assertEquals("审核员", response.getRoles().get(0).getRoleName());
        assertEquals("运营部", response.getDepts().get(0).getDeptName());
    }

    private User user(Long id, String username, String password, boolean enabled, boolean deleted) {
        User user = new User(username, password, enabled, deleted, false);
        user.setId(id);
        return user;
    }

    private Dept dept(Long id, String name, Long parentId) {
        Dept dept = new Dept(name, parentId);
        dept.setId(id);
        return dept;
    }
}
