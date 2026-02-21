package com.aidocsearch.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SearchResultTest {

    private DocumentChunk validChunk;
    private DocumentMetadata validMetadata;

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();
        validMetadata = DocumentMetadata.builder()
                .author("John Doe")
                .createdAt(now)
                .modifiedAt(now)
                .url("https://example.com/doc")
                .permissions(Collections.emptyList())
                .build();
        
        validChunk = DocumentChunk.builder()
                .id("chunk-1")
                .documentId("doc-123")
                .content("Test content")
                .chunkIndex(0)
                .metadata(validMetadata)
                .build();
    }

    @Test
    void shouldCreateValidSearchResult() {
        SearchResult result = SearchResult.builder()
                .chunk(validChunk)
                .relevanceScore(0.85)
                .sourceDocument("doc-123")
                .build();
        
        assertNotNull(result);
        assertEquals(validChunk, result.chunk);
        assertEquals(0.85, result.relevanceScore);
        assertEquals("doc-123", result.sourceDocument);
    }

    @Test
    void shouldRejectNullChunk() {
        assertThrows(IllegalArgumentException.class, () -> {
            SearchResult result = SearchResult.builder()
                    .chunk(null)
                    .relevanceScore(0.85)
                    .sourceDocument("doc-123")
                    .build();
            result.validate();
        });
    }

    @Test
    void shouldRejectNegativeRelevanceScore() {
        assertThrows(IllegalArgumentException.class, () -> {
            SearchResult result = SearchResult.builder()
                    .chunk(validChunk)
                    .relevanceScore(-0.1)
                    .sourceDocument("doc-123")
                    .build();
            result.validate();
        });
    }

    @Test
    void shouldRejectRelevanceScoreAboveOne() {
        assertThrows(IllegalArgumentException.class, () -> {
            SearchResult result = SearchResult.builder()
                    .chunk(validChunk)
                    .relevanceScore(1.1)
                    .sourceDocument("doc-123")
                    .build();
            result.validate();
        });
    }

    @Test
    void shouldAcceptZeroRelevanceScore() {
        SearchResult result = SearchResult.builder()
                .chunk(validChunk)
                .relevanceScore(0.0)
                .sourceDocument("doc-123")
                .build();
        
        result.validate();
        assertEquals(0.0, result.relevanceScore);
    }

    @Test
    void shouldAcceptOneRelevanceScore() {
        SearchResult result = SearchResult.builder()
                .chunk(validChunk)
                .relevanceScore(1.0)
                .sourceDocument("doc-123")
                .build();
        
        result.validate();
        assertEquals(1.0, result.relevanceScore);
    }

    @Test
    void shouldSortByRelevanceScoreDescending() {
        SearchResult high = SearchResult.builder()
                .chunk(validChunk)
                .relevanceScore(0.9)
                .sourceDocument("doc-1")
                .build();
        
        SearchResult low = SearchResult.builder()
                .chunk(validChunk)
                .relevanceScore(0.5)
                .sourceDocument("doc-2")
                .build();
        
        List<SearchResult> results = Arrays.asList(low, high);
        Collections.sort(results);
        
        assertEquals(0.9, results.get(0).relevanceScore);
        assertEquals(0.5, results.get(1).relevanceScore);
    }

    @Test
    void shouldSortByRecencyWhenScoresAreSimilar() {
        Instant older = Instant.parse("2024-01-01T00:00:00Z");
        Instant newer = Instant.parse("2024-02-01T00:00:00Z");
        
        DocumentMetadata olderMetadata = validMetadata.toBuilder()
                .modifiedAt(older)
                .build();
        
        DocumentMetadata newerMetadata = validMetadata.toBuilder()
                .modifiedAt(newer)
                .build();
        
        DocumentChunk olderChunk = validChunk.toBuilder()
                .metadata(olderMetadata)
                .build();
        
        DocumentChunk newerChunk = validChunk.toBuilder()
                .metadata(newerMetadata)
                .build();
        
        SearchResult olderResult = SearchResult.builder()
                .chunk(olderChunk)
                .relevanceScore(0.85)
                .sourceDocument("doc-1")
                .build();
        
        SearchResult newerResult = SearchResult.builder()
                .chunk(newerChunk)
                .relevanceScore(0.85)
                .sourceDocument("doc-2")
                .build();
        
        List<SearchResult> results = Arrays.asList(olderResult, newerResult);
        Collections.sort(results);
        
        // Newer document should come first when scores are equal
        assertEquals(newer, results.get(0).chunk.metadata.modifiedAt);
        assertEquals(older, results.get(1).chunk.metadata.modifiedAt);
    }

    @Test
    void shouldTreatScoresWithin0_01AsSimilar() {
        Instant older = Instant.parse("2024-01-01T00:00:00Z");
        Instant newer = Instant.parse("2024-02-01T00:00:00Z");
        
        DocumentMetadata olderMetadata = validMetadata.toBuilder()
                .modifiedAt(older)
                .build();
        
        DocumentMetadata newerMetadata = validMetadata.toBuilder()
                .modifiedAt(newer)
                .build();
        
        DocumentChunk olderChunk = validChunk.toBuilder()
                .metadata(olderMetadata)
                .build();
        
        DocumentChunk newerChunk = validChunk.toBuilder()
                .metadata(newerMetadata)
                .build();
        
        SearchResult slightlyHigher = SearchResult.builder()
                .chunk(olderChunk)
                .relevanceScore(0.851)
                .sourceDocument("doc-1")
                .build();
        
        SearchResult slightlyLower = SearchResult.builder()
                .chunk(newerChunk)
                .relevanceScore(0.849)
                .sourceDocument("doc-2")
                .build();
        
        List<SearchResult> results = Arrays.asList(slightlyHigher, slightlyLower);
        Collections.sort(results);
        
        // Should sort by recency since scores differ by less than 0.01
        assertEquals(newer, results.get(0).chunk.metadata.modifiedAt);
        assertEquals(older, results.get(1).chunk.metadata.modifiedAt);
    }
}
