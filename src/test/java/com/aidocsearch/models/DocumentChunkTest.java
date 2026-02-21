package com.aidocsearch.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class DocumentChunkTest {

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
    }

    @Test
    void shouldCreateValidChunk() {
        DocumentChunk chunk = DocumentChunk.builder()
                .id("chunk-1")
                .documentId("doc-123")
                .content("This is chunk content")
                .chunkIndex(0)
                .metadata(validMetadata)
                .build();
        
        assertNotNull(chunk);
        assertEquals("chunk-1", chunk.id);
        assertEquals("doc-123", chunk.documentId);
        assertEquals("This is chunk content", chunk.content);
        assertEquals(0, chunk.chunkIndex);
        assertNotNull(chunk.metadata);
    }

    @Test
    void shouldRejectNullChunkId() {
        assertThrows(IllegalArgumentException.class, () -> {
            DocumentChunk chunk = DocumentChunk.builder()
                    .id(null)
                    .documentId("doc-123")
                    .content("Content")
                    .chunkIndex(0)
                    .metadata(validMetadata)
                    .build();
            chunk.validate();
        });
    }

    @Test
    void shouldRejectEmptyContent() {
        assertThrows(IllegalArgumentException.class, () -> {
            DocumentChunk chunk = DocumentChunk.builder()
                    .id("chunk-1")
                    .documentId("doc-123")
                    .content("   ")
                    .chunkIndex(0)
                    .metadata(validMetadata)
                    .build();
            chunk.validate();
        });
    }

    @Test
    void shouldRejectNegativeChunkIndex() {
        assertThrows(IllegalArgumentException.class, () -> {
            DocumentChunk chunk = DocumentChunk.builder()
                    .id("chunk-1")
                    .documentId("doc-123")
                    .content("Content")
                    .chunkIndex(-1)
                    .metadata(validMetadata)
                    .build();
            chunk.validate();
        });
    }

    @Test
    void shouldAcceptZeroChunkIndex() {
        DocumentChunk chunk = DocumentChunk.builder()
                .id("chunk-1")
                .documentId("doc-123")
                .content("Content")
                .chunkIndex(0)
                .metadata(validMetadata)
                .build();
        
        chunk.validate();
        assertEquals(0, chunk.chunkIndex);
    }

    @Test
    void shouldAcceptLargeChunkIndex() {
        DocumentChunk chunk = DocumentChunk.builder()
                .id("chunk-1")
                .documentId("doc-123")
                .content("Content")
                .chunkIndex(999)
                .metadata(validMetadata)
                .build();
        
        chunk.validate();
        assertEquals(999, chunk.chunkIndex);
    }

    @Test
    void shouldRejectNullMetadata() {
        assertThrows(IllegalArgumentException.class, () -> {
            DocumentChunk chunk = DocumentChunk.builder()
                    .id("chunk-1")
                    .documentId("doc-123")
                    .content("Content")
                    .chunkIndex(0)
                    .metadata(null)
                    .build();
            chunk.validate();
        });
    }
}
