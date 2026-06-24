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
            patientRepository.save(new Patient("Priya", "Patel", 28, "Female", "Type 2 Diabetes"));
            patientRepository.save(new Patient("Amit", "Kumar", 45, "Male", "Asthma"));
            patientRepository.save(new Patient("Sneha", "Reddy", 31, "Female", "Migraine"));
            patientRepository.save(new Patient("Vikash", "Singh", 52, "Male", "Coronary Artery Disease"));
            patientRepository.save(new Patient("Anita", "Desai", 39, "Female", "Hypothyroidism"));
            patientRepository.save(new Patient("Rohit", "Mehta", 22, "Male", "Seasonal Allergy"));
            patientRepository.save(new Patient("Kavita", "Nair", 60, "Female", "Osteoarthritis"));
        };
    }
}
