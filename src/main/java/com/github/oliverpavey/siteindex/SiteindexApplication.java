package com.github.oliverpavey.siteindex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Launch spring boot application.
 * <p>
 * N.B. When the application is launched, after the Spring Context set-up is complete, then the
 * application processing in SiteindexAutorun will commence (as it implements CommandLineRunner).
 */
@SpringBootApplication
public class SiteindexApplication {

    public static void main(String[] args) {
        SpringApplication.run(SiteindexApplication.class, args);
    }

}
