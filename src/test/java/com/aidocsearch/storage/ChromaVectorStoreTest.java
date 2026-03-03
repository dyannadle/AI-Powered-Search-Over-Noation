package com.aidocsearch.storage;

import com.aidocsearch.config.SearchConfig;
import com.aidocsearch.models.DocumentChunk;
import com.aidocsearch.models.DocumentMetadata;
import com.aidocsearch.models.SearchResult;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChromaVectorStoreTest {

        private MockWebServer mockWebServer;
        private ChromaVectorStore vectorStore;

        @BeforeEach
        void setUp() throws IOException {
                mockWebServer = new MockWebServer();
                mockWebServer.start();

                // Setup the initial get_or_create collection response
                mockWebServer.enqueue(new MockResponse()
                                .setBody("{\"id\": \"test-collection-id\", \"name\": \"test-collection\"}")
                                .setResponseCode(200));

                SearchConfig config = SearchConfig.builder()
                                .openaiApiKey("dummy-key")
                                .notionApiKey("dummy-key")
                                .notionWorkspaceId("dummy-id")
                                .chromaUrl(mockWebServer.url("/api/v1").toString())
                                .chromaCollectionName("test-collection")
                                .build();

                vectorStore = new ChromaVectorStore(config);
        }

        @AfterEach
        void tearDown() throws IOException {
                mockWebServer.shutdown();
        }

        @Test
        void search_parsesValidResponseCorrectly() {
                // Mock the search response
                String jsonResponse = """
                                {
                                  "ids": [["chunk-1", "chunk-2"]],
                                  "distances": [[0.1, 0.4]],
                                  "documents": [["content 1", "content 2"]],
                                  "metadatas": [[
                                    {"document_id": "doc-1", "chunk_index": "0", "author": "Alice"},
                                    {"document_id": "doc-2", "chunk_index": "1", "author": "Bob"}
                                  ]]
                                }
                                """;

                mockWebServer.enqueue(new MockResponse()
                                .setBody(jsonResponse)
                                .setResponseCode(200));

                List<Float> queryEmbed = List.of(0.1f, 0.2f, 0.3f);
                List<SearchResult> results = vectorStore.search(queryEmbed, 2);

                assertEquals(2, results.size());

                // Assert first result
                SearchResult first = results.get(0);
                assertEquals("doc-1", first.getSourceDocument());
                assertEquals("chunk-1", first.getChunk().getId());
                assertEquals("content 1", first.getChunk().getContent());
                assertTrue(first.getRelevanceScore() > 0.8f); // 1 / (1 + 0.1) = ~0.9

                // Assert second result
                SearchResult second = results.get(1);
                assertEquals("doc-2", second.getSourceDocument());
                assertEquals("chunk-2", second.getChunk().getId());
                assertEquals("content 2", second.getChunk().getContent());
                assertTrue(second.getRelevanceScore() > 0.7f); // 1 / (1 + 0.4) = ~0.71
        }

        @Test
        void search_handlesEmptyResponse() {
                String jsonResponse = """
                                {
                                  "ids": [],
                                  "distances": [],
                                  "documents": [],
                                  "metadatas": []
                                }
                                """;

                mockWebServer.enqueue(new MockResponse()
                                .setBody(jsonResponse)
                                .setResponseCode(200));

                List<Float> queryEmbed = List.of(0.1f);
                List<SearchResult> results = vectorStore.search(queryEmbed, 5);

                assertTrue(results.isEmpty());
        }

        @Test
        void storeChunks_sendsCorrectBatchRequest() throws InterruptedException {
                // Enqueue success response for upsert
                mockWebServer.enqueue(new MockResponse().setResponseCode(200));

                DocumentMetadata meta = DocumentMetadata.builder()
                                .author("Test")
                                .createdAt(Instant.now())
                                .modifiedAt(Instant.now())
                                .permissions(List.of("read"))
                                .build();

                List<DocumentChunk> chunks = List.of(
                                DocumentChunk.builder()
                                                .id("c1").documentId("d1").content("text1").chunkIndex(0).metadata(meta)
                                                .build(),
                                DocumentChunk.builder()
                                                .id("c2").documentId("d1").content("text2").chunkIndex(1).metadata(meta)
                                                .build());

                List<List<Float>> embeddings = List.of(
                                List.of(1.0f, 0.0f),
                                List.of(0.0f, 1.0f));

                vectorStore.storeChunks(chunks, embeddings);

                // Take the initialization request
                mockWebServer.takeRequest();

                // Take the upsert request and verify
                var request = mockWebServer.takeRequest();
                assertEquals("/api/v1/collections/test-collection-id/upsert", request.getPath());

                String body = request.getBody().readUtf8();
                assertTrue(body.contains("\"c1\""));
                assertTrue(body.contains("\"c2\""));
                assertTrue(body.contains("\"text1\""));
                assertTrue(body.contains("\"text2\""));
        }
}
