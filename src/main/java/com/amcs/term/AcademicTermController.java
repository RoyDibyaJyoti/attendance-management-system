package com.amcs.term;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/terms")
public class AcademicTermController {

    private final AcademicTermRepository repo;

    public AcademicTermController(AcademicTermRepository repo) {
        this.repo = repo;
    }

    record TermRequest(
        @NotBlank String name,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        boolean active
    ) {}

    @GetMapping
    public List<AcademicTerm> list() {
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<AcademicTerm> get(@PathVariable Long id) {
        return repo.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<AcademicTerm> create(@Valid @RequestBody TermRequest req) {
        AcademicTerm t = new AcademicTerm();
        t.setName(req.name());
        t.setStartDate(req.startDate());
        t.setEndDate(req.endDate());
        t.setActive(req.active());
        return ResponseEntity.ok(repo.save(t));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<AcademicTerm> update(@PathVariable Long id, @Valid @RequestBody TermRequest req) {
        return repo.findById(id).map(t -> {
            t.setName(req.name());
            t.setStartDate(req.startDate());
            t.setEndDate(req.endDate());
            t.setActive(req.active());
            return ResponseEntity.ok(repo.save(t));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
