package com.tbtha.gespa_backend.util;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod") // Solo en producción
public class FlywayRepairRunner implements CommandLineRunner {

    private final Flyway flyway;

    @Autowired
    public FlywayRepairRunner(Flyway flyway) {
        this.flyway = flyway;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Ejecutando Flyway.repair() para corregir checksums...");
        flyway.repair();
        System.out.println("Flyway.repair() ejecutado correctamente.");
    }
}
