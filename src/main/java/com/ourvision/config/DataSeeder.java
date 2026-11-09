package com.ourvision.config;

import com.ourvision.entity.Role;
import com.ourvision.entity.User;
import com.ourvision.repository.RoleRepository;
import com.ourvision.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {
    @Bean CommandLineRunner seed(RoleRepository roles, UserRepository users, PasswordEncoder encoder) {
        return args -> {
            for (String role : new String[]{"ROLE_ADMIN","ROLE_OFFICE_BEARER","ROLE_MEMBER"})
                roles.findByName(role).orElseGet(() -> roles.save(new Role(role)));
            if (!users.existsByEmail("admin@ourvision.org")) {
                User admin = new User();
                admin.setFullName("OUR VISION Administrator");
                admin.setEmail("admin@ourvision.org");
                admin.setPassword(encoder.encode("Admin@123"));
                admin.setStatus("ACTIVE");
                admin.setRole(roles.findByName("ROLE_ADMIN").orElseThrow());
                users.save(admin);
            }
        };
    }
}
