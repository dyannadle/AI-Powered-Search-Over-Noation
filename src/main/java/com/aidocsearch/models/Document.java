package com.aidocsearch.models;

import lombok.Builder;
import lombok.Value;

/**
 * Immutable representation of a document from an external source (Notion or Google Drive).
 * Contains the document's content, metadata, and source information.
 */
@Value
@Builder(toBuilder = true)
public class Document {
    
    String id;
    String source;
    String title;
    String content;
    DocumentMetadata metadata;
    
    /**
     * Validates that required fields are present and valid.
     * 
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Document ID cannot be null or empty");
        }
        if (source == null || source.trim().isEmpty()) {
            throw new IllegalArgumentException("Source cannot be null or empty");
        }
        if (!source.equals("notion") && !source.equals("google_drive")) {
            throw new IllegalArgumentException("Source must be 'notion' or 'google_drive'");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title cannot be null or empty");
        }
        if (content == null) {
            throw new IllegalArgumentException("Content cannot be null");
        }
        if (metadata == null) {
            throw new IllegalArgumentException("Metadata cannot be null");
        }
        metadata.validate();
    }
}
