package com.ai.kust;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class KustSpringAiApplicationTests {

    @Test
    void contextLoads() {
        System.out.println(ResultCode.formCode(404));
        System.out.println(ResultCode.formCode(404).getMessage());
    }

}
