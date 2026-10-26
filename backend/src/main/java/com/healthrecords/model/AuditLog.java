package com.healthrecords.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Append-only (in application code: never updated or deleted via the API)
 * record of sensitive actions. No health data is stored here — only who
 * did what, to which entity id, from where, and when. Admins can list
 * these logs; there is no API to edit or delete them.
 */
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_logs_actor", columnList = "actor_email"),
        @Index(name = "idx_audit_logs_action", columnList = "action"),
        @Index(name = "idx_audit_logs_created", columnList = "created_at")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_email")
    private String actorEmail;

    @Column(nullable = false)
    private String action;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public AuditLog() {
    }

    public AuditLog(String actorEmail, String action, String entityType, Long entityId,
                    String ipAddress, String userAgent) {
        this.actorEmail = actorEmail;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    public Long getId() { return id; }
    public String getActorEmail() { return actorEmail; }
    public String getAction() { return action; }
    public String getEntityType() { return entityType; }
    public Long getEntityId() { return entityId; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
