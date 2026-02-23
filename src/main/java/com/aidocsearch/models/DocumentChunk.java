package com.aidocsearch.models;

import java.util.Objects;

/**
 * Immutable representation of a chunk of a document.
 * Documents are split into chunks for embedding and retrieval.
 */
public final class DocumentChunk {
    
    private final String id;
    private final String documentId;
    private final String content;
    private final int chunkIndex;
    private final DocumentMetadata metadata;
    
    private DocumentChunk(Builder builder) {
        this.id = builder.id;
        this.documentId = builder.documentId;
        this.content = builder.content;
        this.chunkIndex = builder.chunkIndex;
        this.metadata = builder.metadata;
    }
    
    public String getId() {
        return id;
    }
    
    public String getDocumentId() {
        return documentId;
    }
    
    public String getContent() {
        return content;
    }
    
    public int getChunkIndex() {
        return chunkIndex;
    }
    
    public DocumentMetadata getMetadata() {
        return metadata;
    }
    
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
    
    public static Builder builder() {
        return new Builder();
    }
    
    public Builder toBuilder() {
        return new Builder()
            .id(this.id)
            .documentId(this.documentId)
            .content(this.content)
            .chunkIndex(this.chunkIndex)
            .metadata(this.metadata);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocumentChunk that = (DocumentChunk) o;
        return chunkIndex == that.chunkIndex &&
               Objects.equals(id, that.id) &&
               Objects.equals(documentId, that.documentId) &&
               Objects.equals(content, that.content) &&
               Objects.equals(metadata, that.metadata);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, documentId, content, chunkIndex, metadata);
    }
    
    @Override
    public String toString() {
        return "DocumentChunk{" +
               "id='" + id + '\'' +
               ", documentId='" + documentId + '\'' +
               ", content='" + content + '\'' +
               ", chunkIndex=" + chunkIndex +
               ", metadata=" + metadata +
               '}';
    }
    
    public static final class Builder {
        private String id;
        private String documentId;
        private String content;
        private int chunkIndex;
        private DocumentMetadata metadata;
        
        private Builder() {}
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder documentId(String documentId) {
            this.documentId = documentId;
            return this;
        }
        
        public Builder content(String content) {
            this.content = content;
            return this;
        }
        
        public Builder chunkIndex(int chunkIndex) {
            this.chunkIndex = chunkIndex;
            return this;
        }
        
        public Builder metadata(DocumentMetadata metadata) {
            this.metadata = metadata;
            return this;
        }
        
        public DocumentChunk build() {
            return new DocumentChunk(this);
        }
    }
}
