package com.healthrecords.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates lab-report uploads before anything touches the disk:
 * non-empty, within the configured size limit, with an allowed extension
 * and (when supplied) an allowed content type, and a filename that cannot
 * escape the upload directory. Content-type alone is not trusted — the
 * extension check is the primary gate, and the multipart size limit in
 * application.properties is a second, independent limit.
 */
@Component
public class FileUploadValidator {

    private final long maxFileSizeBytes;
    private final Set<String> allowedContentTypes;
    private final Set<String> allowedExtensions;

    public FileUploadValidator(
            @Value("${app.upload.max-file-size-bytes:10485760}") long maxFileSizeBytes,
            @Value("${app.upload.allowed-content-types:application/pdf,image/png,image/jpeg,text/plain}") String allowedContentTypes,
            @Value("${app.upload.allowed-extensions:pdf,png,jpg,jpeg,txt}") String allowedExtensions) {
        this.maxFileSizeBytes = maxFileSizeBytes;
        this.allowedContentTypes = split(allowedContentTypes);
        this.allowedExtensions = split(allowedExtensions);
    }

    /** Returns the cleaned, basename-only filename, or throws IllegalArgumentException. */
    public String validateAndCleanFilename(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("A file is required for a lab report.");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new IllegalArgumentException(
                    "File is too large. Maximum size is " + (maxFileSizeBytes / 1024 / 1024) + " MB.");
        }
        String original = StringUtils.cleanPath(Objects.requireNonNullElse(file.getOriginalFilename(), ""));
        // Strip any directory components a client may have smuggled in.
        String basename = original.replace('\\', '/');
        if (basename.contains("/")) {
            basename = basename.substring(basename.lastIndexOf('/') + 1);
        }
        if (basename.isBlank() || basename.contains("..")) {
            throw new IllegalArgumentException("Invalid file name.");
        }
        String extension = extensionOf(basename);
        if (!allowedExtensions.contains(extension)) {
            throw new IllegalArgumentException("File type not allowed. Allowed types: " + allowedExtensions);
        }
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank() && !allowedContentTypes.contains(contentType)) {
            throw new IllegalArgumentException("File content type not allowed: " + contentType);
        }
        return basename;
    }

    public static String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static Set<String> split(String value) {
        return Arrays.stream(value.split(","))
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }
}
