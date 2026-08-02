package org.zzq.qirbac.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.zzq.qirbac.common.BusinessException;
import org.zzq.qirbac.common.ResultCode;
import org.zzq.qirbac.role.dto.RoleSummary;
import org.zzq.qirbac.role.service.RoleQueryService;
import org.zzq.qirbac.security.context.LoginUserContext;
import org.zzq.qirbac.security.token.LoginTokenService;
import org.zzq.qirbac.dept.dto.DeptSummary;
import org.zzq.qirbac.dept.service.DeptQueryService;
import org.zzq.qirbac.user.dto.CurrentUserResponse;
import org.zzq.qirbac.user.dto.DeptResponse;
import org.zzq.qirbac.user.dto.RoleResponse;
import org.zzq.qirbac.user.dto.UserCreateRequest;
import org.zzq.qirbac.user.dto.UserDetailResponse;
import org.zzq.qirbac.user.dto.UserResponse;
import org.zzq.qirbac.user.dto.UserUpdateRequest;
import org.zzq.qirbac.user.entity.User;
import org.zzq.qirbac.user.repository.UserRelationRepository;
import org.zzq.qirbac.user.repository.UserRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleQueryService roleQueryService;
    private final DeptQueryService deptQueryService;
    private final UserRelationRepository userRelationRepository;
    private final LoginTokenService loginTokenService;

    public UserService(
            UserRepository userRepository,
            RoleQueryService roleQueryService,
            DeptQueryService deptQueryService,
            UserRelationRepository userRelationRepository,
            LoginTokenService loginTokenService
    ) {
        this.userRepository = userRepository;
        this.roleQueryService = roleQueryService;
        this.deptQueryService = deptQueryService;
        this.userRelationRepository = userRelationRepository;
        this.loginTokenService = loginTokenService;
    }

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        checkCreateRequest(request);
        String username = request.getUsername().trim();
        ensureUsernameAvailable(username, null);

        List<Long> roleIds = normalizeRelationIds(request.getRoleIds());
        List<Long> deptIds = normalizeRelationIds(request.getDeptIds());
        validateRoleIds(roleIds);
        validateDeptIds(deptIds);

        User user = new User(
                username,
                request.getPassword(),
                request.getEnabled() == null ? true : request.getEnabled(),
                false,
                false
        );
        User savedUser = userRepository.save(user);
        userRelationRepository.replaceRoles(savedUser.getId(), roleIds);
        userRelationRepository.replaceDepts(savedUser.getId(), deptIds);

        return toUserResponse(savedUser);
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        checkUserId(id);
        checkUpdateRequest(request);

        User user = getAvailableUser(id);
        String username = request.getUsername().trim();
        ensureUsernameAvailable(username, id);

        List<Long> roleIds = normalizeRelationIds(request.getRoleIds());
        List<Long> deptIds = normalizeRelationIds(request.getDeptIds());
        validateRoleIds(roleIds);
        validateDeptIds(deptIds);

        user.setUsername(username);
        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(request.getPassword());
        }
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }

        User savedUser = userRepository.save(user);
        userRelationRepository.replaceRoles(id, roleIds);
        userRelationRepository.replaceDepts(id, deptIds);

        if (Boolean.FALSE.equals(savedUser.getEnabled())) {
            loginTokenService.removeLoginTokensByUserId(id);
        }

        return toUserResponse(savedUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        checkUserId(id);
        rejectCurrentUser(List.of(id));

        User user = getAvailableUser(id);
        user.setDeleted(true);
        userRepository.save(user);
        userRelationRepository.deleteRolesByUserId(id);
        userRelationRepository.deleteDeptsByUserId(id);
        loginTokenService.removeLoginTokensByUserId(id);
    }

    @Transactional
    public void batchDeleteUsers(Collection<Long> ids) {
        List<Long> userIds = normalizeUserIds(ids);
        rejectCurrentUser(userIds);

        List<User> users = new ArrayList<>();
        userRepository.findAllById(userIds).forEach(user -> {
            if (!Boolean.TRUE.equals(user.getDeleted())) {
                users.add(user);
            }
        });
        if (users.size() != userIds.size()) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        users.forEach(user -> user.setDeleted(true));
        userRepository.saveAll(users);
        userRelationRepository.deleteRolesByUserIds(userIds);
        userRelationRepository.deleteDeptsByUserIds(userIds);
        userIds.forEach(loginTokenService::removeLoginTokensByUserId);
    }

    @Transactional(readOnly = true)
    public UserDetailResponse getUserDetail(Long id) {
        checkUserId(id);
        User user = getAvailableUser(id);
        return new UserDetailResponse(
                user.getId(),
                user.getUsername(),
                user.getEnabled(),
                userRelationRepository.findRoleIdsByUserId(id),
                userRelationRepository.findDeptIdsByUserId(id)
        );
    }

    /**
     * 获取当前登录用户信息。
     *
     * 根据当前 token 解析出的 userId 查询用户基础信息、角色、部门。
     * 未登录时抛 UNAUTHORIZED。
     */
    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser() {
        Long userId = LoginUserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        User user = userRepository.findAvailableById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.USER_NOT_FOUND));

        List<Long> roleIds = userRelationRepository.findRoleIdsByUserId(userId);
        List<Long> deptIds = userRelationRepository.findDeptIdsByUserId(userId);
        Map<Long, RoleSummary> rolesById = roleQueryService.findRoleSummaries(roleIds);
        Map<Long, DeptSummary> deptsById = deptQueryService.findDeptSummaries(deptIds);

        return new CurrentUserResponse(
                user.getId(),
                user.getUsername(),
                user.getEnabled(),
                toRoleResponses(roleIds, rolesById),
                toDeptResponses(deptIds, deptsById)
        );
    }

    private void checkCreateRequest(UserCreateRequest request) {
        if (request == null
                || !StringUtils.hasText(request.getUsername())
                || !StringUtils.hasText(request.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
    }

    private void checkUpdateRequest(UserUpdateRequest request) {
        if (request == null || !StringUtils.hasText(request.getUsername())) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
    }

    private void checkUserId(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
    }

    private User getAvailableUser(Long id) {
        return userRepository.findAvailableById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.USER_NOT_FOUND));
    }

    private void ensureUsernameAvailable(String username, Long excludedId) {
        boolean exists = excludedId == null
                ? userRepository.findAvailableByUsername(username).isPresent()
                : userRepository.findAvailableByUsernameExcludingId(username, excludedId).isPresent();
        if (exists) {
            throw new BusinessException(ResultCode.USERNAME_ALREADY_EXISTS);
        }
    }

    private List<Long> normalizeRelationIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        if (ids.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
        return new ArrayList<>(new LinkedHashSet<>(ids));
    }

    private List<Long> normalizeUserIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResultCode.INVALID_USER_IDS);
        }
        if (ids.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
        return new ArrayList<>(new LinkedHashSet<>(ids));
    }

    private void validateRoleIds(List<Long> roleIds) {
        roleQueryService.validateRoleIds(roleIds);
    }

    private void validateDeptIds(List<Long> deptIds) {
        deptQueryService.validateDeptIds(deptIds);
    }

    private void rejectCurrentUser(Collection<Long> ids) {
        Long currentUserId = LoginUserContext.getUserId();
        if (currentUserId != null && ids.contains(currentUserId)) {
            throw new BusinessException(ResultCode.CANNOT_DELETE_SELF);
        }
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEnabled());
    }

    private List<RoleResponse> toRoleResponses(List<Long> roleIds, Map<Long, RoleSummary> rolesById) {
        return roleIds.stream()
                .map(rolesById::get)
                .filter(java.util.Objects::nonNull)
                .map(role -> new RoleResponse(role.id(), role.roleName()))
                .toList();
    }

    private List<DeptResponse> toDeptResponses(List<Long> deptIds, Map<Long, DeptSummary> deptsById) {
        return deptIds.stream()
                .map(deptsById::get)
                .filter(java.util.Objects::nonNull)
                .map(dept -> new DeptResponse(dept.id(), dept.deptName(), dept.parentId()))
                .toList();
    }
}
