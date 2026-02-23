package com.aidocsearch.models;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable metadata associated with a document.
 * Contains information about authorship, timestamps, location, and permissions.
 */
public final class DocumentMetadata {
    
    private final String author;
    private final Instant createdAt;
    private final Instant modifiedAt;
    private final String url;
    private final List<String> permissions;
    
    private DocumentMetadata(Builder builder) {
        this.author = builder.author;
        this.createdAt = builder.createdAt;
        this.modifiedAt = builder.modifiedAt;
        this.url = builder.url;
        this.permissions = builder.permissions != null 
            ? Collections.unmodifiableList(new ArrayList<>(builder.permissions))
            : null;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public Instant getModifiedAt() {
        return modifiedAt;
    }
    
    public String getUrl() {
        return url;
    }
    
    public List<String> getPermissions() {
        return permissions;
    }
    
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
    
    public static Builder builder() {
        return new Builder();
    }
    
    public Builder toBuilder() {
        return new Builder()
            .author(this.author)
            .createdAt(this.createdAt)
            .modifiedAt(this.modifiedAt)
            .url(this.url)
            .permissions(new ArrayList<>(this.permissions));
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocumentMetadata that = (DocumentMetadata) o;
        return Objects.equals(author, that.author) &&
               Objects.equals(createdAt, that.createdAt) &&
               Objects.equals(modifiedAt, that.modifiedAt) &&
               Objects.equals(url, that.url) &&
               Objects.equals(permissions, that.permissions);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(author, createdAt, modifiedAt, url, permissions);
    }
    
    @Override
    public String toString() {
        return "DocumentMetadata{" +
               "author='" + author + '\'' +
               ", createdAt=" + createdAt +
               ", modifiedAt=" + modifiedAt +
               ", url='" + url + '\'' +
               ", permissions=" + permissions +
               '}';
    }
    
    public static final class Builder {
        private String author;
        private Instant createdAt;
        private Instant modifiedAt;
        private String url;
        private List<String> permissions;
        
        private Builder() {}
        
        public Builder author(String author) {
            this.author = author;
            return this;
        }
        
        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public Builder modifiedAt(Instant modifiedAt) {
            this.modifiedAt = modifiedAt;
            return this;
        }
        
        public Builder url(String url) {
            this.url = url;
            return this;
        }
        
        public Builder permissions(List<String> permissions) {
            this.permissions = permissions;
            return this;
        }
        
        public DocumentMetadata build() {
            return new DocumentMetadata(this);
        }
    }
}
