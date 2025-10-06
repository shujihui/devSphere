package com.shutu.shutuatuh;

import com.shutu.AuthApplication;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(classes = AuthApplication.class)
class AtuhApplicationTests {


    @Resource
    private PasswordEncoder passwordEncoder;
    @Test
    void contextLoads() {
        String m  = "123456";
        String encode = passwordEncoder.encode(m);
        System.out.println(encode);
    }

}
