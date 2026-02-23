package com.aidocsearch.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SearchConfig validation and builder.
 */
class SearchConfigTest {

    @Test
    void testValidConfiguration() {
        SearchConfig config = SearchConfig.builder()
                .openaiApiKey("sk-test-key")
                .notionApiKey("secret_test")
                .notionWorkspaceId("workspace-123")
                .build();

        assertNotNull(config);
        assertEquals("sk-test-key", config.getOpenaiApiKey());
        assertEquals("secret_test", config.getNotionApiKey());
        assertEquals("workspace-123", config.getNotionWorkspaceId());
    }

    @Test
    void testMissingOpenAIApiKey() {
        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            SearchConfig.builder()
                    .notionApiKey("secret_test")
                    .notionWorkspaceId("workspace-123")
                    .build();
        });
        assertTrue(exception.getMessage().contains("OpenAI API key is required"));
    }

    @Test
    void testEmptyOpenAIApiKey() {
        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            SearchConfig.builder()
                    .openaiApiKey("   ")
                    .notionApiKey("secret_test")
                    .notionWorkspaceId("workspace-123")
                    .build();
        });
        assertTrue(exception.getMessage().contains("OpenAI API key is required"));
    }

    @Test
    void testMissingDocumentSource() {
        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            SearchConfig.builder()
                    .openaiApiKey("sk-test-key")
                    .build();
        });
        assertTrue(exception.getMessage().contains("At least one document source"));
    }

    @Test
    void testNotionConfigWithoutWorkspaceId() {
        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            SearchConfig.builder()
                    .openaiApiKey("sk-test-key")
                    .notionApiKey("secret_test")
                    .build();
        });
        assertTrue(exception.getMessage().contains("Notion workspace ID is required"));
    }

    @Test
    void testGoogleDriveOnlyConfiguration() {
        SearchConfig config = SearchConfig.builder()
                .openaiApiKey("sk-test-key")
                .googleCredentialsPath("/path/to/credentials.json")
                .build();

        assertNotNull(config);
        assertEquals("/path/to/credentials.json", config.getGoogleCredentialsPath());
    }

    @Test
    void testInvalidChunkSize() {
        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            SearchConfig.builder()
                    .openaiApiKey("sk-test-key")
                    .notionApiKey("secret_test")
                    .notionWorkspaceId("workspace-123")
                    .chunkSize(0)
                    .build();
        });
        assertTrue(exception.getMessage().contains("Chunk size must be positive"));
    }

    @Test
    void testNegativeChunkOverlap() {
        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            SearchConfig.builder()
                    .openaiApiKey("sk-test-key")
                    .notionApiKey("secret_test")
                    .notionWorkspaceId("workspace-123")
                    .chunkOverlap(-1)
                    .build();
        });
        assertTrue(exception.getMessage().contains("Chunk overlap must be non-negative"));
    }

    @Test
    void testChunkOverlapGreaterThanSize() {
        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            SearchConfig.builder()
                    .openaiApiKey("sk-test-key")
                    .notionApiKey("secret_test")
                    .notionWorkspaceId("workspace-123")
                    .chunkSize(100)
                    .chunkOverlap(100)
                    .build();
        });
        assertTrue(exception.getMessage().contains("Chunk overlap must be less than chunk size"));
    }

    @Test
    void testInvalidDefaultResultLimit() {
        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            SearchConfig.builder()
                    .openaiApiKey("sk-test-key")
                    .notionApiKey("secret_test")
                    .notionWorkspaceId("workspace-123")
                    .defaultResultLimit(0)
                    .build();
        });
        assertTrue(exception.getMessage().contains("Default result limit must be positive"));
    }

    @Test
    void testDefaultResultLimitExceedsMax() {
        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            SearchConfig.builder()
                    .openaiApiKey("sk-test-key")
                    .notionApiKey("secret_test")
                    .notionWorkspaceId("workspace-123")
                    .defaultResultLimit(25)
                    .maxResultLimit(20)
                    .build();
        });
        assertTrue(exception.getMessage().contains("Default result limit must not exceed max result limit"));
    }

    @Test
    void testInvalidSimilarityThreshold() {
        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            SearchConfig.builder()
                    .openaiApiKey("sk-test-key")
                    .notionApiKey("secret_test")
                    .notionWorkspaceId("workspace-123")
                    .similarityThreshold(1.5)
                    .build();
        });
        assertTrue(exception.getMessage().contains("Similarity threshold must be between 0.0 and 1.0"));
    }

    @Test
    void testNegativeSimilarityThreshold() {
        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            SearchConfig.builder()
                    .openaiApiKey("sk-test-key")
                    .notionApiKey("secret_test")
                    .notionWorkspaceId("workspace-123")
                    .similarityThreshold(-0.1)
                    .build();
        });
        assertTrue(exception.getMessage().contains("Similarity threshold must be between 0.0 and 1.0"));
    }

    @Test
    void testInvalidRetryBackoffMultiplier() {
        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            SearchConfig.builder()
                    .openaiApiKey("sk-test-key")
                    .notionApiKey("secret_test")
                    .notionWorkspaceId("workspace-123")
                    .retryBackoffMultiplier(1.0)
                    .build();
        });
        assertTrue(exception.getMessage().contains("Retry backoff multiplier must be greater than 1.0"));
    }

    @Test
    void testRetryInitialDelayExceedsMax() {
        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            SearchConfig.builder()
                    .openaiApiKey("sk-test-key")
                    .notionApiKey("secret_test")
                    .notionWorkspaceId("workspace-123")
                    .retryInitialDelayMs(5000)
                    .retryMaxDelayMs(1000)
                    .build();
        });
        assertTrue(exception.getMessage().contains("Retry initial delay must not exceed max delay"));
    }

    @Test
    void testDefaultValues() {
        SearchConfig config = SearchConfig.builder()
                .openaiApiKey("sk-test-key")
                .notionApiKey("secret_test")
                .notionWorkspaceId("workspace-123")
                .build();

        assertEquals("text-embedding-ada-002", config.getOpenaiEmbeddingModel());
        assertEquals("./data/chroma", config.getChromaPersistDirectory());
        assertEquals("document-embeddings", config.getChromaCollectionName());
        assertEquals(1000, config.getChunkSize());
        assertEquals(200, config.getChunkOverlap());
        assertEquals(5, config.getDefaultResultLimit());
        assertEquals(20, config.getMaxResultLimit());
        assertEquals(0.7, config.getSimilarityThreshold(), 0.001);
        assertEquals(500, config.getMaxQueryLength());
        assertEquals(10, config.getMaxConcurrentQueries());
        assertEquals(100, config.getEmbeddingBatchSize());
        assertEquals(3, config.getQueryTimeoutSeconds());
        assertEquals(3, config.getRetryMaxAttempts());
        assertEquals(1000, config.getRetryInitialDelayMs());
        assertEquals(60000, config.getRetryMaxDelayMs());
        assertEquals(2.0, config.getRetryBackoffMultiplier(), 0.001);
        assertTrue(config.isEncryptCredentials());
        assertTrue(config.isEnforceHttps());
    }

    @Test
    void testToStringMasksSensitiveData() {
        SearchConfig config = SearchConfig.builder()
                .openaiApiKey("sk-test-key")
                .notionApiKey("secret_test")
                .notionWorkspaceId("workspace-123")
                .build();

        String toString = config.toString();
        assertFalse(toString.contains("sk-test-key"));
        assertFalse(toString.contains("secret_test"));
        assertTrue(toString.contains("***"));
    }

    @Test
    void testEqualsAndHashCode() {
        SearchConfig config1 = SearchConfig.builder()
                .openaiApiKey("sk-test-key")
                .notionApiKey("secret_test")
                .notionWorkspaceId("workspace-123")
                .build();

        SearchConfig config2 = SearchConfig.builder()
                .openaiApiKey("sk-test-key")
                .notionApiKey("secret_test")
                .notionWorkspaceId("workspace-123")
                .build();

        assertEquals(config1, config2);
        assertEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    void testNotEquals() {
        SearchConfig config1 = SearchConfig.builder()
                .openaiApiKey("sk-test-key")
                .notionApiKey("secret_test")
                .notionWorkspaceId("workspace-123")
                .build();

        SearchConfig config2 = SearchConfig.builder()
                .openaiApiKey("sk-different-key")
                .notionApiKey("secret_test")
                .notionWorkspaceId("workspace-123")
                .build();

        assertNotEquals(config1, config2);
    }
}
