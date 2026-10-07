package com.amcs.enrollment;

import com.amcs.lab.LabBatchRepository;
import com.amcs.section.SectionRepository;
import com.amcs.student.StudentRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/enrollments")
public class EnrollmentController {

    private final EnrollmentRepository enrollmentRepo;
    private final StudentRepository studentRepo;
    private final SectionRepository sectionRepo;
    private final LabBatchRepository labBatchRepo;

    public EnrollmentController(EnrollmentRepository enrollmentRepo, StudentRepository studentRepo,
                                SectionRepository sectionRepo, LabBatchRepository labBatchRepo) {
        this.enrollmentRepo = enrollmentRepo;
        this.studentRepo = studentRepo;
        this.sectionRepo = sectionRepo;
        this.labBatchRepo = labBatchRepo;
    }

    record EnrollRequest(@NotNull Long studentId, @NotNull Long sectionId, Long labBatchId) {}
    record ErrorBody(String message) {}

    @GetMapping
    public List<Enrollment> list(@RequestParam(required = false) Long sectionId) {
        if (sectionId != null) return enrollmentRepo.findBySectionId(sectionId);
        return enrollmentRepo.findAll();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> enroll(@Valid @RequestBody EnrollRequest req) {
        var student = studentRepo.findById(req.studentId()).orElse(null);
        if (student == null) return ResponseEntity.badRequest().body(new ErrorBody("Student not found"));
        var section = sectionRepo.findById(req.sectionId()).orElse(null);
        if (section == null) return ResponseEntity.badRequest().body(new ErrorBody("Section not found"));
        if (enrollmentRepo.existsByStudentIdAndSectionId(req.studentId(), req.sectionId())) {
            return ResponseEntity.badRequest().body(new ErrorBody("Student is already enrolled in this section"));
        }

        Enrollment e = new Enrollment();
        e.setStudent(student);
        e.setSection(section);
        if (req.labBatchId() != null) {
            labBatchRepo.findById(req.labBatchId()).ifPresent(e::setLabBatch);
        }
        return ResponseEntity.ok(enrollmentRepo.save(e));
    }

    @PutMapping("/{id}/batch")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> assignBatch(@PathVariable Long id, @RequestParam(required = false) Long labBatchId) {
        return enrollmentRepo.findById(id).map(e -> {
            if (labBatchId == null || labBatchId <= 0) {
                e.setLabBatch(null);
            } else {
                labBatchRepo.findById(labBatchId).ifPresent(e::setLabBatch);
            }
            return ResponseEntity.ok(enrollmentRepo.save(e));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!enrollmentRepo.existsById(id)) return ResponseEntity.notFound().build();
        enrollmentRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
