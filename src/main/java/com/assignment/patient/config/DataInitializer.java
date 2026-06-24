package com.assignment.patient.config;

import com.assignment.patient.model.Patient;
import com.assignment.patient.repository.PatientRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedPatients(PatientRepository patientRepository) {
        return args -> {
            if (patientRepository.count() > 0) {
                return;
            }

            patientRepository.save(new Patient("Rahul", "Sharma", 34, "Male", "Hypertension"));
        };
    }
}
