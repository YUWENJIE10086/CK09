package com.barn.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Web MVC 配置 - CORS + 静态资源
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${upload.path:d:/云盘/MyWeb/烤房项目/upload/}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将 /upload/** 映射到磁盘上传目录，开发环境可直接访问图片
        String location = uploadPath;
        if (!location.endsWith("/") && !location.endsWith("\\")) {
            location += File.separator;
        }
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:" + location);
    }
}
