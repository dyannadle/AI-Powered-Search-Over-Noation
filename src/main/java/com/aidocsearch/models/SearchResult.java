package com.aidocsearch.models;

import lombok.Builder;
import lombok.Value;

import java.util.Objects;

/**
 * Immutable representation of a search result.
 * Contains a document chunk, its relevance score, and source document information.
 * Implements Comparable for ranking by relevance score and recency.
 */
@Value
@Builder(toBuilder = true)
public class SearchResult implements Comparable<SearchResult> {
    
    DocumentChunk chunk;
    double relevanceScore;
    String sourceDocument;
    
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
        return other.chunk.metadata.modifiedAt
                .compareTo(this.chunk.metadata.modifiedAt);
    }
}
