package com.amcs.application.port.out;

import com.amcs.infrastructure.persistence.entity.LabGroupEntity;
import com.amcs.infrastructure.persistence.entity.LabGroupMembershipEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LabGroupRepositoryPort {
    LabGroupEntity saveGroup(LabGroupEntity group);
    Optional<LabGroupEntity> findGroupById(UUID id);
    List<LabGroupEntity> findGroupsBySection(UUID sectionId);

    LabGroupMembershipEntity saveMembership(LabGroupMembershipEntity membership);
    Optional<LabGroupMembershipEntity> findActiveMembership(UUID studentId, LocalDate date);
    List<LabGroupMembershipEntity> findMembershipsByStudent(UUID studentId);
}
