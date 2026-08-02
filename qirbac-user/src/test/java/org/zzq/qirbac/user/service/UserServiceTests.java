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
import org.zzq.qirbac.dept.dto.DeptSummary;
import org.zzq.qirbac.dept.service.DeptQueryService;
import org.zzq.qirbac.role.dto.RoleSummary;
import org.zzq.qirbac.role.service.RoleQueryService;
import org.zzq.qirbac.security.context.LoginUser;
import org.zzq.qirbac.security.context.LoginUserContext;
import org.zzq.qirbac.security.token.LoginTokenService;
import org.zzq.qirbac.user.dto.CurrentUserResponse;
import org.zzq.qirbac.user.dto.UserCreateRequest;
import org.zzq.qirbac.user.dto.UserResponse;
import org.zzq.qirbac.user.dto.UserUpdateRequest;
import org.zzq.qirbac.user.entity.User;
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
    private DeptQueryService deptQueryService;

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
        verify(deptQueryService).validateDeptIds(List.of(10L));
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
    void getCurrentUserReturnsInfoAndRoleAndDeptObjects() {
        LoginUserContext.set(new LoginUser(1L, "zhangsan", false));
        User currentUser = user(1L, "zhangsan", "password", true, false);

        when(userRepository.findAvailableById(1L)).thenReturn(Optional.of(currentUser));
        when(userRelationRepository.findRoleIdsByUserId(1L)).thenReturn(List.of(3L));
        when(userRelationRepository.findDeptIdsByUserId(1L)).thenReturn(List.of(4L));
        when(roleQueryService.findRoleSummaries(any()))
                .thenReturn(Map.of(3L, new RoleSummary(3L, "审核员")));
        when(deptQueryService.findDeptSummaries(any()))
                .thenReturn(Map.of(4L, new DeptSummary(4L, "运营部", 0L)));

        CurrentUserResponse response = userService.getCurrentUser();

        assertEquals("zhangsan", response.getUsername());
        assertEquals("审核员", response.getRoles().get(0).getRoleName());
        assertEquals("运营部", response.getDepts().get(0).getDeptName());
    }

    private User user(Long id, String username, String password, boolean enabled, boolean deleted) {
        User user = new User(username, password, enabled, deleted, false);
        user.setId(id);
        return user;
    }
}
