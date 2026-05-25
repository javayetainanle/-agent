package cn.yangeit.config;

import cn.yangeit.pojo.Elder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration//这是一个配置对象
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    private LoginCheckInterceptor loginCheckInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry
                .addInterceptor(loginCheckInterceptor)
                .addPathPatterns("/customer/**")
                .excludePathPatterns("/customer/user/login",
                        "/customer/roomTypes",
                        "/error",
                        "/swagger-ui/**", "/v3/**");
    }
}
