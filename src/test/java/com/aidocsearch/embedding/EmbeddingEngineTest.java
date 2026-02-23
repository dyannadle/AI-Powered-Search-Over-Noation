package com.aidocsearch.embedding;

import com.aidocsearch.config.SearchConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EmbeddingEngine.
 * Note: These tests require a valid OpenAI API key to run against the real API.
 * For CI/CD, consider mocking the OpenAI service or using integration test profiles.
 */
class EmbeddingEngineTest {
    
    private SearchConfig config;
    
    @BeforeEach
    void setUp() {
        // Create a test configuration
        config = SearchConfig.builder()
                .openaiApiKey("test-api-key")
                .notionApiKey("test-notion-key")
                .notionWorkspaceId("test-workspace")
                .openaiEmbeddingModel("text-embedding-ada-002")
                .retryMaxAttempts(3)
                .retryInitialDelayMs(100)
                .retryMaxDelayMs(1000)
                .retryBackoffMultiplier(2.0)
                .embeddingBatchSize(10)
                .build();
    }
    
    @Test
    void testConstructorWithValidConfig() {
        assertDoesNotThrow(() -> new EmbeddingEngine(config));
    }
    
    @Test
    void testConstructorWithNullConfig() {
        assertThrows(NullPointerException.class, () -> new EmbeddingEngine(null));
    }
    
    @Test
    void testConstructorWithMissingApiKey() {
        // The SearchConfig builder will throw ConfigurationException before we even get to EmbeddingEngine
        // So we test that the validation happens at the config level
        assertThrows(Exception.class, () -> {
            SearchConfig invalidConfig = SearchConfig.builder()
                    .openaiApiKey("")
                    .notionApiKey("test-notion-key")
                    .notionWorkspaceId("test-workspace")
                    .build();
            new EmbeddingEngine(invalidConfig);
        });
    }
    
    @Test
    void testGenerateEmbeddingWithNullText() {
        EmbeddingEngine engine = new EmbeddingEngine(config);
        assertThrows(IllegalArgumentException.class, () -> engine.generateEmbedding(null));
    }
    
    @Test
    void testGenerateEmbeddingWithEmptyText() {
        EmbeddingEngine engine = new EmbeddingEngine(config);
        assertThrows(IllegalArgumentException.class, () -> engine.generateEmbedding(""));
        assertThrows(IllegalArgumentException.class, () -> engine.generateEmbedding("   "));
    }
    
    @Test
    void testGenerateEmbeddingsBatchWithNullList() {
        EmbeddingEngine engine = new EmbeddingEngine(config);
        assertThrows(IllegalArgumentException.class, () -> engine.generateEmbeddingsBatch(null));
    }
    
    @Test
    void testGenerateEmbeddingsBatchWithEmptyList() {
        EmbeddingEngine engine = new EmbeddingEngine(config);
        assertThrows(IllegalArgumentException.class, () -> engine.generateEmbeddingsBatch(List.of()));
    }
    
    @Test
    void testGenerateEmbeddingsBatchWithNullElement() {
        EmbeddingEngine engine = new EmbeddingEngine(config);
        List<String> texts = Arrays.asList("valid text", null, "another valid text");
        assertThrows(IllegalArgumentException.class, () -> engine.generateEmbeddingsBatch(texts));
    }
    
    @Test
    void testGenerateEmbeddingsBatchWithEmptyElement() {
        EmbeddingEngine engine = new EmbeddingEngine(config);
        List<String> texts = Arrays.asList("valid text", "", "another valid text");
        assertThrows(IllegalArgumentException.class, () -> engine.generateEmbeddingsBatch(texts));
    }
    
    @Test
    void testCloseEngine() {
        EmbeddingEngine engine = new EmbeddingEngine(config);
        assertDoesNotThrow(() -> engine.close());
    }
    
    // Integration tests - disabled by default as they require a real API key
    // To run these tests, set a valid OpenAI API key and remove @Disabled
    
    @Test
    @Disabled("Requires valid OpenAI API key")
    void testGenerateEmbeddingIntegration() {
        // Set a real API key in the config
        SearchConfig realConfig = SearchConfig.builder()
                .openaiApiKey(System.getenv("OPENAI_API_KEY"))
                .notionApiKey("test-notion-key")
                .notionWorkspaceId("test-workspace")
                .build();
        
        EmbeddingEngine engine = new EmbeddingEngine(realConfig);
        
        String text = "This is a test document for embedding generation.";
        List<Float> embedding = engine.generateEmbedding(text);
        
        assertNotNull(embedding);
        assertFalse(embedding.isEmpty());
        // OpenAI ada-002 model produces 1536-dimensional embeddings
        assertEquals(1536, embedding.size());
        
        engine.close();
    }
    
    @Test
    @Disabled("Requires valid OpenAI API key")
    void testGenerateEmbeddingsBatchIntegration() {
        // Set a real API key in the config
        SearchConfig realConfig = SearchConfig.builder()
                .openaiApiKey(System.getenv("OPENAI_API_KEY"))
                .notionApiKey("test-notion-key")
                .notionWorkspaceId("test-workspace")
                .embeddingBatchSize(5)
                .build();
        
        EmbeddingEngine engine = new EmbeddingEngine(realConfig);
        
        List<String> texts = Arrays.asList(
                "First document about machine learning.",
                "Second document about natural language processing.",
                "Third document about computer vision.",
                "Fourth document about deep learning.",
                "Fifth document about neural networks.",
                "Sixth document about transformers.",
                "Seventh document about embeddings."
        );
        
        List<List<Float>> embeddings = engine.generateEmbeddingsBatch(texts);
        
        assertNotNull(embeddings);
        assertEquals(texts.size(), embeddings.size());
        
        for (List<Float> embedding : embeddings) {
            assertNotNull(embedding);
            assertEquals(1536, embedding.size());
        }
        
        engine.close();
    }
    
    @Test
    @Disabled("Requires valid OpenAI API key and may trigger rate limits")
    void testRetryLogicIntegration() {
        // This test would need to simulate failures, which is difficult with the real API
        // In a real scenario, you would use a mock or a test double
        SearchConfig retryConfig = SearchConfig.builder()
                .openaiApiKey(System.getenv("OPENAI_API_KEY"))
                .notionApiKey("test-notion-key")
                .notionWorkspaceId("test-workspace")
                .retryMaxAttempts(3)
                .retryInitialDelayMs(100)
                .retryMaxDelayMs(1000)
                .retryBackoffMultiplier(2.0)
                .build();
        
        EmbeddingEngine engine = new EmbeddingEngine(retryConfig);
        
        // This should succeed even if there are transient failures
        String text = "Test document for retry logic.";
        List<Float> embedding = engine.generateEmbedding(text);
        
        assertNotNull(embedding);
        assertEquals(1536, embedding.size());
        
        engine.close();
    }
}
