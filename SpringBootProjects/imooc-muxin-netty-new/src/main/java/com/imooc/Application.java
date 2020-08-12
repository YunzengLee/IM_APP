package com.imooc;

//import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import tk.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
//扫描mybatis mapper包路径
@MapperScan(basePackages ="com.imooc.mapper")
//扫描所有需要的包，包含一些自用的工具类包所在路径
@ComponentScan(basePackages = {"com.imooc","org.n3r.idworker"})
//开启定时任务
//@EnableScheduling
//开启异步调用方法
//@EnableAsync
public class Application {

    //此处是让spring管理SpringUtil，注册SpringUtil这个类，不加这一段，调用SpringUtil的地方就会报错
    @Bean
    public SpringUtil getSpringUtil(){
        return new SpringUtil();
    }

    public static void main(String[] args) {

        SpringApplication.run(Application.class,args);
    }
}
