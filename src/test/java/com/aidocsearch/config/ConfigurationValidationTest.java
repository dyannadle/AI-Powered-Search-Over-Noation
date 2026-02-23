package com.aidocsearch.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for configuration validation.
 * Tests invalid configuration handling and missing required fields.
 * 
 * Requirements: 6.5
 */
class ConfigurationValidationTest {

    // ========== Missing Required Fields Tests ==========

    @Test
    void testMissingOpenAIApiKeyInProperties(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                notion.api.key=secret_test
                notion.workspace.id=workspace-123
                """;
        Files.writeString(propsFile, content);

        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            ConfigLoader.loadFromProperties(propsFile.toString());
        });
        assertTrue(exception.getMessage().contains("OpenAI API key is required"));
    }

    @Test
    void testMissingOpenAIApiKeyInHocon(@TempDir Path tempDir) throws IOException {
        Path hoconFile = tempDir.resolve("test.conf");
        String content = """
                ai-document-search {
                  notion {
                    api-key = "secret_test"
                    workspace-id = "workspace-123"
                  }
                }
                """;
        Files.writeString(hoconFile, content);

        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            ConfigLoader.loadFromHocon(hoconFile.toString());
        });
        assertTrue(exception.getMessage().contains("OpenAI API key is required") ||
                   exception.getMessage().contains("Failed to load HOCON file"));
    }

    @Test
    void testMissingDocumentSourceInProperties(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                openai.api.key=sk-test-key
                """;
        Files.writeString(propsFile, content);

        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            ConfigLoader.loadFromProperties(propsFile.toString());
        });
        assertTrue(exception.getMessage().contains("At least one document source"));
    }

    @Test
    void testMissingNotionWorkspaceIdWhenApiKeyProvided(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                openai.api.key=sk-test-key
                notion.api.key=secret_test
                """;
        Files.writeString(propsFile, content);

        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            ConfigLoader.loadFromProperties(propsFile.toString());
        });
        assertTrue(exception.getMessage().contains("Notion workspace ID is required"));
    }

    @Test
    void testEmptyOpenAIApiKey(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                openai.api.key=   
                notion.api.key=secret_test
                notion.workspace.id=workspace-123
                """;
        Files.writeString(propsFile, content);

        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            ConfigLoader.loadFromProperties(propsFile.toString());
        });
        assertTrue(exception.getMessage().contains("OpenAI API key is required"));
    }

    @Test
    void testEmptyNotionApiKey(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                openai.api.key=sk-test-key
                notion.api.key=   
                notion.workspace.id=workspace-123
                """;
        Files.writeString(propsFile, content);

        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            ConfigLoader.loadFromProperties(propsFile.toString());
        });
        assertTrue(exception.getMessage().contains("At least one document source"));
    }

    @Test
    void testEmptyGoogleCredentialsPath(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                openai.api.key=sk-test-key
                google.drive.credentials.path=   
                """;
        Files.writeString(propsFile, content);

        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            ConfigLoader.loadFromProperties(propsFile.toString());
        });
        assertTrue(exception.getMessage().contains("At least one document source"));
    }

    // ========== Invalid Configuration Handling Tests ==========

    @Test
    void testInvalidChunkSizeZero(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                openai.api.key=sk-test-key
                notion.api.key=secret_test
                notion.workspace.id=workspace-123
                processing.chunk.size=0
                """;
        Files.writeString(propsFile, content);

        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            ConfigLoader.loadFromProperties(propsFile.toString());
        });
        assertTrue(exception.getMessage().contains("Chunk size must be positive"));
    }

    @Test
    void testInvalidChunkSizeNegative(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                openai.api.key=sk-test-key
                notion.api.key=secret_test
                notion.workspace.id=workspace-123
                processing.chunk.size=-500
                """;
        Files.writeString(propsFile, content);

        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            ConfigLoader.loadFromProperties(propsFile.toString());
        });
        assertTrue(exception.getMessage().contains("Chunk size must be positive"));
    }

    @Test
    void testInvalidChunkOverlapNegative(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                openai.api.key=sk-test-key
                notion.api.key=secret_test
                notion.workspace.id=workspace-123
                processing.chunk.overlap=-50
                """;
        Files.writeString(propsFile, content);

        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            ConfigLoader.loadFromProperties(propsFile.toString());
        });
        assertTrue(exception.getMessage().contains("Chunk overlap must be non-negative"));
    }

    @Test
    void testInvalidChunkOverlapEqualToSize(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                openai.api.key=sk-test-key
                notion.api.key=secret_test
                notion.workspace.id=workspace-123
                processing.chunk.size=500
                processing.chunk.overlap=500
                """;
        Files.writeString(propsFile, content);

        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            ConfigLoader.loadFromProperties(propsFile.toString());
        });
        assertTrue(exception.getMessage().contains("Chunk overlap must be less than chunk size"));
    }

    @Test
    void testInvalidChunkOverlapGreaterThanSize(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                openai.api.key=sk-test-key
                notion.api.key=secret_test
                notion.workspace.id=workspace-123
                processing.chunk.size=500
                processing.chunk.overlap=600
                """;
        Files.writeString(propsFile, content);

        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            ConfigLoader.loadFromProperties(propsFile.toString());
        });
        assertTrue(exception.getMessage().contains("Chunk overlap must be less than chunk size"));
    }

    @Test
    void testInvalidDefaultResultLimitZero(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                openai.api.key=sk-test-key
                notion.api.key=secret_test
                notion.workspace.id=workspace-123
                query.default.result.limit=0
                """;
        Files.writeString(propsFile, content);

        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            ConfigLoader.loadFromProperties(propsFile.toString());
        });
        assertTrue(exception.getMessage().contains("Default result limit must be positive"));
    }

    @Test
    void testInvalidDefaultResultLimitNegative(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                openai.api.key=sk-test-key
                notion.api.key=secret_test
                notion.workspace.id=workspace-123
                query.default.result.limit=-5
                """;
        Files.writeString(propsFile, content);

        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            ConfigLoader.loadFromProperties(propsFile.toString());
        });
        assertTrue(exception.getMessage().contains("Default result limit must be positive"));
    }

    @Test
    void testInvalidMaxResultLimitZero(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                openai.api.key=sk-test-key
                notion.api.key=secret_test
                notion.workspace.id=workspace-123
                query.max.result.limit=0
                """;
        Files.writeString(propsFile, content);

        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            ConfigLoader.loadFromProperties(propsFile.toString());
        });
        assertTrue(exception.getMessage().contains("Max result limit must be positive"));
    }

    @Test
    void testInvalidDefaultResultLimitExceedsMax(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                openai.api.key=sk-test-key
                notion.api.key=secret_test
                notion.workspace.id=workspace-123
                query.default.result.limit=30
                query.max.result.limit=20
                """;
        Files.writeString(propsFile, content);

        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            ConfigLoader.loadFromProperties(propsFile.toString());
        });
        assertTrue(exception.getMessage().contains("Default result limit must not exceed max result limit"));
    }

    @Test
    void testInvalidSimilarityThresholdAboveOne(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                openai.api.key=sk-test-key
                notion.api.key=secret_test
                notion.workspace.id=workspace-123
                query.similarity.threshold=1.5
                """;
        Files.writeString(propsFile, content);

        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            ConfigLoader.loadFromProperties(propsFile.toString());
        });
        assertTrue(exception.getMessage().contains("Similarity threshold must be between 0.0 and 1.0"));
    }

    @Test
    void testInvalidSimilarityThresholdBelowZero(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                openai.api.key=sk-test-key
                notion.api.key=secret_test
                notion.workspace.id=workspace-123
                query.similarity.threshold=-0.5
                """;
        Files.writeString(propsFile, content);

        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            ConfigLoader.loadFromProperties(propsFile.toString());
        });
        assertTrue(exception.getMessage().contains("Similarity threshold must be between 0.0 and 1.0"));
    }

    @Test
    void testInvalidMaxQueryLengthZero(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                openai.api.key=sk-test-key
                notion.api.key=secret_test
                notion.workspace.id=workspace-123
                query.max.query.length=0
                """;
        Files.writeString(propsFile, content);

        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            ConfigLoader.loadFromProperties(propsFile.toString());
        });
        assertTrue(exception.getMessage().contains("Max query length must be positive"));
    }

    @Test
    void testInvalidMaxQueryLengthNegative(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                openai.api.key=sk-test-key
                notion.api.key=secret_test
                notion.workspace.id=workspace-123
                query.max.query.length=-100
                """;
        Files.writeString(propsFile, content);

        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            ConfigLoader.loadFromProperties(propsFile.toString());
        });
        assertTrue(exception.getMessage().contains("Max query length must be positive"));
    }

    @Test
    void testInvalidMaxConcurrentQueriesZero(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                openai.api.key=sk-test-key
                notion.api.key=secret_test
                notion.workspace.id=workspace-123
                performance.max.concurrent.queries=0
                """;
        Files.writeString(propsFile, content);

        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            ConfigLoader.loadFromProperties(propsFile.toString());
        });
        assertTrue(exception.getMessage().contains("Max concurrent queries must be positive"));
    }

    @Test
    void testInvalidEmbeddingBatchSizeZero(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                openai.api.key=sk-test-key
                notion.api.key=secret_test
                notion.workspace.id=workspace-123
                performance.embedding.batch.size=0
                """;
        Files.writeString(propsFile, content);

        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            ConfigLoader.loadFromProperties(propsFile.toString());
        });
        assertTrue(exception.getMessage().contains("Embedding batch size must be positive"));
    }

    @Test
    void testInvalidQueryTimeoutZero(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                openai.api.key=sk-test-key
                notion.api.key=secret_test
                notion.workspace.id=workspace-123
                performance.query.timeout.seconds=0
                """;
        Files.writeString(propsFile, content);

        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            ConfigLoader.loadFromProperties(propsFile.toString());
        });
        assertTrue(exception.getMessage().contains("Query timeout must be positive"));
    }

    @Test
    void testInvalidRetryMaxAttemptsZero(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                openai.api.key=sk-test-key
                notion.api.key=secret_test
                notion.workspace.id=workspace-123
                retry.max.attempts=0
                """;
        Files.writeString(propsFile, content);

        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            ConfigLoader.loadFromProperties(propsFile.toString());
        });
        assertTrue(exception.getMessage().contains("Retry max attempts must be positive"));
    }

    @Test
    void testInvalidRetryInitialDelayZero(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                openai.api.key=sk-test-key
                notion.api.key=secret_test
                notion.workspace.id=workspace-123
                retry.initial.delay.ms=0
                """;
        Files.writeString(propsFile, content);

        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            ConfigLoader.loadFromProperties(propsFile.toString());
        });
        assertTrue(exception.getMessage().contains("Retry initial delay must be positive"));
    }

    @Test
    void testInvalidRetryMaxDelayZero(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                openai.api.key=sk-test-key
                notion.api.key=secret_test
                notion.workspace.id=workspace-123
                retry.max.delay.ms=0
                """;
        Files.writeString(propsFile, content);

        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            ConfigLoader.loadFromProperties(propsFile.toString());
        });
        assertTrue(exception.getMessage().contains("Retry max delay must be positive"));
    }

    @Test
    void testInvalidRetryInitialDelayExceedsMax(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                openai.api.key=sk-test-key
                notion.api.key=secret_test
                notion.workspace.id=workspace-123
                retry.initial.delay.ms=10000
                retry.max.delay.ms=5000
                """;
        Files.writeString(propsFile, content);

        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            ConfigLoader.loadFromProperties(propsFile.toString());
        });
        assertTrue(exception.getMessage().contains("Retry initial delay must not exceed max delay"));
    }

    @Test
    void testInvalidRetryBackoffMultiplierOne(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                openai.api.key=sk-test-key
                notion.api.key=secret_test
                notion.workspace.id=workspace-123
                retry.backoff.multiplier=1.0
                """;
        Files.writeString(propsFile, content);

        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            ConfigLoader.loadFromProperties(propsFile.toString());
        });
        assertTrue(exception.getMessage().contains("Retry backoff multiplier must be greater than 1.0"));
    }

    @Test
    void testInvalidRetryBackoffMultiplierBelowOne(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                openai.api.key=sk-test-key
                notion.api.key=secret_test
                notion.workspace.id=workspace-123
                retry.backoff.multiplier=0.5
                """;
        Files.writeString(propsFile, content);

        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            ConfigLoader.loadFromProperties(propsFile.toString());
        });
        assertTrue(exception.getMessage().contains("Retry backoff multiplier must be greater than 1.0"));
    }

    // ========== Invalid Data Type Tests ==========

    @Test
    void testInvalidIntegerFormatInChunkSize(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                openai.api.key=sk-test-key
                notion.api.key=secret_test
                notion.workspace.id=workspace-123
                processing.chunk.size=not-a-number
                """;
        Files.writeString(propsFile, content);

        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            ConfigLoader.loadFromProperties(propsFile.toString());
        });
        assertTrue(exception.getMessage().contains("Invalid integer value"));
    }

    @Test
    void testInvalidDoubleFormatInSimilarityThreshold(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                openai.api.key=sk-test-key
                notion.api.key=secret_test
                notion.workspace.id=workspace-123
                query.similarity.threshold=not-a-number
                """;
        Files.writeString(propsFile, content);

        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            ConfigLoader.loadFromProperties(propsFile.toString());
        });
        assertTrue(exception.getMessage().contains("Invalid double value"));
    }

    @Test
    void testInvalidLongFormatInRetryDelay(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                openai.api.key=sk-test-key
                notion.api.key=secret_test
                notion.workspace.id=workspace-123
                retry.initial.delay.ms=not-a-number
                """;
        Files.writeString(propsFile, content);

        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            ConfigLoader.loadFromProperties(propsFile.toString());
        });
        assertTrue(exception.getMessage().contains("Invalid long value"));
    }

    // ========== Edge Case Tests ==========

    @Test
    void testBoundaryValueSimilarityThresholdZero(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                openai.api.key=sk-test-key
                notion.api.key=secret_test
                notion.workspace.id=workspace-123
                query.similarity.threshold=0.0
                """;
        Files.writeString(propsFile, content);

        SearchConfig config = ConfigLoader.loadFromProperties(propsFile.toString());
        assertEquals(0.0, config.getSimilarityThreshold(), 0.0001);
    }

    @Test
    void testBoundaryValueSimilarityThresholdOne(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                openai.api.key=sk-test-key
                notion.api.key=secret_test
                notion.workspace.id=workspace-123
                query.similarity.threshold=1.0
                """;
        Files.writeString(propsFile, content);

        SearchConfig config = ConfigLoader.loadFromProperties(propsFile.toString());
        assertEquals(1.0, config.getSimilarityThreshold(), 0.0001);
    }

    @Test
    void testBoundaryValueChunkOverlapZero(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                openai.api.key=sk-test-key
                notion.api.key=secret_test
                notion.workspace.id=workspace-123
                processing.chunk.overlap=0
                """;
        Files.writeString(propsFile, content);

        SearchConfig config = ConfigLoader.loadFromProperties(propsFile.toString());
        assertEquals(0, config.getChunkOverlap());
    }

    @Test
    void testBoundaryValueChunkOverlapOneLessThanSize(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                openai.api.key=sk-test-key
                notion.api.key=secret_test
                notion.workspace.id=workspace-123
                processing.chunk.size=1000
                processing.chunk.overlap=999
                """;
        Files.writeString(propsFile, content);

        SearchConfig config = ConfigLoader.loadFromProperties(propsFile.toString());
        assertEquals(999, config.getChunkOverlap());
    }

    @Test
    void testBoundaryValueDefaultResultLimitEqualsMax(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                openai.api.key=sk-test-key
                notion.api.key=secret_test
                notion.workspace.id=workspace-123
                query.default.result.limit=20
                query.max.result.limit=20
                """;
        Files.writeString(propsFile, content);

        SearchConfig config = ConfigLoader.loadFromProperties(propsFile.toString());
        assertEquals(20, config.getDefaultResultLimit());
        assertEquals(20, config.getMaxResultLimit());
    }

    @Test
    void testBoundaryValueRetryBackoffMultiplierJustAboveOne(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                openai.api.key=sk-test-key
                notion.api.key=secret_test
                notion.workspace.id=workspace-123
                retry.backoff.multiplier=1.01
                """;
        Files.writeString(propsFile, content);

        SearchConfig config = ConfigLoader.loadFromProperties(propsFile.toString());
        assertEquals(1.01, config.getRetryBackoffMultiplier(), 0.0001);
    }

    @Test
    void testBoundaryValueRetryInitialDelayEqualsMax(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                openai.api.key=sk-test-key
                notion.api.key=secret_test
                notion.workspace.id=workspace-123
                retry.initial.delay.ms=5000
                retry.max.delay.ms=5000
                """;
        Files.writeString(propsFile, content);

        SearchConfig config = ConfigLoader.loadFromProperties(propsFile.toString());
        assertEquals(5000, config.getRetryInitialDelayMs());
        assertEquals(5000, config.getRetryMaxDelayMs());
    }

    // ========== File Not Found Tests ==========

    @Test
    void testNonExistentPropertiesFile() {
        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            ConfigLoader.loadFromProperties("nonexistent-file.properties");
        });
        assertTrue(exception.getMessage().contains("Properties file not found") ||
                   exception.getMessage().contains("Failed to load properties file"));
    }

    @Test
    void testNonExistentHoconFile() {
        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            ConfigLoader.loadFromHocon("nonexistent-file.conf");
        });
        assertTrue(exception.getMessage().contains("Failed to load HOCON file"));
    }

    // ========== Empty Configuration File Tests ==========

    @Test
    void testEmptyPropertiesFile(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        Files.writeString(propsFile, "");

        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            ConfigLoader.loadFromProperties(propsFile.toString());
        });
        assertTrue(exception.getMessage().contains("OpenAI API key is required"));
    }

    @Test
    void testEmptyHoconFile(@TempDir Path tempDir) throws IOException {
        Path hoconFile = tempDir.resolve("test.conf");
        Files.writeString(hoconFile, "");

        ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
            ConfigLoader.loadFromHocon(hoconFile.toString());
        });
        assertTrue(exception.getMessage().contains("OpenAI API key is required") ||
                   exception.getMessage().contains("Failed to load HOCON file"));
    }
}
