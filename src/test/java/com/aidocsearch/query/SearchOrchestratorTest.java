package com.aidocsearch.query;

import com.aidocsearch.config.SearchConfig;
import com.aidocsearch.embedding.EmbeddingEngine;
import com.aidocsearch.models.Document;
import com.aidocsearch.models.DocumentChunk;
import com.aidocsearch.models.SearchResult;
import com.aidocsearch.processing.DocumentProcessor;
import com.aidocsearch.storage.ChromaVectorStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SearchOrchestratorTest {

    private SearchOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        // Setup logic if needed

        // Inject mocks via reflection or assume a package-private constructor for
        // testing
        // Since we don't have dependency injection framework, we will use a test
        // subclass
        // or just test the public behavior if we had setter injection.
        // For this test, we assume we modified Orchestrator to accept mocks or we use
        // reflection.
        // Actually, we can use reflection utility or just test the end-to-end
        // integration if we mock the config.
        // Since the orchestrator creates "new" instances internally based on config,
        // a pure unit test requires either a factory pattern or package-private
        // constructors.
    }

    @Test
    void search_emptyQuery_returnsEarly() {
        // To test real Orchestrator, we would need to mock its dependencies.
        // Given the current tightly-coupled constructor (new EmbeddingEngine()),
        // we'll run a minimal config-based test that verifies prompt rejection.

        SearchConfig realConfig = SearchConfig.builder()
                .openaiApiKey("dummy")
                .notionApiKey("dummy")
                .notionWorkspaceId("dummy")
                .build();

        SearchOrchestrator realOrchestrator = new SearchOrchestrator(realConfig);

        SearchOrchestrator.SearchResponse response = realOrchestrator.search("   ");

        assertTrue(response.getResults().isEmpty());
        assertEquals("Please provide a search query.", response.getSynthesizedAnswer());
        assertNull(response.getParsedQuery());
    }
}
