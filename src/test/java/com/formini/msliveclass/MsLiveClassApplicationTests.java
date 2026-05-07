package com.formini.msliveclass;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MsLiveClassApplicationTests {

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.context.ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        // Verify the Spring application context loads successfully.
        org.junit.jupiter.api.Assertions.assertNotNull(applicationContext, "The application context should not be null");
    }

}
