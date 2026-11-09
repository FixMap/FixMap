package com.fixmap.config;

import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import jakarta.servlet.Filter;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletRegistration;

/**
 * web.xml 대신 Spring을 등록하는 시작점. Tomcat이 시작할 때 자동으로 실행된다.
 * Spring Boot의 main() + 자동 설정이 하던 일을 여기와 설정 클래스들이 맡는다.
 */
public class WebAppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

    /** 첨부 파일 1개 최대 크기 */
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    /** 요청 1건 최대 크기 (파일 여러 개 포함) */
    private static final long MAX_REQUEST_SIZE = 35L * 1024 * 1024;

    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class<?>[] {RootConfig.class};
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class<?>[] {WebConfig.class};
    }

    @Override
    protected String[] getServletMappings() {
        return new String[] {"/"};
    }

    /** 모든 요청·응답을 UTF-8로 처리 */
    @Override
    protected Filter[] getServletFilters() {
        return new Filter[] {new CharacterEncodingFilter("UTF-8", true)};
    }

    /** 파일 업로드(multipart) 허용 */
    @Override
    protected void customizeRegistration(ServletRegistration.Dynamic registration) {
        registration.setMultipartConfig(new MultipartConfigElement("", MAX_FILE_SIZE, MAX_REQUEST_SIZE, 0));
    }
}
