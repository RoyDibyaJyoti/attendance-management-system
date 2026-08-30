package com.amcs.infrastructure.persistence.adapter;

import com.amcs.application.port.out.EnrollmentRepositoryPort;
import com.amcs.domain.enrollment.Enrollment;
import com.amcs.infrastructure.persistence.entity.EnrollmentEntity;
import com.amcs.infrastructure.persistence.entity.LabGroupMembershipEntity;
import com.amcs.infrastructure.persistence.mapper.EnrollmentPersistenceMapper;
import com.amcs.infrastructure.persistence.repository.SpringDataEnrollmentRepository;
import com.amcs.infrastructure.persistence.repository.SpringDataLabGroupMembershipRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class EnrollmentPersistenceAdapter implements EnrollmentRepositoryPort {

    private final SpringDataEnrollmentRepository enrollmentRepository;
    private final SpringDataLabGroupMembershipRepository membershipRepository;
    private final EnrollmentPersistenceMapper mapper;

    public EnrollmentPersistenceAdapter(
        SpringDataEnrollmentRepository enrollmentRepository,
        SpringDataLabGroupMembershipRepository membershipRepository,
        EnrollmentPersistenceMapper mapper
    ) {
        this.enrollmentRepository = Objects.requireNonNull(enrollmentRepository, "enrollmentRepository");
        this.membershipRepository = Objects.requireNonNull(membershipRepository, "membershipRepository");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    @Override
    public Enrollment save(Enrollment enrollment) {
        UUID id = UUID.randomUUID();
        EnrollmentEntity entity = mapper.toEntity(id, enrollment, "ACTIVE");
        EnrollmentEntity saved = enrollmentRepository.save(entity);

        // If lab group is present, also persist a lab group membership
        if (enrollment.labGroupId().isPresent()) {
            LabGroupMembershipEntity membership = new LabGroupMembershipEntity(
                UUID.randomUUID(),
                enrollment.studentId(),
                enrollment.labGroupId().get(),
                enrollment.enrollmentStart(),
                enrollment.enrollmentEnd().orElse(null)
            );
            membershipRepository.save(membership);
        }

        return mapper.toDomain(saved, enrollment.labGroupId());
    }

    @Override
    public Optional<Enrollment> findActiveEnrollment(UUID studentId, UUID sectionId) {
        return enrollmentRepository.findActiveByStudentIdAndSectionId(studentId, sectionId)
            .map(entity -> {
                Optional<UUID> labGroupId = membershipRepository
                    .findActiveByStudentIdOnDate(studentId, LocalDate.now())
                    .map(LabGroupMembershipEntity::getLabGroupId);
                return mapper.toDomain(entity, labGroupId);
            });
    }

    @Override
    public List<Enrollment> findByStudent(UUID studentId) {
        return enrollmentRepository.findByStudentId(studentId)
            .stream()
            .map(e -> {
                Optional<UUID> labGroupId = membershipRepository
                    .findActiveByStudentIdOnDate(studentId, e.getEnrollmentStart())
                    .map(LabGroupMembershipEntity::getLabGroupId);
                return mapper.toDomain(e, labGroupId);
            })
            .toList();
    }

    @Override
    public List<Enrollment> findBySection(UUID sectionId) {
        return enrollmentRepository.findBySectionId(sectionId)
            .stream()
            .map(e -> {
                Optional<UUID> labGroupId = membershipRepository
                    .findActiveByStudentIdOnDate(e.getStudentId(), e.getEnrollmentStart())
                    .map(LabGroupMembershipEntity::getLabGroupId);
                return mapper.toDomain(e, labGroupId);
            })
            .toList();
    }
}
