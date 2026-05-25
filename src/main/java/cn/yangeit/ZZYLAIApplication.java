package cn.yangeit;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;

//  启动类
@SpringBootApplication
@ComponentScan({"cn.yangeit", "com.example"})
@EnableCaching
@MapperScan({"cn.yangeit.mapper", "com.example.mapper"})
public class ZZYLAIApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZZYLAIApplication.class, args);
        System.out.println("启动成功");
    }

}
