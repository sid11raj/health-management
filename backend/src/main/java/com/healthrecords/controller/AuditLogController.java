package com.healthrecords.controller;

import com.healthrecords.dto.AuthDtos.AuditLogResponse;
import com.healthrecords.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only audit-log listing. Logs are append-only via the API: there
 * is no endpoint to create, edit or delete them.
 */
@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogRepository repository;

    public AuditLogController(AuditLogRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<AuditLogResponse> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String action) {
        Page<com.healthrecords.model.AuditLog> page = (action == null || action.isBlank())
                ? repository.findAll(pageable)
                : repository.findByActionContainingIgnoreCase(action.trim(), pageable);
        return page.map(l -> new AuditLogResponse(l.getId(), l.getActorEmail(), l.getAction(),
                l.getEntityType(), l.getEntityId(), l.getIpAddress(), l.getUserAgent(), l.getCreatedAt()));
    }
}
