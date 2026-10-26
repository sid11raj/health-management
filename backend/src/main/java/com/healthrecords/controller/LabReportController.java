package com.healthrecords.controller;

import com.healthrecords.dto.RecordDtos.LabReportResponse;
import com.healthrecords.model.LabReport;
import com.healthrecords.service.LabReportService;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/lab-reports")
public class LabReportController {

    private final LabReportService service;

    public LabReportController(LabReportService service) {
        this.service = service;
    }

    @GetMapping
    public List<LabReportResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long patientId) {
        return service.listFiltered(search, patientId);
    }

    @GetMapping("/paged")
    public Page<LabReportResponse> listPaged(
            @PageableDefault(size = 20, sort = "uploadedAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long patientId) {
        return service.listPaged(pageable, search, patientId);
    }

    @GetMapping("/{id}")
    public LabReportResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LabReportResponse> upload(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam Long patientId,
            @RequestParam(required = false) Long medicalRecordId,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.status(201)
                .body(service.upload(title, description, patientId, medicalRecordId, file));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) throws IOException {
        LabReport report = service.findForDownload(id);
        Resource resource = service.download(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + report.getOriginalFileName() + "\"")
                .contentType(MediaType.parseMediaType(
                        report.getContentType() != null ? report.getContentType() : "application/octet-stream"))
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) throws IOException {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
