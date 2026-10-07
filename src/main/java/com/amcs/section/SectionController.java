package com.amcs.section;

import com.amcs.department.DepartmentRepository;
import com.amcs.term.AcademicTermRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sections")
public class SectionController {

    private final SectionRepository sectionRepo;
    private final DepartmentRepository deptRepo;
    private final AcademicTermRepository termRepo;

    public SectionController(SectionRepository sectionRepo, DepartmentRepository deptRepo, AcademicTermRepository termRepo) {
        this.sectionRepo = sectionRepo;
        this.deptRepo = deptRepo;
        this.termRepo = termRepo;
    }

    record SectionRequest(@NotBlank String name, @NotNull Long departmentId, @NotNull Long termId) {}
    record ErrorBody(String message) {}

    @GetMapping
    public List<Section> list(@RequestParam(required = false) Long termId,
                               @RequestParam(required = false) Long departmentId) {
        if (termId != null) return sectionRepo.findByTermId(termId);
        if (departmentId != null) return sectionRepo.findByDepartmentId(departmentId);
        return sectionRepo.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Section> get(@PathVariable Long id) {
        return sectionRepo.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> create(@Valid @RequestBody SectionRequest req) {
        var dept = deptRepo.findById(req.departmentId()).orElse(null);
        if (dept == null) return ResponseEntity.badRequest().body(new ErrorBody("Department not found"));
        var term = termRepo.findById(req.termId()).orElse(null);
        if (term == null) return ResponseEntity.badRequest().body(new ErrorBody("Term not found"));
        Section s = new Section();
        s.setName(req.name());
        s.setDepartment(dept);
        s.setTerm(term);
        return ResponseEntity.ok(sectionRepo.save(s));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody SectionRequest req) {
        return sectionRepo.findById(id).map(s -> {
            deptRepo.findById(req.departmentId()).ifPresent(s::setDepartment);
            termRepo.findById(req.termId()).ifPresent(s::setTerm);
            s.setName(req.name());
            return ResponseEntity.ok(sectionRepo.save(s));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!sectionRepo.existsById(id)) return ResponseEntity.notFound().build();
        sectionRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
