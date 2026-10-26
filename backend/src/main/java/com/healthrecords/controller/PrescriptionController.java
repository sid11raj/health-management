package com.healthrecords.controller;

import com.healthrecords.dto.RecordDtos.PrescriptionRequest;
import com.healthrecords.dto.RecordDtos.PrescriptionResponse;
import com.healthrecords.service.PrescriptionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prescriptions")
public class PrescriptionController {

    private final PrescriptionService service;

    public PrescriptionController(PrescriptionService service) {
        this.service = service;
    }

    @GetMapping
    public List<PrescriptionResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long patientId) {
        return service.listFiltered(search, patientId);
    }

    @GetMapping("/paged")
    public Page<PrescriptionResponse> listPaged(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long patientId) {
        return service.listPaged(pageable, search, patientId);
    }

    @GetMapping("/{id}")
    public PrescriptionResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<PrescriptionResponse> create(@Valid @RequestBody PrescriptionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public PrescriptionResponse update(@PathVariable Long id, @Valid @RequestBody PrescriptionRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
