package com.energy.config;

import com.energy.entity.User;
import com.energy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        userRepository.findByPhone("13800000000").ifPresent(admin -> {
            if (!encoder.matches("123456", admin.getPassword())) {
                admin.setPassword(encoder.encode("123456"));
                admin.setRole("admin");
                admin.setStatus("active");
                userRepository.save(admin);
                System.out.println("[初始化] 管理员密码已重置为 123456");
            }
        });
    }
}
