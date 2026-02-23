package com.aidocsearch.embedding;

import com.aidocsearch.config.SearchConfig;
import com.aidocsearch.models.DocumentChunk;
import com.aidocsearch.models.DocumentMetadata;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.NotBlank;
import net.jqwik.api.constraints.NotEmpty;
import net.jqwik.api.constraints.StringLength;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for embedding generation.
 * 
 * **Validates: Requirements 3.1**
 * 
 * Property 11: Embedding generation for all chunks
 * For any document chunk, an embedding vector should be successfully generated.
 * 
 * Note: These tests require a valid OpenAI API key set in the OPENAI_API_KEY environment variable.
 */
class EmbeddingGenerationPropertyTest {

    /**
     * Property 11: For any document chunk, an embedding vector should be successfully generated.
     * 
     * This test verifies that the embedding engine can generate embeddings for any valid
     * document chunk content, regardless of the specific text content.
     */
    @Property(tries = 100)
    void embeddingsShouldBeGeneratedForAnyDocumentChunk(
            @ForAll("validDocumentChunk") DocumentChunk chunk
    ) {
        // Arrange: Create embedding engine with real API key
        SearchConfig config = SearchConfig.builder()
                .openaiApiKey(System.getenv("OPENAI_API_KEY"))
                .notionApiKey("test-notion-key")
                .notionWorkspaceId("test-workspace")
                .build();
        
        EmbeddingEngine engine = new EmbeddingEngine(config);
        
        try {
            // Act: Generate embedding for the chunk content
            List<Float> embedding = engine.generateEmbedding(chunk.getContent());
            
            // Assert: Embedding should be successfully generated
            assertNotNull(embedding, "Embedding should not be null");
            assertFalse(embedding.isEmpty(), "Embedding should not be empty");
            
            // OpenAI's text-embedding-ada-002 model produces 1536-dimensional embeddings
            assertEquals(1536, embedding.size(), 
                    "Embedding should have 1536 dimensions for ada-002 model");
            
            // All embedding values should be finite numbers
            for (Float value : embedding) {
                assertNotNull(value, "Embedding value should not be null");
                assertTrue(Float.isFinite(value), 
                        "Embedding value should be finite (not NaN or Infinity)");
            }
            
        } finally {
            engine.close();
        }
    }

    /**
     * Property 11 variant: Batch embedding generation should work for multiple chunks.
     * 
     * This verifies that batch processing maintains the property that embeddings
     * are generated for all chunks.
     */
    @Property(tries = 50)
    void embeddingsShouldBeGeneratedForBatchOfChunks(
            @ForAll("validChunkContentList") List<String> chunkContents
    ) {
        // Arrange: Create embedding engine with real API key
        SearchConfig config = SearchConfig.builder()
                .openaiApiKey(System.getenv("OPENAI_API_KEY"))
                .notionApiKey("test-notion-key")
                .notionWorkspaceId("test-workspace")
                .embeddingBatchSize(10)
                .build();
        
        EmbeddingEngine engine = new EmbeddingEngine(config);
        
        try {
            // Act: Generate embeddings for all chunks
            List<List<Float>> embeddings = engine.generateEmbeddingsBatch(chunkContents);
            
            // Assert: One embedding per chunk
            assertNotNull(embeddings, "Embeddings list should not be null");
            assertEquals(chunkContents.size(), embeddings.size(), 
                    "Should generate one embedding per chunk");
            
            // Each embedding should be valid
            for (int i = 0; i < embeddings.size(); i++) {
                List<Float> embedding = embeddings.get(i);
                assertNotNull(embedding, 
                        "Embedding at index " + i + " should not be null");
                assertEquals(1536, embedding.size(), 
                        "Embedding at index " + i + " should have 1536 dimensions");
                
                // Verify all values are finite
                for (Float value : embedding) {
                    assertTrue(Float.isFinite(value), 
                            "All embedding values should be finite");
                }
            }
            
        } finally {
            engine.close();
        }
    }

    /**
     * Property 11 variant: Embeddings should be generated for chunks with various content types.
     * 
     * This tests that the property holds across different types of text content:
     * short text, long text, special characters, numbers, etc.
     */
    @Property(tries = 100)
    void embeddingsShouldBeGeneratedForVariousContentTypes(
            @ForAll("variousTextContent") String content
    ) {
        // Arrange: Create embedding engine with real API key
        SearchConfig config = SearchConfig.builder()
                .openaiApiKey(System.getenv("OPENAI_API_KEY"))
                .notionApiKey("test-notion-key")
                .notionWorkspaceId("test-workspace")
                .build();
        
        EmbeddingEngine engine = new EmbeddingEngine(config);
        
        try {
            // Act: Generate embedding
            List<Float> embedding = engine.generateEmbedding(content);
            
            // Assert: Embedding should be successfully generated regardless of content type
            assertNotNull(embedding, 
                    "Embedding should be generated for content: " + content.substring(0, Math.min(50, content.length())));
            assertEquals(1536, embedding.size(), 
                    "Embedding should have correct dimensions");
            
            // Verify embedding contains meaningful values (not all zeros)
            boolean hasNonZeroValue = embedding.stream()
                    .anyMatch(v -> Math.abs(v) > 0.0001f);
            assertTrue(hasNonZeroValue, 
                    "Embedding should contain non-zero values");
            
        } finally {
            engine.close();
        }
    }

    // ========== Arbitrary Providers ==========

    @Provide
    Arbitrary<DocumentChunk> validDocumentChunk() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20),  // id
                Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20),  // documentId
                Arbitraries.strings().ofMinLength(10).ofMaxLength(500),        // content
                Arbitraries.integers().between(0, 100),                         // chunkIndex
                validMetadata()                                                 // metadata
        ).as((id, docId, content, index, metadata) ->
                DocumentChunk.builder()
                        .id(id)
                        .documentId(docId)
                        .content(content)
                        .chunkIndex(index)
                        .metadata(metadata)
                        .build()
        );
    }

    @Provide
    Arbitrary<DocumentMetadata> validMetadata() {
        return Combinators.combine(
                Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(50),  // author
                validTimestamps(),                                              // timestamps
                Arbitraries.strings().alpha().ofMinLength(10).ofMaxLength(100), // url
                Arbitraries.of("read", "write", "admin").list().ofMinSize(1).ofMaxSize(3) // permissions
        ).as((author, timestamps, url, permissions) ->
                DocumentMetadata.builder()
                        .author(author)
                        .createdAt(timestamps[0])
                        .modifiedAt(timestamps[1])
                        .url("https://" + url + ".com")
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

    @Provide
    Arbitrary<List<String>> validChunkContentList() {
        return Arbitraries.strings()
                .ofMinLength(10)
                .ofMaxLength(200)
                .list()
                .ofMinSize(1)
                .ofMaxSize(15);
    }

    @Provide
    Arbitrary<String> variousTextContent() {
        return Arbitraries.oneOf(
                // Short text
                Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(50),
                // Long text
                Arbitraries.strings().ofMinLength(100).ofMaxLength(1000),
                // Text with numbers
                Arbitraries.strings().numeric().ofMinLength(10).ofMaxLength(100),
                // Text with special characters
                Arbitraries.strings().withCharRange('!', '~').ofMinLength(10).ofMaxLength(100),
                // Mixed content
                Arbitraries.strings().ofMinLength(20).ofMaxLength(200)
        );
    }
}
