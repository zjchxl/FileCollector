package com.example;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@Slf4j
public class FileMonitorApplication implements ApplicationRunner {

    public static void main(String[] args) {
        log.info("================================================");
        log.info("FileMonitor start    文件监控系统启动中...");
        log.info("================================================");

        SpringApplication app = new SpringApplication(FileMonitorApplication.class);
        app.setBannerMode(Banner.Mode.OFF);

        ConfigurableApplicationContext context = app.run(args);

        // 检查所有Bean是否加载正常
        log.info("应用启动完成，监控系统已就绪");
        log.info("================================================");
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // ApplicationRunner接口确保在所有Bean初始化完成后执行
        log.info("应用程序启动完成，开始执行初始化任务...");

        // 添加关闭钩子
        // 这里可以执行一些启动后的任务
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("================================================");
            log.info("FileMonitor end    文件监控系统正在关闭...");
            log.info("================================================");
        }));
    }
}
