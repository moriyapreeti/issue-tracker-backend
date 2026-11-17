package com.tracker.config;

import com.tracker.entity.*;
import com.tracker.repository.ProjectRepository;
import com.tracker.repository.TicketRepository;
import com.tracker.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;


@Configuration
public class DataInitializer {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner seed(UserRepository users, ProjectRepository projects, TicketRepository tickets, PasswordEncoder encoder) {
        return args -> {
            if (users.count() > 0) return;
            User admin = users.save(new User("Admin", "admin@tracker.com", encoder.encode("Admin@123"), Role.ADMIN));
            User manager = users.save(new User("Maya Manager", "manager@tracker.com", encoder.encode("Manager@123"), Role.MANAGER));
            User dev = users.save(new User("Dev Developer", "dev@tracker.com", encoder.encode("Dev@1234"), Role.DEVELOPER));

            Project p = new Project();
            p.setName("Website Revamp");
            p.setProjectKey("WEB");
            p.setDescription("Sample project created on first start-up.");
            p.setOwner(manager);
            p.getMembers().add(manager);
            p.getMembers().add(dev);
            p = projects.save(p);

            addTicket(tickets, p, "Login button unresponsive on Safari", "Clicking login does nothing on Safari 17.", Status.TODO, Priority.HIGH, IssueType.BUG, manager, dev);
            addTicket(tickets, p, "Design new landing page hero", "Create mockups for the new hero section.", Status.IN_PROGRESS, Priority.MEDIUM, IssueType.TASK, manager, dev);
            addTicket(tickets, p, "As a user I can reset my password", "Email-based password reset flow.", Status.TODO, Priority.LOW, IssueType.STORY, manager, null);
            addTicket(tickets, p, "Set up CI pipeline", "Run tests on every pull request.", Status.DONE, Priority.MEDIUM, IssueType.TASK, manager, manager);
            log.info("Seeded demo data. Admin login: admin@tracker.com / Admin@123");
        };
    }

    private void addTicket(TicketRepository repo, Project p, String title, String desc, Status s, Priority pr,
                           IssueType type, User reporter, User assignee) {
        Ticket t = new Ticket();
        t.setTitle(title);
        t.setDescription(desc);
        t.setStatus(s);
        t.setPriority(pr);
        t.setType(type);
        t.setProject(p);
        t.setReporter(reporter);
        t.setAssignee(assignee);
        repo.save(t);
    }
}
