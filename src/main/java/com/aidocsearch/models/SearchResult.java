package com.aidocsearch.models;

import java.util.Objects;

/**
 * Immutable representation of a search result.
 * Contains a document chunk, its relevance score, and source document information.
 * Implements Comparable for ranking by relevance score and recency.
 */
public final class SearchResult implements Comparable<SearchResult> {
    
    private final DocumentChunk chunk;
    private final double relevanceScore;
    private final String sourceDocument;
    
    private SearchResult(Builder builder) {
        this.chunk = builder.chunk;
        this.relevanceScore = builder.relevanceScore;
        this.sourceDocument = builder.sourceDocument;
    }
    
    public DocumentChunk getChunk() {
        return chunk;
    }
    
    public double getRelevanceScore() {
        return relevanceScore;
    }
    
    public String getSourceDocument() {
        return sourceDocument;
    }
    
    /**
     * Validates that required fields are present and valid.
     * 
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        if (chunk == null) {
            throw new IllegalArgumentException("Chunk cannot be null");
        }
        chunk.validate();
        
        if (relevanceScore < 0.0 || relevanceScore > 1.0) {
            throw new IllegalArgumentException("Relevance score must be between 0.0 and 1.0");
        }
        if (sourceDocument == null || sourceDocument.trim().isEmpty()) {
            throw new IllegalArgumentException("Source document cannot be null or empty");
        }
    }
    
    /**
     * Compares search results by relevance score, then by recency.
     * Results with higher relevance scores come first.
     * If scores are within 0.01 of each other, more recent documents come first.
     * 
     * @param other the other SearchResult to compare to
     * @return negative if this result should come before other, positive if after, 0 if equal
     */
    @Override
    public int compareTo(SearchResult other) {
        Objects.requireNonNull(other, "Cannot compare to null SearchResult");
        
        // If relevance scores are significantly different, sort by score (descending)
        if (Math.abs(this.relevanceScore - other.relevanceScore) >= 0.01) {
            return Double.compare(other.relevanceScore, this.relevanceScore);
        }
        
        // If scores are similar, sort by recency (most recent first)
        return other.chunk.getMetadata().getModifiedAt()
                .compareTo(this.chunk.getMetadata().getModifiedAt());
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public Builder toBuilder() {
        return new Builder()
            .chunk(this.chunk)
            .relevanceScore(this.relevanceScore)
            .sourceDocument(this.sourceDocument);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchResult that = (SearchResult) o;
        return Double.compare(that.relevanceScore, relevanceScore) == 0 &&
               Objects.equals(chunk, that.chunk) &&
               Objects.equals(sourceDocument, that.sourceDocument);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(chunk, relevanceScore, sourceDocument);
    }
    
    @Override
    public String toString() {
        return "SearchResult{" +
               "chunk=" + chunk +
               ", relevanceScore=" + relevanceScore +
               ", sourceDocument='" + sourceDocument + '\'' +
               '}';
    }
    
    public static final class Builder {
        private DocumentChunk chunk;
        private double relevanceScore;
        private String sourceDocument;
        
        private Builder() {}
        
        public Builder chunk(DocumentChunk chunk) {
            this.chunk = chunk;
            return this;
        }
        
        public Builder relevanceScore(double relevanceScore) {
            this.relevanceScore = relevanceScore;
            return this;
        }
        
        public Builder sourceDocument(String sourceDocument) {
            this.sourceDocument = sourceDocument;
            return this;
        }
        
        public SearchResult build() {
            return new SearchResult(this);
        }
    }
}
