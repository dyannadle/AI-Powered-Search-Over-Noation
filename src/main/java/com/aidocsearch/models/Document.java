package com.aidocsearch.models;

import java.util.Objects;

/**
 * Immutable representation of a document from an external source (Notion or Google Drive).
 * Contains the document's content, metadata, and source information.
 */
public final class Document {
    
    private final String id;
    private final String source;
    private final String title;
    private final String content;
    private final DocumentMetadata metadata;
    
    private Document(Builder builder) {
        this.id = builder.id;
        this.source = builder.source;
        this.title = builder.title;
        this.content = builder.content;
        this.metadata = builder.metadata;
    }
    
    public String getId() {
        return id;
    }
    
    public String getSource() {
        return source;
    }
    
    public String getTitle() {
        return title;
    }
    
    public String getContent() {
        return content;
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
    
    public static Builder builder() {
        return new Builder();
    }
    
    public Builder toBuilder() {
        return new Builder()
            .id(this.id)
            .source(this.source)
            .title(this.title)
            .content(this.content)
            .metadata(this.metadata);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Document document = (Document) o;
        return Objects.equals(id, document.id) &&
               Objects.equals(source, document.source) &&
               Objects.equals(title, document.title) &&
               Objects.equals(content, document.content) &&
               Objects.equals(metadata, document.metadata);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, source, title, content, metadata);
    }
    
    @Override
    public String toString() {
        return "Document{" +
               "id='" + id + '\'' +
               ", source='" + source + '\'' +
               ", title='" + title + '\'' +
               ", content='" + content + '\'' +
               ", metadata=" + metadata +
               '}';
    }
    
    public static final class Builder {
        private String id;
        private String source;
        private String title;
        private String content;
        private DocumentMetadata metadata;
        
        private Builder() {}
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder source(String source) {
            this.source = source;
            return this;
        }
        
        public Builder title(String title) {
            this.title = title;
            return this;
        }
        
        public Builder content(String content) {
            this.content = content;
            return this;
        }
        
        public Builder metadata(DocumentMetadata metadata) {
            this.metadata = metadata;
            return this;
        }
        
        public Document build() {
            return new Document(this);
        }
    }
}
