package com.wingstep.config;

import com.wingstep.common.web.LogonCheckInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final LogonCheckInterceptor logonCheckInterceptor;
    
    @Value("${app.upload.review-dir}")
    private String reviewUploadDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(logonCheckInterceptor)
				.addPathPatterns("/user/**"/* , "/api/**" */);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {

        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // === 후기 이미지(upload/review) ===
        String reviewDir = reviewUploadDir;
        if (!reviewDir.endsWith("/") && !reviewDir.endsWith("\\")) {
            reviewDir = reviewDir + "/";
        }
        reviewDir = reviewDir.replace("\\", "/");

        registry.addResourceHandler("/upload/review/**")
                .addResourceLocations("file:" + reviewDir);
    }

}
