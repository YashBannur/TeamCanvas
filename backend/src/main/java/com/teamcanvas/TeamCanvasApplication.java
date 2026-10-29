package com.teamcanvas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TeamCanvasApplication {

    public static void main(String[] args) {
        SpringApplication.run(TeamCanvasApplication.class, args);
    }
}
