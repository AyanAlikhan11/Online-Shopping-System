package com.ayan.shop.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;

    @Value("${app.upload.dir:uploads}")
    String uploadDir;

    public List<String> store(List<MultipartFile> files) {
        List<String> urls = new ArrayList<>();
        if (files == null || files.isEmpty()) {
            return urls;
        }

        List<MultipartFile> valid = files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();
        for (MultipartFile file : valid) {
            validate(file);
        }
        if (valid.isEmpty()) {
            return urls;
        }

        Path directory = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + e.getMessage(), e);
        }

        for (MultipartFile file : valid) {
            String extension = resolveExtension(file.getOriginalFilename());
            String fileName = UUID.randomUUID() + extension;
            Path destination = directory.resolve(fileName).normalize();

            if (!destination.startsWith(directory)) {
                throw new IllegalArgumentException("Invalid file destination path");
            }

            try (InputStream in = file.getInputStream()) {
                Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
                urls.add("/uploads/" + fileName);
            } catch (IOException e) {
                throw new RuntimeException("Failed to save uploaded file: " + e.getMessage(), e);
            }
        }
        return urls;
    }

    private void validate(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File " + file.getOriginalFilename() + " exceeds maximum allowed size of 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Unsupported file type: " + contentType + ". Allowed: JPEG, PNG, WEBP, GIF");
        }
    }

    private String resolveExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return ".jpg";
        }
        String extension = fileName.substring(fileName.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        return switch (extension) {
            case ".png" -> ".png";
            case ".webp" -> ".webp";
            case ".gif" -> ".gif";
            default -> ".jpg";
        };
    }
}
