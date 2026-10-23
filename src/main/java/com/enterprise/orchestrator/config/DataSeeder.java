package com.enterprise.orchestrator.config;

import com.enterprise.orchestrator.domain.*;
import com.enterprise.orchestrator.repository.AppUserRepository;
import com.enterprise.orchestrator.repository.AssetRepository;
import com.enterprise.orchestrator.repository.ResourceTypeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/** Demo data so the system is usable immediately. All passwords are "password". */
@Component
public class DataSeeder implements CommandLineRunner {

    private final AppUserRepository users;
    private final ResourceTypeRepository types;
    private final AssetRepository assets;
    private final PasswordEncoder encoder;

    public DataSeeder(AppUserRepository users, ResourceTypeRepository types, AssetRepository assets, PasswordEncoder encoder) {
        this.users = users; this.types = types; this.assets = assets; this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (users.count() > 0) return;

        String pw = encoder.encode("password");
        AppUser admin = users.save(new AppUser("admin", pw, "Ada Admin", "admin@corp.com", "Management", Role.ADMIN, null));
        AppUser alice = users.save(new AppUser("alice", pw, "Alice Manager", "alice@corp.com", "Engineering", Role.MANAGER, admin));
        users.save(new AppUser("bob", pw, "Bob Developer", "bob@corp.com", "Engineering", Role.EMPLOYEE, alice));
        users.save(new AppUser("carol", pw, "Carol Developer", "carol@corp.com", "Engineering", Role.EMPLOYEE, alice));
        users.save(new AppUser("ian", pw, "Ian IT", "ian@corp.com", "IT", Role.IT_ADMIN, admin));

        ResourceType laptop = types.save(new ResourceType("Dell XPS 15 Laptop", ResourceCategory.LAPTOP, new BigDecimal("1800.00"), 1));
        ResourceType ide = types.save(new ResourceType("IntelliJ IDEA Ultimate License", ResourceCategory.SOFTWARE_LICENSE, new BigDecimal("250.00"), 1));
        ResourceType monitor = types.save(new ResourceType("27\" 4K Monitor", ResourceCategory.PERIPHERAL, new BigDecimal("400.00"), 2));
        types.save(new ResourceType("GPU Workstation", ResourceCategory.EQUIPMENT, new BigDecimal("8500.00"), 1)); // exceeds budget ceiling

        for (int i = 1; i <= 2; i++) assets.save(new Asset(String.format("LAP-%03d", i), laptop));
        for (int i = 1; i <= 5; i++) assets.save(new Asset(String.format("LIC-IDEA-%03d", i), ide));
        for (int i = 1; i <= 3; i++) assets.save(new Asset(String.format("MON-%03d", i), monitor));
    }
}
