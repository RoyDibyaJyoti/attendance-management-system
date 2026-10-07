package com.amcs.faculty;

import com.amcs.auth.User;
import com.amcs.auth.UserRepository;
import com.amcs.department.DepartmentRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/faculty")
public class FacultyController {

    private final FacultyRepository facultyRepo;
    private final UserRepository userRepo;
    private final DepartmentRepository deptRepo;
    private final PasswordEncoder passwordEncoder;

    public FacultyController(FacultyRepository facultyRepo, UserRepository userRepo,
                             DepartmentRepository deptRepo, PasswordEncoder passwordEncoder) {
        this.facultyRepo = facultyRepo;
        this.userRepo = userRepo;
        this.deptRepo = deptRepo;
        this.passwordEncoder = passwordEncoder;
    }

    record FacultyRequest(
        @NotBlank String employeeId,
        @NotBlank String fullName,
        @NotBlank String username,
        String password,    // required only on create
        @NotNull Long departmentId
    ) {}
    record ErrorBody(String message) {}

    record FacultyUpdateRequest(
        @NotBlank String employeeId,
        @NotBlank String fullName,
        @NotNull Long departmentId,
        Boolean active,
        String password
    ) {}

    @GetMapping
    public List<Faculty> list() { return facultyRepo.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<Faculty> get(@PathVariable Long id) {
        return facultyRepo.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> create(@Valid @RequestBody FacultyRequest req) {
        if (facultyRepo.existsByEmployeeId(req.employeeId())) {
            return ResponseEntity.badRequest().body(new ErrorBody("Employee ID already exists"));
        }
        if (userRepo.findByUsername(req.username()).isPresent()) {
            return ResponseEntity.badRequest().body(new ErrorBody("Username already exists"));
        }
        var dept = deptRepo.findById(req.departmentId()).orElse(null);
        if (dept == null) return ResponseEntity.badRequest().body(new ErrorBody("Department not found"));
        if (req.password() == null || req.password().isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorBody("Password is required"));
        }

        User user = new User();
        user.setUsername(req.username());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setRole("FACULTY");
        user.setFullName(req.fullName());
        User savedUser = userRepo.save(user);

        Faculty f = new Faculty();
        f.setEmployeeId(req.employeeId());
        f.setUser(savedUser);
        f.setDepartment(dept);
        return ResponseEntity.ok(facultyRepo.save(f));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody FacultyUpdateRequest req) {
        var opt = facultyRepo.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Faculty f = opt.get();

        if (!f.getEmployeeId().equals(req.employeeId()) && facultyRepo.existsByEmployeeId(req.employeeId())) {
            return ResponseEntity.badRequest().body(new ErrorBody("Employee ID already exists"));
        }
        var dept = deptRepo.findById(req.departmentId()).orElse(null);
        if (dept == null) return ResponseEntity.badRequest().body(new ErrorBody("Department not found"));

        f.setEmployeeId(req.employeeId());
        f.setDepartment(dept);
        if (req.active() != null) {
            f.setActive(req.active());
        }

        User u = f.getUser();
        if (u != null) {
            u.setFullName(req.fullName());
            if (req.active() != null) {
                u.setActive(req.active());
            }
            if (req.password() != null && !req.password().isBlank()) {
                u.setPassword(passwordEncoder.encode(req.password()));
            }
            userRepo.save(u);
        }

        return ResponseEntity.ok(facultyRepo.save(f));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        var opt = facultyRepo.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Faculty f = opt.get();
        User u = f.getUser();
        facultyRepo.delete(f);
        if (u != null) {
            userRepo.delete(u);
        }
        return ResponseEntity.noContent().build();
    }
}
