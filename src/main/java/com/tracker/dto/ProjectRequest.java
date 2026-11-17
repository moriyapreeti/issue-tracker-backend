package com.tracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProjectRequest(
        @NotBlank @Size(max = 120) String name,
        @Pattern(regexp = "^[A-Za-z]{2,10}$", message = "Key must be 2-10 letters") String key,
        String description) {
}
