package com.fixmap.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 설정: 컨트롤러 등록, JSP 위치, 정적 파일 경로.
 * Jackson이 클래스패스에 있어서 @RestController 응답은 자동으로 JSON이 된다.
 */
@Configuration
@EnableWebMvc
@ComponentScan(basePackages = "com.fixmap.controller")
public class WebConfig implements WebMvcConfigurer {

    /** return "main" → /WEB-INF/views/main.jsp */
    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
        registry.jsp("/WEB-INF/views/", ".jsp");
    }

    /** DispatcherServlet이 "/"를 받으므로 css·js·배치도는 따로 열어 준다. */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**").addResourceLocations("/static/");
    }
}
