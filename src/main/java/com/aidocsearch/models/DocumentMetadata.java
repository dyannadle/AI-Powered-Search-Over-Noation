package com.aidocsearch.models;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

/**
 * Immutable metadata associated with a document.
 * Contains information about authorship, timestamps, location, and permissions.
 */
@Value
@Builder(toBuilder = true)
public class DocumentMetadata {
    
    String author;
    Instant createdAt;
    Instant modifiedAt;
    String url;
    List<String> permissions;
    
    /**
     * Validates that required fields are present and valid.
     * 
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        if (author == null || author.trim().isEmpty()) {
            throw new IllegalArgumentException("Author cannot be null or empty");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("Created timestamp cannot be null");
        }
        if (modifiedAt == null) {
            throw new IllegalArgumentException("Modified timestamp cannot be null");
        }
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }
        if (permissions == null) {
            throw new IllegalArgumentException("Permissions list cannot be null");
        }
        if (modifiedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("Modified timestamp cannot be before created timestamp");
        }
    }
}
