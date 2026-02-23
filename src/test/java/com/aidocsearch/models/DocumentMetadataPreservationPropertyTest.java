package com.aidocsearch.models;

import net.jqwik.api.*;
import net.jqwik.api.constraints.NotBlank;
import net.jqwik.api.constraints.NotEmpty;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for metadata preservation.
 * 
 * **Validates: Requirements 2.2**
 * 
 * Property 6: Metadata preservation
 * For any document fetched from a source, the extracted document should contain 
 * all required metadata fields (title, author, created_at, modified_at).
 */
class DocumentMetadataPreservationPropertyTest {

    @Property(tries = 100)
    void documentShouldPreserveAllRequiredMetadataFields(
            @ForAll @NotBlank String id,
            @ForAll("validSource") String source,
            @ForAll @NotBlank String title,
            @ForAll String content,
            @ForAll @NotBlank String author,
            @ForAll("validTimestamps") Instant[] timestamps,
            @ForAll @NotBlank String url,
            @ForAll @NotEmpty List<@NotBlank String> permissions
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
        
        // Act: Create document with metadata
        Document document = Document.builder()
                .id(id)
                .source(source)
                .title(title)
                .content(content)
                .metadata(metadata)
                .build();
        
        // Assert: Verify all required metadata fields are preserved
        assertNotNull(document.getMetadata(), "Metadata should not be null");
        assertNotNull(document.getMetadata().getAuthor(), "Author should not be null");
        assertFalse(document.getMetadata().getAuthor().trim().isEmpty(), "Author should not be empty");
        assertNotNull(document.getMetadata().getCreatedAt(), "Created timestamp should not be null");
        assertNotNull(document.getMetadata().getModifiedAt(), "Modified timestamp should not be null");
        assertNotNull(document.getMetadata().getUrl(), "URL should not be null");
        assertFalse(document.getMetadata().getUrl().trim().isEmpty(), "URL should not be empty");
        assertNotNull(document.getMetadata().getPermissions(), "Permissions should not be null");
        
        // Verify the values are exactly what was provided
        assertEquals(author, document.getMetadata().getAuthor(), "Author should be preserved");
        assertEquals(createdAt, document.getMetadata().getCreatedAt(), "Created timestamp should be preserved");
        assertEquals(modifiedAt, document.getMetadata().getModifiedAt(), "Modified timestamp should be preserved");
        assertEquals(url, document.getMetadata().getUrl(), "URL should be preserved");
        assertEquals(permissions, document.getMetadata().getPermissions(), "Permissions should be preserved");
    }

    @Property(tries = 100)
    void documentShouldValidateMetadataPresence(
            @ForAll @NotBlank String id,
            @ForAll("validSource") String source,
            @ForAll @NotBlank String title,
            @ForAll String content
    ) {
        // Act & Assert: Document without metadata should fail validation
        Document document = Document.builder()
                .id(id)
                .source(source)
                .title(title)
                .content(content)
                .metadata(null)
                .build();
        
        assertThrows(IllegalArgumentException.class, document::validate,
                "Document should reject null metadata");
    }

    @Property(tries = 100)
    void metadataShouldRejectMissingRequiredFields(
            @ForAll("validTimestamps") Instant[] timestamps
    ) {
        Instant createdAt = timestamps[0];
        Instant modifiedAt = timestamps[1];
        
        // Test missing author
        assertThrows(IllegalArgumentException.class, () -> {
            DocumentMetadata metadata = DocumentMetadata.builder()
                    .author(null)
                    .createdAt(createdAt)
                    .modifiedAt(modifiedAt)
                    .url("https://example.com")
                    .permissions(List.of("read"))
                    .build();
            metadata.validate();
        }, "Metadata should reject null author");
        
        // Test missing createdAt
        assertThrows(IllegalArgumentException.class, () -> {
            DocumentMetadata metadata = DocumentMetadata.builder()
                    .author("John Doe")
                    .createdAt(null)
                    .modifiedAt(modifiedAt)
                    .url("https://example.com")
                    .permissions(List.of("read"))
                    .build();
            metadata.validate();
        }, "Metadata should reject null createdAt");
        
        // Test missing modifiedAt
        assertThrows(IllegalArgumentException.class, () -> {
            DocumentMetadata metadata = DocumentMetadata.builder()
                    .author("John Doe")
                    .createdAt(createdAt)
                    .modifiedAt(null)
                    .url("https://example.com")
                    .permissions(List.of("read"))
                    .build();
            metadata.validate();
        }, "Metadata should reject null modifiedAt");
    }

    @Provide
    Arbitrary<String> validSource() {
        return Arbitraries.of("notion", "google_drive");
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
