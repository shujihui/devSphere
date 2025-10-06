package com.shutu.shutucommonssecurity;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
class ShutuCommonsSecurityApplicationTests {

    @Resource
    private PasswordEncoder passwordEncoder;
    @Test
    void contextLoads() {
        String m  = "123456";
        String encode = passwordEncoder.encode(m);
        System.out.println(encode);
    }

}
