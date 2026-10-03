package com.trackflow.gateway.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.trackflow.gateway.auth.domain.RoleName;
import com.trackflow.gateway.auth.repository.UserRepository;
import com.trackflow.gateway.auth.security.AuthProperties;

/**
 * Creates demo accounts on a fresh database. The rider usernames match the couriers seeded by
 * delivery-service, which is how a rider's login is linked to their courier profile.
 */
@Component
public class DemoUserSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoUserSeeder.class);

    private final UserRepository users;
    private final AuthService authService;
    private final AuthProperties properties;

    public DemoUserSeeder(UserRepository users, AuthService authService, AuthProperties properties) {
        this.users = users;
        this.authService = authService;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.seedDemoUsers() || users.count() > 0) {
            return;
        }
        String password = properties.demoPassword();
        authService.createUser("admin", password, "Ada Admin", RoleName.ADMIN);
        authService.createUser("grace", password, "Grace Hopper", RoleName.USER);
        authService.createUser("alan", password, "Alan Turing", RoleName.USER);
        authService.createUser("amara", password, "Amara Okafor", RoleName.RIDER);
        authService.createUser("bilal", password, "Bilal Hussain", RoleName.RIDER);
        authService.createUser("chen", password, "Chen Wei", RoleName.RIDER);
        authService.createUser("dana", password, "Dana Kowalski", RoleName.RIDER);
        log.info("Seeded demo accounts (admin, grace, alan, amara, bilal, chen, dana)");
    }
}
