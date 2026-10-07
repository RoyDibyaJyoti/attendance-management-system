package com.amcs.department;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/departments")
public class DepartmentController {

    private final DepartmentRepository repo;

    public DepartmentController(DepartmentRepository repo) {
        this.repo = repo;
    }

    record DeptRequest(@NotBlank String code, @NotBlank String name) {}

    @GetMapping
    public List<Department> list() { return repo.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<Department> get(@PathVariable Long id) {
        return repo.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> create(@Valid @RequestBody DeptRequest req) {
        if (repo.existsByCode(req.code())) {
            return ResponseEntity.badRequest().body(new ErrorBody("Department code already exists: " + req.code()));
        }
        Department d = new Department();
        d.setCode(req.code().toUpperCase());
        d.setName(req.name());
        return ResponseEntity.ok(repo.save(d));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody DeptRequest req) {
        return repo.findById(id).map(d -> {
            d.setCode(req.code().toUpperCase());
            d.setName(req.name());
            return ResponseEntity.ok(repo.save(d));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    record ErrorBody(String message) {}
}
