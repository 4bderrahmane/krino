package com.krino.backend;

import com.krino.backend.configuration.ProductionConfigurationValidator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KrinoApplication {
    static void main(String[] args) {
        SpringApplication application = new SpringApplication(KrinoApplication.class);
        /*
        * Registered as a listener rather than a bean so it can reject an unsafe production
        * configuration before the context is refreshed, in particular before Hibernate has had
        * the chance to act on ddl-auto. See {@link ProductionConfigurationValidator}.
        */
        application.addListeners(new ProductionConfigurationValidator());
        application.run(args);
    }
}
