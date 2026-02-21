package com.aidocsearch.models;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class DocumentMetadataTest {

    @Test
    void shouldCreateValidMetadata() {
        Instant now = Instant.now();
        Instant later = now.plusSeconds(3600);
        
        DocumentMetadata metadata = DocumentMetadata.builder()
                .author("John Doe")
                .createdAt(now)
                .modifiedAt(later)
                .url("https://example.com/doc")
                .permissions(Arrays.asList("read", "write"))
                .build();
        
        assertNotNull(metadata);
        assertEquals("John Doe", metadata.author);
        assertEquals(now, metadata.createdAt);
        assertEquals(later, metadata.modifiedAt);
        assertEquals("https://example.com/doc", metadata.url);
        assertEquals(2, metadata.permissions.size());
    }

    @Test
    void shouldRejectNullAuthor() {
        Instant now = Instant.now();
        
        assertThrows(IllegalArgumentException.class, () -> {
            DocumentMetadata metadata = DocumentMetadata.builder()
                    .author(null)
                    .createdAt(now)
                    .modifiedAt(now)
                    .url("https://example.com/doc")
                    .permissions(Collections.emptyList())
                    .build();
            metadata.validate();
        });
    }

    @Test
    void shouldRejectEmptyAuthor() {
        Instant now = Instant.now();
        
        assertThrows(IllegalArgumentException.class, () -> {
            DocumentMetadata metadata = DocumentMetadata.builder()
                    .author("   ")
                    .createdAt(now)
                    .modifiedAt(now)
                    .url("https://example.com/doc")
                    .permissions(Collections.emptyList())
                    .build();
            metadata.validate();
        });
    }

    @Test
    void shouldRejectModifiedBeforeCreated() {
        Instant now = Instant.now();
        Instant earlier = now.minusSeconds(3600);
        
        assertThrows(IllegalArgumentException.class, () -> {
            DocumentMetadata metadata = DocumentMetadata.builder()
                    .author("John Doe")
                    .createdAt(now)
                    .modifiedAt(earlier)
                    .url("https://example.com/doc")
                    .permissions(Collections.emptyList())
                    .build();
            metadata.validate();
        });
    }

    @Test
    void shouldRejectNullPermissions() {
        Instant now = Instant.now();
        
        assertThrows(IllegalArgumentException.class, () -> {
            DocumentMetadata metadata = DocumentMetadata.builder()
                    .author("John Doe")
                    .createdAt(now)
                    .modifiedAt(now)
                    .url("https://example.com/doc")
                    .permissions(null)
                    .build();
            metadata.validate();
        });
    }

    @Test
    void shouldSupportToBuilder() {
        Instant now = Instant.now();
        
        DocumentMetadata original = DocumentMetadata.builder()
                .author("John Doe")
                .createdAt(now)
                .modifiedAt(now)
                .url("https://example.com/doc")
                .permissions(Collections.emptyList())
                .build();
        
        DocumentMetadata modified = original.toBuilder()
                .author("Jane Smith")
                .build();
        
        assertEquals("Jane Smith", modified.author);
        assertEquals(now, modified.createdAt);
    }
}
