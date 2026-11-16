package co.uniquindio.crackscolombia.cracks_colombia_api.admin.controller;

import co.uniquindio.crackscolombia.cracks_colombia_api.admin.dto.request.CreateVerifierRequest;
import co.uniquindio.crackscolombia.cracks_colombia_api.admin.dto.response.VerifierResponse;
import co.uniquindio.crackscolombia.cracks_colombia_api.admin.service.VerifierAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/verifiers")
public class AdminVerifierController {

    private final VerifierAdminService verifierAdminService;

    public AdminVerifierController(VerifierAdminService verifierAdminService) {
        this.verifierAdminService = verifierAdminService;
    }

    @PostMapping
    public ResponseEntity<VerifierResponse> create(@Valid @RequestBody CreateVerifierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(verifierAdminService.create(request));
    }

    @GetMapping
    public List<VerifierResponse> list() {
        return verifierAdminService.list();
    }

    @PatchMapping("/{verifierId}/deactivate")
    public VerifierResponse deactivate(@PathVariable UUID verifierId) {
        return verifierAdminService.deactivate(verifierId);
    }
}
