package cn.sc.blog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan({"cn.sc.love"})
public class ServiceBlogApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceBlogApplication.class, args);
    }
}