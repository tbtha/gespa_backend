package com.tbtha.gespa_backend.config;

import com.tbtha.gespa_backend.entities.Specialty;
import com.tbtha.gespa_backend.repositories.SpecialtyRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DefaultSpecialtiesInitializer implements CommandLineRunner {

    private static final List<String> DEFAULT_SPECIALTIES = List.of(
            "Medicina General",
            "Cardiología",
            "Dermatología",
            "Endocrinología",
            "Gastroenterología",
            "Ginecología",
            "Neurología",
            "Nutrición",
            "Pediatría",
            "Psiquiatría",
            "Traumatología"
    );

    private final SpecialtyRepository specialtyRepository;

    public DefaultSpecialtiesInitializer(SpecialtyRepository specialtyRepository) {
        this.specialtyRepository = specialtyRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        for (String name : DEFAULT_SPECIALTIES) {
            specialtyRepository.findByNameIgnoreCase(name)
                    .ifPresentOrElse(existing -> {
                        if (!existing.isActive()) {
                            existing.setActive(true);
                            specialtyRepository.save(existing);
                        }
                    }, () -> {
                        Specialty specialty = new Specialty();
                        specialty.setName(name);
                        specialty.setActive(true);
                        specialtyRepository.save(specialty);
                    });
        }
    }
}
