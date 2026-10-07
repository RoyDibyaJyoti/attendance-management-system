package com.amcs.student;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    boolean existsByRegistrationNumber(String registrationNumber);
    Optional<Student> findByRegistrationNumber(String registrationNumber);
    Page<Student> findByNameContainingIgnoreCaseOrRegistrationNumberContainingIgnoreCase(
            String name, String regNo, Pageable pageable);
}
