package com.barn;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 烤房管理系统启动类
 */
@SpringBootApplication
@MapperScan("com.barn.**.mapper")
public class BarnApplication {
    public static void main(String[] args) {
        SpringApplication.run(BarnApplication.class, args);
        System.out.println("========== 烤房管理系统启动成功 ==========");
        System.out.println("接口地址: http://localhost:8080");
        System.out.println("Swagger: 暂未启用");
    }
}
