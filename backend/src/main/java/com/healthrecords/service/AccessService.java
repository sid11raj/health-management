package com.healthrecords.service;

import com.healthrecords.model.Role;
import com.healthrecords.model.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Centralises the ownership rule used across record services:
 * ADMIN and DOCTOR may access any patient's data; a PATIENT may only
 * access their own data.
 */
@Service
public class AccessService {

    public User currentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User user) {
            return user;
        }
        throw new AccessDeniedException("Not authenticated");
    }

    public void requireAccessToPatient(Long patientId) {
        User current = currentUser();
        if (current.getRole() == Role.ADMIN || current.getRole() == Role.DOCTOR) {
            return;
        }
        if (!current.getId().equals(patientId)) {
            throw new AccessDeniedException("Patients can only access their own records.");
        }
    }

    public boolean isStaff() {
        Role role = currentUser().getRole();
        return role == Role.ADMIN || role == Role.DOCTOR;
    }
}
