package com.aidocsearch.models;

import lombok.Builder;
import lombok.Value;

/**
 * Immutable representation of a chunk of a document.
 * Documents are split into chunks for embedding and retrieval.
 */
@Value
@Builder(toBuilder = true)
public class DocumentChunk {
    
    String id;
    String documentId;
    String content;
    int chunkIndex;
    DocumentMetadata metadata;
    
    /**
     * Validates that required fields are present and valid.
     * 
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Chunk ID cannot be null or empty");
        }
        if (documentId == null || documentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Document ID cannot be null or empty");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Content cannot be null or empty");
        }
        if (chunkIndex < 0) {
            throw new IllegalArgumentException("Chunk index cannot be negative");
        }
        if (metadata == null) {
            throw new IllegalArgumentException("Metadata cannot be null");
        }
        metadata.validate();
    }
}
