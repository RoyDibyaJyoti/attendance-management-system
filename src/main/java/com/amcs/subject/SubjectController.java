package com.amcs.subject;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subjects")
public class SubjectController {

    private final SubjectRepository repo;

    public SubjectController(SubjectRepository repo) {
        this.repo = repo;
    }

    record SubjectRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotBlank @Pattern(regexp = "THEORY|LAB") String type,
        int credits
    ) {}
    record ErrorBody(String message) {}

    @GetMapping
    public List<Subject> list() { return repo.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<Subject> get(@PathVariable Long id) {
        return repo.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> create(@Valid @RequestBody SubjectRequest req) {
        if (repo.existsByCode(req.code())) {
            return ResponseEntity.badRequest().body(new ErrorBody("Subject code already exists"));
        }
        Subject s = new Subject();
        s.setCode(req.code().toUpperCase());
        s.setName(req.name());
        s.setType(req.type());
        s.setCredits(req.credits());
        return ResponseEntity.ok(repo.save(s));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody SubjectRequest req) {
        return repo.findById(id).map(s -> {
            s.setCode(req.code().toUpperCase());
            s.setName(req.name());
            s.setType(req.type());
            s.setCredits(req.credits());
            return ResponseEntity.ok(repo.save(s));
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
