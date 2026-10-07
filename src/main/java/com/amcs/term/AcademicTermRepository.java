package com.amcs.term;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AcademicTermRepository extends JpaRepository<AcademicTerm, Long> {
    Optional<AcademicTerm> findByActiveTrue();
    Optional<AcademicTerm> findByName(String name);
}
