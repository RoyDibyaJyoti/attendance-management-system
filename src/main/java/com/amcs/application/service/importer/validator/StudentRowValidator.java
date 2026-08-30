package com.amcs.application.service.importer.validator;

import com.amcs.application.port.out.DepartmentRepositoryPort;
import com.amcs.application.port.out.SectionRepositoryPort;
import com.amcs.application.port.out.StudentRepositoryPort;
import com.amcs.application.port.out.excel.ParsedRow;
import com.amcs.application.service.importer.payload.StagedStudentPayload;
import com.amcs.domain.importer.RowValidationError;
import com.amcs.infrastructure.persistence.entity.DepartmentEntity;
import com.amcs.infrastructure.persistence.entity.SectionEntity;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class StudentRowValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final StudentRepositoryPort studentRepository;
    private final DepartmentRepositoryPort departmentRepository;
    private final SectionRepositoryPort sectionRepository;

    public StudentRowValidator(
        StudentRepositoryPort studentRepository,
        DepartmentRepositoryPort departmentRepository,
        SectionRepositoryPort sectionRepository
    ) {
        this.studentRepository = Objects.requireNonNull(studentRepository, "studentRepository");
        this.departmentRepository = Objects.requireNonNull(departmentRepository, "departmentRepository");
        this.sectionRepository = Objects.requireNonNull(sectionRepository, "sectionRepository");
    }

    public ValidationOutcome<StagedStudentPayload> validateRow(
        ParsedRow row,
        Set<String> seenRegNos,
        Set<String> seenEmails
    ) {
        int rowIndex = row.rowIndex();

        // Level 1: Syntactic
        String regNo = row.get("Registration Number");
        if (regNo.isEmpty()) {
            return ValidationOutcome.invalid(new RowValidationError(
                rowIndex, "Registration Number", "", "MISSING_REQUIRED_FIELD", "Registration Number is required"
            ));
        }

        String name = row.get("Full Name");
        if (name.isEmpty()) {
            return ValidationOutcome.invalid(new RowValidationError(
                rowIndex, "Full Name", "", "MISSING_REQUIRED_FIELD", "Full Name is required"
            ));
        }

        String email = row.get("Email");
        if (email.isEmpty()) {
            return ValidationOutcome.invalid(new RowValidationError(
                rowIndex, "Email", "", "MISSING_REQUIRED_FIELD", "Email is required"
            ));
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return ValidationOutcome.invalid(new RowValidationError(
                rowIndex, "Email", email, "INVALID_EMAIL", "Email format is invalid: '" + email + "'"
            ));
        }

        String deptCode = row.get("Department Code");
        if (deptCode.isEmpty()) {
            return ValidationOutcome.invalid(new RowValidationError(
                rowIndex, "Department Code", "", "MISSING_REQUIRED_FIELD", "Department Code is required"
            ));
        }

        String sectionName = row.get("Section Name");
        if (sectionName.isEmpty()) {
            return ValidationOutcome.invalid(new RowValidationError(
                rowIndex, "Section Name", "", "MISSING_REQUIRED_FIELD", "Section Name is required"
            ));
        }

        // Level 2: Referential
        Optional<DepartmentEntity> deptOpt = departmentRepository.findByCode(deptCode);
        if (deptOpt.isEmpty()) {
            return ValidationOutcome.invalid(new RowValidationError(
                rowIndex, "Department Code", deptCode, "DEPARTMENT_NOT_FOUND", "Department not found with code: '" + deptCode + "'"
            ));
        }
        DepartmentEntity dept = deptOpt.get();

        Optional<SectionEntity> sectionOpt = sectionRepository.findAll().stream()
            .filter(s -> s.getName().equalsIgnoreCase(sectionName))
            .findFirst();
        if (sectionOpt.isEmpty()) {
            return ValidationOutcome.invalid(new RowValidationError(
                rowIndex, "Section Name", sectionName, "SECTION_NOT_FOUND", "Section not found with name: '" + sectionName + "'"
            ));
        }
        SectionEntity section = sectionOpt.get();

        // Level 3: Duplicates & Business
        String normRegNo = regNo.toLowerCase();
        if (seenRegNos.contains(normRegNo)) {
            return ValidationOutcome.invalid(new RowValidationError(
                rowIndex, "Registration Number", regNo, "DUPLICATE_IN_FILE", "Duplicate registration number within file: '" + regNo + "'"
            ));
        }
        seenRegNos.add(normRegNo);

        String normEmail = email.toLowerCase();
        if (seenEmails.contains(normEmail)) {
            return ValidationOutcome.invalid(new RowValidationError(
                rowIndex, "Email", email, "DUPLICATE_IN_FILE", "Duplicate email within file: '" + email + "'"
            ));
        }
        seenEmails.add(normEmail);

        if (studentRepository.findByRegistrationNumber(regNo).isPresent()) {
            return ValidationOutcome.invalid(new RowValidationError(
                rowIndex, "Registration Number", regNo, "STUDENT_ALREADY_EXISTS", "Student already exists with registration number: '" + regNo + "'"
            ));
        }

        if (studentRepository.findByEmail(email).isPresent()) {
            return ValidationOutcome.invalid(new RowValidationError(
                rowIndex, "Email", email, "EMAIL_ALREADY_EXISTS", "Student already exists with email: '" + email + "'"
            ));
        }

        return ValidationOutcome.valid(new StagedStudentPayload(
            regNo, name, email, dept.getId(), section.getId()
        ));
    }
}
