package com.popularpreferred;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAspectJAutoProxy(exposeProxy = true) // 暴露代理对象
@MapperScan("com.popularpreferred.mapper")
@SpringBootApplication
@EnableScheduling
public class PopularPreferredApplication {

    public static void main(String[] args) {
        SpringApplication.run(PopularPreferredApplication.class, args);
    }

}
