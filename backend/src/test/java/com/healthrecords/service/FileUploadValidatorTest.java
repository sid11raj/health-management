package com.healthrecords.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileUploadValidatorTest {

    private final FileUploadValidator validator = new FileUploadValidator(
            1024, "application/pdf,image/png", "pdf,png");

    @Test
    void acceptsAllowedPdf() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", "content".getBytes());
        assertThat(validator.validateAndCleanFilename(file)).isEqualTo("report.pdf");
    }

    @Test
    void rejectsExecutableExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "evil.exe", "application/octet-stream", "content".getBytes());
        assertThatThrownBy(() -> validator.validateAndCleanFilename(file))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsOversizedFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "big.pdf", "application/pdf", new byte[2048]);
        assertThatThrownBy(() -> validator.validateAndCleanFilename(file))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void stripsDirectoryComponentsFromFilename() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "../../etc/report.pdf", "application/pdf", "content".getBytes());
        assertThat(validator.validateAndCleanFilename(file)).isEqualTo("report.pdf");
    }
}
