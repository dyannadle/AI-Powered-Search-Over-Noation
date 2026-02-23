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
        assertEquals("chunk-1", chunk.getId());
        assertEquals("doc-123", chunk.getDocumentId());
        assertEquals("This is chunk content", chunk.getContent());
        assertEquals(0, chunk.getChunkIndex());
        assertNotNull(chunk.getMetadata());
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
        assertEquals(0, chunk.getChunkIndex());
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
        assertEquals(999, chunk.getChunkIndex());
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
