package com.amcs.infrastructure.persistence.repository;

import com.amcs.infrastructure.persistence.entity.SubjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataSubjectRepository extends JpaRepository<SubjectEntity, UUID> {
    Optional<SubjectEntity> findByCode(String code);
}
