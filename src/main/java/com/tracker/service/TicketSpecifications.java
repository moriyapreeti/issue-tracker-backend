package com.tracker.service;

import com.tracker.dto.TicketFilter;
import com.tracker.entity.Project;
import com.tracker.entity.Ticket;
import com.tracker.entity.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class TicketSpecifications {
    private TicketSpecifications() {}

    public static Specification<Ticket> build(TicketFilter f, Long userId, boolean admin) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();

            // Non-admins only ever see tickets from projects they belong to
            if (!admin) {
                Join<Ticket, Project> project = root.join("project", JoinType.INNER);
                Join<Project, User> member = project.join("members", JoinType.INNER);
                ps.add(cb.equal(member.get("id"), userId));
                query.distinct(true);
            }
            if (f.projectId() != null) ps.add(cb.equal(root.get("project").get("id"), f.projectId()));
            if (f.status() != null) ps.add(cb.equal(root.get("status"), f.status()));
            if (f.priority() != null) ps.add(cb.equal(root.get("priority"), f.priority()));
            if (f.type() != null) ps.add(cb.equal(root.get("type"), f.type()));
            if (f.assigneeId() != null) ps.add(cb.equal(root.get("assignee").get("id"), f.assigneeId()));
            if (f.reporterId() != null) ps.add(cb.equal(root.get("reporter").get("id"), f.reporterId()));
            if (f.q() != null && !f.q().isBlank()) {
                String like = "%" + f.q().trim().toLowerCase().replace("%", "\\%").replace("_", "\\_") + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like, '\\'),
                        cb.like(cb.lower(root.<String>get("description")), like, '\\')));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }
}
