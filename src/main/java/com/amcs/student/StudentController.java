package com.amcs.student;

import com.amcs.department.DepartmentRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/students")
public class StudentController {

    private final StudentRepository studentRepo;
    private final DepartmentRepository deptRepo;

    public StudentController(StudentRepository studentRepo, DepartmentRepository deptRepo) {
        this.studentRepo = studentRepo;
        this.deptRepo = deptRepo;
    }

    record StudentRequest(
        @NotBlank String registrationNumber,
        @NotBlank String name,
        String email,
        @NotNull Long departmentId,
        Boolean active
    ) {}
    record ErrorBody(String message) {}

    @GetMapping
    public Page<Student> list(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        if (!search.isBlank()) {
            return studentRepo.findByNameContainingIgnoreCaseOrRegistrationNumberContainingIgnoreCase(
                    search, search, PageRequest.of(page, size, Sort.by("name")));
        }
        return studentRepo.findAll(PageRequest.of(page, size, Sort.by("name")));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Student> get(@PathVariable Long id) {
        return studentRepo.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> create(@Valid @RequestBody StudentRequest req) {
        if (studentRepo.existsByRegistrationNumber(req.registrationNumber())) {
            return ResponseEntity.badRequest().body(new ErrorBody("Registration number already exists"));
        }
        var dept = deptRepo.findById(req.departmentId()).orElse(null);
        if (dept == null) return ResponseEntity.badRequest().body(new ErrorBody("Department not found"));
        Student s = new Student();
        s.setRegistrationNumber(req.registrationNumber());
        s.setName(req.name());
        s.setEmail(req.email());
        s.setDepartment(dept);
        if (req.active() != null) {
            s.setActive(req.active());
        }
        return ResponseEntity.ok(studentRepo.save(s));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody StudentRequest req) {
        return studentRepo.findById(id).map(s -> {
            if (!s.getRegistrationNumber().equals(req.registrationNumber()) && studentRepo.existsByRegistrationNumber(req.registrationNumber())) {
                return ResponseEntity.badRequest().body(new ErrorBody("Registration number already exists"));
            }
            s.setRegistrationNumber(req.registrationNumber());
            s.setName(req.name());
            s.setEmail(req.email());
            if (req.active() != null) {
                s.setActive(req.active());
            }
            deptRepo.findById(req.departmentId()).ifPresent(s::setDepartment);
            return ResponseEntity.ok(studentRepo.save(s));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!studentRepo.existsById(id)) return ResponseEntity.notFound().build();
        studentRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
