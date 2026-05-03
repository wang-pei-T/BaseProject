package com.baseproject.config;

import com.baseproject.security.AuthInterceptor;
import com.baseproject.security.PlatformPermissionInterceptor;
import com.baseproject.security.PrincipalTypeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final PrincipalTypeInterceptor principalTypeInterceptor;
    private final PlatformPermissionInterceptor platformPermissionInterceptor;

    public WebMvcConfig(
            AuthInterceptor authInterceptor,
            PrincipalTypeInterceptor principalTypeInterceptor,
            PlatformPermissionInterceptor platformPermissionInterceptor) {
        this.authInterceptor = authInterceptor;
        this.principalTypeInterceptor = principalTypeInterceptor;
        this.platformPermissionInterceptor = platformPermissionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**", "/auth/**", "/tenant/**", "/platform/**")
                .excludePathPatterns("/api/ping");
        registry.addInterceptor(principalTypeInterceptor)
                .addPathPatterns("/api/**", "/auth/**", "/tenant/**", "/platform/**")
                .excludePathPatterns("/api/ping");
        registry.addInterceptor(platformPermissionInterceptor)
                .addPathPatterns("/api/**", "/auth/**", "/tenant/**", "/platform/**")
                .excludePathPatterns("/api/ping");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "X-Request-Id")
                .maxAge(3600);
    }
}

