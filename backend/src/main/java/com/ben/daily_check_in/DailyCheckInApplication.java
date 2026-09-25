package com.ben.daily_check_in;

import com.ben.daily_check_in.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableConfigurationProperties(AppProperties.class)
@SpringBootApplication
public class DailyCheckInApplication {

    public static void main(String[] args) {
        SpringApplication.run(DailyCheckInApplication.class, args);
    }

}
