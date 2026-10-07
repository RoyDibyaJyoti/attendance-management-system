package com.amcs.holiday;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/holidays")
public class HolidayController {

    private final HolidayRepository repo;

    public HolidayController(HolidayRepository repo) {
        this.repo = repo;
    }

    public record HolidayRequest(
            @NotNull LocalDate date,
            @NotBlank String name
    ) {}

    public record ErrorBody(String message) {}

    @GetMapping
    public List<Holiday> list() {
        return repo.findAllByOrderByDateAsc();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Holiday> get(@PathVariable Long id) {
        return repo.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> create(@Valid @RequestBody HolidayRequest req) {
        if (repo.existsByDate(req.date())) {
            return ResponseEntity.badRequest().body(new ErrorBody("A holiday is already declared for date " + req.date()));
        }
        Holiday h = new Holiday(req.date(), req.name().trim());
        return ResponseEntity.ok(repo.save(h));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody HolidayRequest req) {
        return repo.findById(id).map(h -> {
            if (!h.getDate().equals(req.date()) && repo.existsByDate(req.date())) {
                return ResponseEntity.badRequest().body(new ErrorBody("A holiday is already declared for date " + req.date()));
            }
            h.setDate(req.date());
            h.setName(req.name().trim());
            return ResponseEntity.ok(repo.save(h));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
