package com.zw.zwaicodemother;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;


@EnableAspectJAutoProxy(exposeProxy = true)
@SpringBootApplication
public class ZwAiCodeMotherApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZwAiCodeMotherApplication.class, args);
    }

}
