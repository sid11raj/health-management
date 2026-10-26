package com.healthrecords.service;

import com.healthrecords.model.AuditLog;
import com.healthrecords.model.User;
import com.healthrecords.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Audit logging. Best-effort and never fatal: an audit failure must not
 * roll back the business operation it describes, so it runs in its own
 * transaction and swallows its own errors (logging them instead).
 * No health data, passwords, tokens or TOTP codes are ever written here.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String entityType, Long entityId) {
        recordForActor(currentActorEmail(), action, entityType, entityId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordForActor(String actorEmail, String action, String entityType, Long entityId) {
        try {
            HttpServletRequest request = currentRequest();
            String ip = null;
            String userAgent = null;
            if (request != null) {
                String forwarded = request.getHeader("X-Forwarded-For");
                ip = forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
                userAgent = request.getHeader("User-Agent");
                if (userAgent != null && userAgent.length() > 500) {
                    userAgent = userAgent.substring(0, 500);
                }
            }
            repository.save(new AuditLog(actorEmail, action, entityType, entityId, ip, userAgent));
        } catch (Exception ex) {
            // Audit must never break the user-facing operation.
            log.warn("Failed to write audit log for action {}: {}", action, ex.getMessage());
        }
    }

    private String currentActorEmail() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                    ? SecurityContextHolder.getContext().getAuthentication().getPrincipal() : null;
            if (principal instanceof User user) {
                return user.getEmail();
            }
            if (principal instanceof String s) {
                return s;
            }
        } catch (Exception ignored) {
            // fall through to anonymous
        }
        return "anonymous";
    }

    private HttpServletRequest currentRequest() {
        try {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
                return attrs.getRequest();
            }
        } catch (Exception ignored) {
            // no request context (e.g. unit test)
        }
        return null;
    }
}
