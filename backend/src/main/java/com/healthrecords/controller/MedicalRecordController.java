package com.healthrecords.controller;

import com.healthrecords.dto.RecordDtos.MedicalRecordRequest;
import com.healthrecords.dto.RecordDtos.MedicalRecordResponse;
import com.healthrecords.service.MedicalRecordService;
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
@RequestMapping("/api/medical-records")
public class MedicalRecordController {

    private final MedicalRecordService service;

    public MedicalRecordController(MedicalRecordService service) {
        this.service = service;
    }

    @GetMapping
    public List<MedicalRecordResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long patientId) {
        return service.listFiltered(search, patientId);
    }

    @GetMapping("/paged")
    public Page<MedicalRecordResponse> listPaged(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long patientId) {
        return service.listPaged(pageable, search, patientId);
    }

    @GetMapping("/{id}")
    public MedicalRecordResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<MedicalRecordResponse> create(@Valid @RequestBody MedicalRecordRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public MedicalRecordResponse update(@PathVariable Long id, @Valid @RequestBody MedicalRecordRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
