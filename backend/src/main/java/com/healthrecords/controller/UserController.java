package com.healthrecords.controller;

import com.healthrecords.dto.AuthDtos.UserResponse;
import com.healthrecords.exception.ResourceNotFoundException;
import com.healthrecords.mapper.RecordMapper;
import com.healthrecords.model.Role;
import com.healthrecords.model.User;
import com.healthrecords.repository.UserRepository;
import com.healthrecords.service.AccessService;
import com.healthrecords.service.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final AccessService accessService;
    private final AuditService auditService;

    public UserController(UserRepository userRepository, AccessService accessService, AuditService auditService) {
        this.userRepository = userRepository;
        this.accessService = accessService;
        this.auditService = auditService;
    }

    @GetMapping("/me")
    public UserResponse me() {
        User user = accessService.currentUser();
        return RecordMapper.toUserResponse(user);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public List<UserResponse> all(@RequestParam(required = false) Role role) {
        List<User> users = role == null ? userRepository.findAll() : userRepository.findByRole(role);
        return users.stream().map(RecordMapper::toUserResponse).toList();
    }

    @GetMapping("/paged")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public Page<UserResponse> allPaged(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) Role role) {
        Page<User> page = role == null ? userRepository.findAll(pageable) : userRepository.findByRole(role, pageable);
        return page.map(RecordMapper::toUserResponse);
    }

    @GetMapping("/patients")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public List<UserResponse> patients() {
        return userRepository.findByRole(Role.PATIENT).stream()
                .map(RecordMapper::toUserResponse).toList();
    }

    /** Admin-only role change. Audited as USER_ROLE_CHANGE; an admin cannot change their own role. */
    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse changeRole(@PathVariable Long id, @RequestBody RoleChangeRequest request) {
        User admin = accessService.currentUser();
        if (admin.getId().equals(id)) {
            throw new IllegalArgumentException("An admin cannot change their own role.");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        user.setRole(request.role());
        userRepository.save(user);
        auditService.record("USER_ROLE_CHANGE", "User", id);
        return RecordMapper.toUserResponse(user);
    }

    public record RoleChangeRequest(Role role) {
    }
}
