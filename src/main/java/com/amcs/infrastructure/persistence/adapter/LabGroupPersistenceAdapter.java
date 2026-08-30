package com.amcs.infrastructure.persistence.adapter;

import com.amcs.application.port.out.LabGroupRepositoryPort;
import com.amcs.infrastructure.persistence.entity.LabGroupEntity;
import com.amcs.infrastructure.persistence.entity.LabGroupMembershipEntity;
import com.amcs.infrastructure.persistence.repository.SpringDataLabGroupMembershipRepository;
import com.amcs.infrastructure.persistence.repository.SpringDataLabGroupRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class LabGroupPersistenceAdapter implements LabGroupRepositoryPort {

    private final SpringDataLabGroupRepository groupRepository;
    private final SpringDataLabGroupMembershipRepository membershipRepository;

    public LabGroupPersistenceAdapter(
        SpringDataLabGroupRepository groupRepository,
        SpringDataLabGroupMembershipRepository membershipRepository
    ) {
        this.groupRepository = Objects.requireNonNull(groupRepository, "groupRepository");
        this.membershipRepository = Objects.requireNonNull(membershipRepository, "membershipRepository");
    }

    @Override
    public LabGroupEntity saveGroup(LabGroupEntity group) {
        return groupRepository.save(group);
    }

    @Override
    public Optional<LabGroupEntity> findGroupById(UUID id) {
        return groupRepository.findById(id);
    }

    @Override
    public List<LabGroupEntity> findGroupsBySection(UUID sectionId) {
        return groupRepository.findBySectionId(sectionId);
    }

    @Override
    public LabGroupMembershipEntity saveMembership(LabGroupMembershipEntity membership) {
        return membershipRepository.save(membership);
    }

    @Override
    public Optional<LabGroupMembershipEntity> findActiveMembership(UUID studentId, LocalDate date) {
        return membershipRepository.findActiveByStudentIdOnDate(studentId, date);
    }

    @Override
    public List<LabGroupMembershipEntity> findMembershipsByStudent(UUID studentId) {
        return membershipRepository.findByStudentId(studentId);
    }
}
