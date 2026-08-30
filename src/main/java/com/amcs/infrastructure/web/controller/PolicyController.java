package com.amcs.infrastructure.web.controller;

import com.amcs.application.dto.policy.AttendancePolicyResponse;
import com.amcs.application.dto.policy.CreateAttendancePolicyRequest;
import com.amcs.application.dto.policy.CreateOverallAttendancePolicyRequest;
import com.amcs.application.dto.policy.OverallAttendancePolicyResponse;
import com.amcs.application.service.PolicyApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/policies")
@Tag(name = "Attendance Policies", description = "Versioned, immutable attendance rules and thresholds")
public class PolicyController {

    private final PolicyApplicationService policyService;

    public PolicyController(PolicyApplicationService policyService) {
        this.policyService = Objects.requireNonNull(policyService, "policyService");
    }

    @PostMapping
    @Operation(summary = "Publish a new immutable version of an attendance policy")
    public ResponseEntity<AttendancePolicyResponse> createPolicy(
        @Valid @RequestBody CreateAttendancePolicyRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(policyService.createPolicy(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get policy snapshot by ID")
    public ResponseEntity<AttendancePolicyResponse> getPolicyById(@PathVariable UUID id) {
        return ResponseEntity.ok(policyService.getPolicyById(id));
    }

    @GetMapping("/{name}/versions")
    @Operation(summary = "List all historical versions of a policy")
    public ResponseEntity<List<AttendancePolicyResponse>> listPolicyVersions(@PathVariable String name) {
        return ResponseEntity.ok(policyService.listPolicyVersions(name));
    }

    @PostMapping("/overall")
    @Operation(summary = "Publish a new overall attendance aggregation policy")
    public ResponseEntity<OverallAttendancePolicyResponse> createOverallPolicy(
        @Valid @RequestBody CreateOverallAttendancePolicyRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(policyService.createOverallPolicy(request));
    }
}
