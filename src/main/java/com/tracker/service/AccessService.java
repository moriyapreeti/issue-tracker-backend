package com.tracker.service;

import com.tracker.entity.Project;
import com.tracker.entity.Role;
import com.tracker.entity.Ticket;
import com.tracker.entity.User;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Central place for role-based / ownership-based permission rules.
 *
 *  ADMIN     : everything.
 *  MANAGER   : can create projects; manages (members, edit, delete) projects they own.
 *  DEVELOPER : works on projects they are a member of.
 */
@Component
public class AccessService {

    public boolean isAdmin(User u) {
        return u.getRole() == Role.ADMIN;
    }

    public boolean canAccess(Project p, User u) {
        return isAdmin(u) || p.hasMember(u.getId());
    }

    public void requireAccess(Project p, User u) {
        if (!canAccess(p, u)) throw new AccessDeniedException("Not a member of this project");
    }

    public boolean canManage(Project p, User u) {
        return isAdmin(u) || p.getOwner().getId().equals(u.getId());
    }

    public void requireManage(Project p, User u) {
        if (!canManage(p, u)) throw new AccessDeniedException("Only the project owner or an admin can do this");
    }

    public boolean canEditTicket(Ticket t, User u) {
        return canManage(t.getProject(), u)
                || t.getReporter().getId().equals(u.getId())
                || (t.getAssignee() != null && t.getAssignee().getId().equals(u.getId()));
    }

    public boolean canDeleteTicket(Ticket t, User u) {
        return canManage(t.getProject(), u) || t.getReporter().getId().equals(u.getId());
    }
}
