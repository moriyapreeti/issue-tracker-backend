package com.tracker.service;

import com.tracker.exception.BadRequestException;
import com.tracker.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final Set<String> ALLOWED_EXT = Set.of("png", "jpg", "jpeg", "gif", "webp");
    private final Path root;

    public FileStorageService(@Value("${app.upload-dir}") String dir) throws IOException {
        this.root = Paths.get(dir).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BadRequestException("File is empty");
        String type = file.getContentType();
        if (type == null || !type.startsWith("image/")) throw new BadRequestException("Only image files are allowed");
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dot = original.lastIndexOf('.');
        String ext = dot >= 0 ? original.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
        if (!ALLOWED_EXT.contains(ext)) throw new BadRequestException("Allowed types: png, jpg, jpeg, gif, webp");
        String name = UUID.randomUUID() + "." + ext;
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, root.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BadRequestException("Could not store file");
        }
        return name;
    }

    public Resource load(String name) {
        try {
            Path p = root.resolve(name).normalize();
            if (!p.startsWith(root) || !Files.exists(p)) throw new ResourceNotFoundException("File not found");
            return new UrlResource(p.toUri());
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("File not found");
        }
    }

    public String contentType(String name) {
        try {
            String t = Files.probeContentType(root.resolve(name).normalize());
            return t != null ? t : "application/octet-stream";
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }

    public void delete(String name) {
        if (name == null) return;
        try {
            Path p = root.resolve(name).normalize();
            if (p.startsWith(root)) Files.deleteIfExists(p);
        } catch (IOException ignored) {
            // best effort
        }
    }
}
