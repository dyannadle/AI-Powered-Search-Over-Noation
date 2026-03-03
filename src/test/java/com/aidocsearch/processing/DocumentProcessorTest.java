package com.aidocsearch.processing;

import com.aidocsearch.config.SearchConfig;
import com.aidocsearch.models.Document;
import com.aidocsearch.models.DocumentChunk;
import com.aidocsearch.models.DocumentMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocumentProcessorTest {

    private SearchConfig config;
    private DocumentProcessor processor;
    private DocumentMetadata metadata;

    @BeforeEach
    void setUp() {
        config = SearchConfig.builder()
                .openaiApiKey("dummy")
                .notionApiKey("dummy")
                .notionWorkspaceId("dummy")
                .chunkSize(100)
                .chunkOverlap(20)
                .build();
        processor = new DocumentProcessor(config);

        metadata = DocumentMetadata.builder()
                .author("Test Author")
                .createdAt(Instant.now())
                .modifiedAt(Instant.now())
                .url("http://test.com")
                .permissions(List.of("read"))
                .build();
    }

    @Test
    void process_emptyContent_returnsEmptyList() {
        Document doc = Document.builder()
                .id("doc-1")
                .source("test")
                .title("Empty Doc")
                .content("")
                .metadata(metadata)
                .build();

        List<DocumentChunk> chunks = processor.process(doc);
        assertTrue(chunks.isEmpty());
    }

    @Test
    void process_shortContent_returnsSingleChunk() {
        String content = "This is a short document. It fits in one chunk.";
        Document doc = Document.builder()
                .id("doc-1")
                .source("test")
                .title("Short Doc")
                .content(content)
                .metadata(metadata)
                .build();

        List<DocumentChunk> chunks = processor.process(doc);

        assertEquals(1, chunks.size());
        assertEquals(content, chunks.get(0).getContent());
        assertEquals(0, chunks.get(0).getChunkIndex());
        assertEquals("doc-1", chunks.get(0).getDocumentId());
    }

    @Test
    void process_longContent_splitsAtSentenceBoundaries() {
        // 173 characters total. Chunk size is 100. Overlap is 20.
        // Sentences:
        // 1. "This is the first sentence." (27 chars)
        // 2. "This is the second sentence, which makes it a bit longer." (57 chars)
        // 3. "And here is the third sentence!" (31 chars)
        // 4. "Finally, the fourth sentence concludes it." (42 chars)

        String content = "This is the first sentence. This is the second sentence, which makes it a bit longer. And here is the third sentence! Finally, the fourth sentence concludes it.";

        Document doc = Document.builder()
                .id("doc-1")
                .source("test")
                .title("Long Doc")
                .content(content)
                .metadata(metadata)
                .build();

        List<DocumentChunk> chunks = processor.process(doc);

        assertTrue(chunks.size() >= 2, "Should split into multiple chunks");

        // Ensure no chunk exceeds chunk size (with slight tolerance for word
        // boundaries)
        for (DocumentChunk chunk : chunks) {
            assertTrue(chunk.getContent().length() <= config.getChunkSize() + 20,
                    "Chunk too large: " + chunk.getContent().length());
        }

        // Re-assembling chunks without overlap should contain all original sentences
        String firstChunk = chunks.get(0).getContent();
        assertTrue(firstChunk.contains("first sentence"), "First chunk missing content");

        String lastChunk = chunks.get(chunks.size() - 1).getContent();
        assertTrue(lastChunk.contains("concludes it"), "Last chunk missing content");
    }

    @Test
    void process_veryLongSentence_splitsAtWordBoundaries() {
        // Chunk size is 100. This single sentence is ~150 chars.
        String content = "This is a single very very very very very very very very very very very very very very very very very very very very very very very very long sentence.";

        Document doc = Document.builder()
                .id("doc-1")
                .source("test")
                .title("Very Long Sentence")
                .content(content)
                .metadata(metadata)
                .build();

        List<DocumentChunk> chunks = processor.process(doc);

        assertTrue(chunks.size() >= 2);
        assertTrue(chunks.get(0).getContent().length() <= config.getChunkSize() + 10);
        assertTrue(chunks.get(1).getContent().length() <= config.getChunkSize() + 10);
    }
}
