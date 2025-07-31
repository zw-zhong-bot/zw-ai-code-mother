package com.zw.zwaicodemother;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;


@EnableAspectJAutoProxy(exposeProxy = true)
@SpringBootApplication
@MapperScan("com.zw.zwaicodemother.mapper")
public class ZwAiCodeMotherApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZwAiCodeMotherApplication.class, args);
    }

}
