package com.amcs.lab;

import com.amcs.section.SectionRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lab-batches")
public class LabBatchController {

    private final LabBatchRepository labBatchRepo;
    private final SectionRepository sectionRepo;

    public LabBatchController(LabBatchRepository labBatchRepo, SectionRepository sectionRepo) {
        this.labBatchRepo = labBatchRepo;
        this.sectionRepo = sectionRepo;
    }

    record BatchRequest(@NotBlank String name, @NotNull Long sectionId) {}
    record ErrorBody(String message) {}

    @GetMapping
    public List<LabBatch> list(@RequestParam(required = false) Long sectionId) {
        if (sectionId != null) return labBatchRepo.findBySectionId(sectionId);
        return labBatchRepo.findAll();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> create(@Valid @RequestBody BatchRequest req) {
        var section = sectionRepo.findById(req.sectionId()).orElse(null);
        if (section == null) return ResponseEntity.badRequest().body(new ErrorBody("Section not found"));
        if (labBatchRepo.existsByNameAndSectionId(req.name(), req.sectionId())) {
            return ResponseEntity.badRequest().body(new ErrorBody("Batch name already exists in this section"));
        }
        LabBatch lb = new LabBatch();
        lb.setName(req.name());
        lb.setSection(section);
        return ResponseEntity.ok(labBatchRepo.save(lb));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody BatchRequest req) {
        return labBatchRepo.findById(id).map(lb -> {
            var section = sectionRepo.findById(req.sectionId()).orElse(null);
            if (section == null) return ResponseEntity.badRequest().body(new ErrorBody("Section not found"));
            if ((!lb.getName().equals(req.name()) || !lb.getSection().getId().equals(req.sectionId()))
                && labBatchRepo.existsByNameAndSectionId(req.name(), req.sectionId())) {
                return ResponseEntity.badRequest().body(new ErrorBody("Batch name already exists in this section"));
            }
            lb.setName(req.name());
            lb.setSection(section);
            return ResponseEntity.ok(labBatchRepo.save(lb));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!labBatchRepo.existsById(id)) return ResponseEntity.notFound().build();
        labBatchRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
