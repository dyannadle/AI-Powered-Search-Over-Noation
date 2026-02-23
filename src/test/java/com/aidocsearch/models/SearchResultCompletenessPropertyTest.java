package com.aidocsearch.models;

import net.jqwik.api.*;
import net.jqwik.api.constraints.NotBlank;
import net.jqwik.api.constraints.NotEmpty;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for search result completeness.
 * 
 * **Validates: Requirements 5.4**
 * 
 * Property 21: Result completeness
 * For any search result returned, it should include the original text content, 
 * all source document metadata, and a relevance score.
 */
class SearchResultCompletenessPropertyTest {

    @Property(tries = 100)
    void searchResultShouldContainOriginalTextContent(
            @ForAll @NotBlank String chunkId,
            @ForAll @NotBlank String documentId,
            @ForAll @NotBlank String content,
            @ForAll("validChunkIndex") int chunkIndex,
            @ForAll("validMetadata") DocumentMetadata metadata,
            @ForAll("validRelevanceScore") double relevanceScore,
            @ForAll @NotBlank String sourceDocument
    ) {
        // Arrange: Create a document chunk with content
        DocumentChunk chunk = DocumentChunk.builder()
                .id(chunkId)
                .documentId(documentId)
                .content(content)
                .chunkIndex(chunkIndex)
                .metadata(metadata)
                .build();
        
        // Act: Create a search result
        SearchResult result = SearchResult.builder()
                .chunk(chunk)
                .relevanceScore(relevanceScore)
                .sourceDocument(sourceDocument)
                .build();
        
        // Assert: Verify original text content is present and accessible
        assertNotNull(result.getChunk(), "Search result should contain a chunk");
        assertNotNull(result.getChunk().getContent(), "Chunk should contain content");
        assertEquals(content, result.getChunk().getContent(), 
                "Original text content should be preserved exactly");
        assertFalse(result.getChunk().getContent().trim().isEmpty(), 
                "Content should not be empty");
    }

    @Property(tries = 100)
    void searchResultShouldContainAllSourceDocumentMetadata(
            @ForAll @NotBlank String chunkId,
            @ForAll @NotBlank String documentId,
            @ForAll @NotBlank String content,
            @ForAll("validChunkIndex") int chunkIndex,
            @ForAll @NotBlank String author,
            @ForAll("validTimestamps") Instant[] timestamps,
            @ForAll @NotBlank String url,
            @ForAll @NotEmpty List<@NotBlank String> permissions,
            @ForAll("validRelevanceScore") double relevanceScore,
            @ForAll @NotBlank String sourceDocument
    ) {
        // Arrange: Create metadata with all required fields
        Instant createdAt = timestamps[0];
        Instant modifiedAt = timestamps[1];
        
        DocumentMetadata metadata = DocumentMetadata.builder()
                .author(author)
                .createdAt(createdAt)
                .modifiedAt(modifiedAt)
                .url(url)
                .permissions(permissions)
                .build();
        
        DocumentChunk chunk = DocumentChunk.builder()
                .id(chunkId)
                .documentId(documentId)
                .content(content)
                .chunkIndex(chunkIndex)
                .metadata(metadata)
                .build();
        
        // Act: Create a search result
        SearchResult result = SearchResult.builder()
                .chunk(chunk)
                .relevanceScore(relevanceScore)
                .sourceDocument(sourceDocument)
                .build();
        
        // Assert: Verify all metadata fields are present and preserved
        assertNotNull(result.getChunk().getMetadata(), 
                "Search result should contain metadata");
        
        // Verify author
        assertNotNull(result.getChunk().getMetadata().getAuthor(), 
                "Metadata should contain author");
        assertEquals(author, result.getChunk().getMetadata().getAuthor(), 
                "Author should be preserved");
        
        // Verify timestamps
        assertNotNull(result.getChunk().getMetadata().getCreatedAt(), 
                "Metadata should contain creation timestamp");
        assertEquals(createdAt, result.getChunk().getMetadata().getCreatedAt(), 
                "Creation timestamp should be preserved");
        
        assertNotNull(result.getChunk().getMetadata().getModifiedAt(), 
                "Metadata should contain modification timestamp");
        assertEquals(modifiedAt, result.getChunk().getMetadata().getModifiedAt(), 
                "Modification timestamp should be preserved");
        
        // Verify URL
        assertNotNull(result.getChunk().getMetadata().getUrl(), 
                "Metadata should contain URL");
        assertEquals(url, result.getChunk().getMetadata().getUrl(), 
                "URL should be preserved");
        
        // Verify permissions
        assertNotNull(result.getChunk().getMetadata().getPermissions(), 
                "Metadata should contain permissions");
        assertEquals(permissions, result.getChunk().getMetadata().getPermissions(), 
                "Permissions should be preserved");
    }

