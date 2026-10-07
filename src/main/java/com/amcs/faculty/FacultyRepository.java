package com.amcs.faculty;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FacultyRepository extends JpaRepository<Faculty, Long> {
    Optional<Faculty> findByUserUsername(String username);
    Optional<Faculty> findByEmployeeId(String employeeId);
    boolean existsByEmployeeId(String employeeId);
}
