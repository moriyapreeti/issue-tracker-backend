package com.tracker.controller;

import com.tracker.service.FileStorageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/files")
@Tag(name = "Files")
public class FileController {
    private final FileStorageService files;

    public FileController(FileStorageService files) {
        this.files = files;
    }

    /** Public (random UUID file names) so <img> tags can load screenshots without an auth header. */
    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> get(@PathVariable String filename) {
        Resource r = files.load(filename);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(files.contentType(filename)))
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS))
                .body(r);
    }
}