    @Property(tries = 100)
    void searchResultShouldContainRelevanceScore(
            @ForAll @NotBlank String chunkId,
            @ForAll @NotBlank String documentId,
            @ForAll @NotBlank String content,
            @ForAll("validChunkIndex") int chunkIndex,
            @ForAll("validMetadata") DocumentMetadata metadata,
            @ForAll("validRelevanceScore") double relevanceScore,
            @ForAll @NotBlank String sourceDocument
    ) {
        // Arrange: Create a document chunk
        DocumentChunk chunk = DocumentChunk.builder()
                .id(chunkId)
                .documentId(documentId)
                .content(content)
                .chunkIndex(chunkIndex)
                .metadata(metadata)
                .build();
        
        // Act: Create a search result with a relevance score
        SearchResult result = SearchResult.builder()
                .chunk(chunk)
                .relevanceScore(relevanceScore)
                .sourceDocument(sourceDocument)
                .build();
        
        // Assert: Verify relevance score is present and valid
        double actualScore = result.getRelevanceScore();
        assertEquals(relevanceScore, actualScore, 0.0001, 
                "Relevance score should be preserved exactly");
        assertTrue(actualScore >= 0.0 && actualScore <= 1.0, 
                "Relevance score should be between 0.0 and 1.0");
    }

    @Property(tries = 100)
    void searchResultShouldContainAllThreeComponents(
            @ForAll @NotBlank String chunkId,
            @ForAll @NotBlank String documentId,
            @ForAll @NotBlank String content,
            @ForAll("validChunkIndex") int chunkIndex,
            @ForAll("validMetadata") DocumentMetadata metadata,
            @ForAll("validRelevanceScore") double relevanceScore,
            @ForAll @NotBlank String sourceDocument
    ) {
        // Arrange: Create a complete document chunk
        DocumentChunk chunk = DocumentChunk.builder()
                .id(chunkId)
                .documentId(documentId)
                .content(content)
                .chunkIndex(chunkIndex)
                .metadata(metadata)
                .build();
        
        // Act: Create a search result
        SearchResult result = SearchResult.builder()
                .chunk(chunk)
                .relevanceScore(relevanceScore)
                .sourceDocument(sourceDocument)
                .build();
        
        // Assert: Verify all three required components are present
        // 1. Original text content
        assertNotNull(result.getChunk(), "Search result must contain chunk");
        assertNotNull(result.getChunk().getContent(), "Chunk must contain content");
        assertFalse(result.getChunk().getContent().trim().isEmpty(), 
                "Content must not be empty");
        
        // 2. All source document metadata
        assertNotNull(result.getChunk().getMetadata(), "Chunk must contain metadata");
        assertNotNull(result.getChunk().getMetadata().getAuthor(), 
                "Metadata must contain author");
        assertNotNull(result.getChunk().getMetadata().getCreatedAt(), 
                "Metadata must contain creation timestamp");
        assertNotNull(result.getChunk().getMetadata().getModifiedAt(), 
                "Metadata must contain modification timestamp");
        assertNotNull(result.getChunk().getMetadata().getUrl(), 
                "Metadata must contain URL");
        assertNotNull(result.getChunk().getMetadata().getPermissions(), 
                "Metadata must contain permissions");
        
        // 3. Relevance score
        double score = result.getRelevanceScore();
        assertTrue(score >= 0.0 && score <= 1.0, 
                "Relevance score must be between 0.0 and 1.0");
    }

    @Property(tries = 100)
    void searchResultShouldRejectIncompleteData(
            @ForAll("validRelevanceScore") double relevanceScore,
            @ForAll @NotBlank String sourceDocument
    ) {
        // Act & Assert: Search result without chunk should fail validation
        SearchResult result = SearchResult.builder()
                .chunk(null)
                .relevanceScore(relevanceScore)
                .sourceDocument(sourceDocument)
                .build();
        
        assertThrows(IllegalArgumentException.class, result::validate,
                "Search result should reject null chunk");
    }

    // Providers for custom generators

    @Provide
    Arbitrary<Integer> validChunkIndex() {
        return Arbitraries.integers().greaterOrEqual(0);
    }

    @Provide
    Arbitrary<Double> validRelevanceScore() {
        return Arbitraries.doubles()
                .between(0.0, 1.0)
                .ofScale(2);
    }

    @Provide
    Arbitrary<DocumentMetadata> validMetadata() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50),
                validTimestamps(),
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(100),
                Arbitraries.of(List.of("read"), List.of("write"), List.of("read", "write"))
        ).as((author, timestamps, url, permissions) -> 
                DocumentMetadata.builder()
                        .author(author)
                        .createdAt(timestamps[0])
                        .modifiedAt(timestamps[1])
                        .url("https://example.com/" + url)
                        .permissions(permissions)
                        .build()
        );
    }

    @Provide
    Arbitrary<Instant[]> validTimestamps() {
        return Arbitraries.longs()
                .between(0, Instant.now().getEpochSecond())
                .flatMap(createdEpoch -> {
                    Instant created = Instant.ofEpochSecond(createdEpoch);
                    return Arbitraries.longs()
                            .between(createdEpoch, Instant.now().getEpochSecond())
                            .map(modifiedEpoch -> new Instant[]{
                                    created,
                                    Instant.ofEpochSecond(modifiedEpoch)
                            });
                });
    }
}
