package com.aidocsearch.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class DocumentTest {

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
    void shouldCreateValidDocument() {
        Document doc = Document.builder()
                .id("doc-123")
                .source("notion")
                .title("Test Document")
                .content("This is test content")
                .metadata(validMetadata)
                .build();
        
        assertNotNull(doc);
        assertEquals("doc-123", doc.id);
        assertEquals("notion", doc.source);
        assertEquals("Test Document", doc.title);
        assertEquals("This is test content", doc.content);
        assertNotNull(doc.metadata);
    }

    @Test
    void shouldAcceptNotionSource() {
        Document doc = Document.builder()
                .id("doc-123")
                .source("notion")
                .title("Test")
                .content("Content")
                .metadata(validMetadata)
                .build();
        
        doc.validate();
        assertEquals("notion", doc.source);
    }

    @Test
    void shouldAcceptGoogleDriveSource() {
        Document doc = Document.builder()
                .id("doc-123")
                .source("google_drive")
                .title("Test")
                .content("Content")
                .metadata(validMetadata)
                .build();
        
        doc.validate();
        assertEquals("google_drive", doc.source);
    }

    @Test
    void shouldRejectInvalidSource() {
        assertThrows(IllegalArgumentException.class, () -> {
            Document doc = Document.builder()
                    .id("doc-123")
                    .source("dropbox")
                    .title("Test")
                    .content("Content")
                    .metadata(validMetadata)
                    .build();
            doc.validate();
        });
    }

    @Test
    void shouldRejectNullId() {
        assertThrows(IllegalArgumentException.class, () -> {
            Document doc = Document.builder()
                    .id(null)
                    .source("notion")
                    .title("Test")
                    .content("Content")
                    .metadata(validMetadata)
                    .build();
            doc.validate();
        });
    }

    @Test
    void shouldRejectEmptyTitle() {
        assertThrows(IllegalArgumentException.class, () -> {
            Document doc = Document.builder()
                    .id("doc-123")
                    .source("notion")
                    .title("   ")
                    .content("Content")
                    .metadata(validMetadata)
                    .build();
            doc.validate();
        });
    }

    @Test
    void shouldAcceptEmptyContent() {
        Document doc = Document.builder()
                .id("doc-123")
                .source("notion")
                .title("Test")
                .content("")
                .metadata(validMetadata)
                .build();
        
        doc.validate();
        assertEquals("", doc.content);
    }

    @Test
    void shouldRejectNullMetadata() {
        assertThrows(IllegalArgumentException.class, () -> {
            Document doc = Document.builder()
                    .id("doc-123")
                    .source("notion")
                    .title("Test")
                    .content("Content")
                    .metadata(null)
                    .build();
            doc.validate();
        });
    }
}
