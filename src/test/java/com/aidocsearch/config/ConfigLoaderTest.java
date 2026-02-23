package com.aidocsearch.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ConfigLoader.
 */
class ConfigLoaderTest {

    @Test
    void testLoadFromPropertiesFile(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                openai.api.key=sk-test-key
                notion.api.key=secret_test
                notion.workspace.id=workspace-123
                processing.chunk.size=500
                query.similarity.threshold=0.8
                """;
        Files.writeString(propsFile, content);

        SearchConfig config = ConfigLoader.loadFromProperties(propsFile.toString());

        assertNotNull(config);
        assertEquals("sk-test-key", config.getOpenaiApiKey());
        assertEquals("secret_test", config.getNotionApiKey());
        assertEquals("workspace-123", config.getNotionWorkspaceId());
        assertEquals(500, config.getChunkSize());
        assertEquals(0.8, config.getSimilarityThreshold(), 0.001);
    }

    @Test
    void testLoadFromPropertiesWithDefaults(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                openai.api.key=sk-test-key
                google.drive.credentials.path=/path/to/creds.json
                """;
        Files.writeString(propsFile, content);

        SearchConfig config = ConfigLoader.loadFromProperties(propsFile.toString());

        assertNotNull(config);
        assertEquals("sk-test-key", config.getOpenaiApiKey());
        assertEquals("/path/to/creds.json", config.getGoogleCredentialsPath());
        // Check defaults
        assertEquals(1000, config.getChunkSize());
        assertEquals(0.7, config.getSimilarityThreshold(), 0.001);
    }

    @Test
    void testLoadFromHoconFile(@TempDir Path tempDir) throws IOException {
        Path hoconFile = tempDir.resolve("test.conf");
        String content = """
                ai-document-search {
                  openai {
                    api-key = "sk-test-key"
                    embedding-model = "text-embedding-3-small"
                  }
                  notion {
                    api-key = "secret_test"
                    workspace-id = "workspace-123"
                  }
                  processing {
                    chunk-size = 800
                  }
                }
                """;
        Files.writeString(hoconFile, content);

        SearchConfig config = ConfigLoader.loadFromHocon(hoconFile.toString());

        assertNotNull(config);
        assertEquals("sk-test-key", config.getOpenaiApiKey());
        assertEquals("text-embedding-3-small", config.getOpenaiEmbeddingModel());
        assertEquals("secret_test", config.getNotionApiKey());
        assertEquals("workspace-123", config.getNotionWorkspaceId());
        assertEquals(800, config.getChunkSize());
    }

    @Test
    void testInvalidPropertiesFile() {
        assertThrows(ConfigurationException.class, () -> {
            ConfigLoader.loadFromProperties("nonexistent.properties");
        });
    }

    @Test
    void testInvalidHoconFile() {
        assertThrows(ConfigurationException.class, () -> {
            ConfigLoader.loadFromHocon("nonexistent.conf");
        });
    }

    @Test
    void testPropertiesWithInvalidInteger(@TempDir Path tempDir) throws IOException {
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
    void testPropertiesWithInvalidDouble(@TempDir Path tempDir) throws IOException {
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
    void testPropertiesWithBooleanValues(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                openai.api.key=sk-test-key
                notion.api.key=secret_test
                notion.workspace.id=workspace-123
                security.encrypt.credentials=false
                security.enforce.https=false
                """;
        Files.writeString(propsFile, content);

        SearchConfig config = ConfigLoader.loadFromProperties(propsFile.toString());

        assertFalse(config.isEncryptCredentials());
        assertFalse(config.isEnforceHttps());
    }

    @Test
    void testPropertiesWithAllConfigurationOptions(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                notion.api.key=secret_test
                notion.workspace.id=workspace-123
                google.drive.credentials.path=/path/to/creds.json
                openai.api.key=sk-test-key
                openai.embedding.model=text-embedding-3-large
                chroma.persist.directory=/custom/path
                chroma.collection.name=custom-collection
                processing.chunk.size=1500
                processing.chunk.overlap=300
                query.default.result.limit=10
                query.max.result.limit=50
                query.similarity.threshold=0.75
                query.max.query.length=1000
                performance.max.concurrent.queries=20
                performance.embedding.batch.size=200
                performance.query.timeout.seconds=5
                retry.max.attempts=5
                retry.initial.delay.ms=2000
                retry.max.delay.ms=120000
                retry.backoff.multiplier=3.0
                security.encrypt.credentials=true
                security.enforce.https=true
                """;
        Files.writeString(propsFile, content);

        SearchConfig config = ConfigLoader.loadFromProperties(propsFile.toString());

        assertEquals("secret_test", config.getNotionApiKey());
        assertEquals("workspace-123", config.getNotionWorkspaceId());
        assertEquals("/path/to/creds.json", config.getGoogleCredentialsPath());
        assertEquals("sk-test-key", config.getOpenaiApiKey());
        assertEquals("text-embedding-3-large", config.getOpenaiEmbeddingModel());
        assertEquals("/custom/path", config.getChromaPersistDirectory());
        assertEquals("custom-collection", config.getChromaCollectionName());
        assertEquals(1500, config.getChunkSize());
        assertEquals(300, config.getChunkOverlap());
        assertEquals(10, config.getDefaultResultLimit());
        assertEquals(50, config.getMaxResultLimit());
        assertEquals(0.75, config.getSimilarityThreshold(), 0.001);
        assertEquals(1000, config.getMaxQueryLength());
        assertEquals(20, config.getMaxConcurrentQueries());
        assertEquals(200, config.getEmbeddingBatchSize());
        assertEquals(5, config.getQueryTimeoutSeconds());
        assertEquals(5, config.getRetryMaxAttempts());
        assertEquals(2000, config.getRetryInitialDelayMs());
        assertEquals(120000, config.getRetryMaxDelayMs());
        assertEquals(3.0, config.getRetryBackoffMultiplier(), 0.001);
        assertTrue(config.isEncryptCredentials());
        assertTrue(config.isEnforceHttps());
    }

    @Test
    void testPropertiesWithWhitespace(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                openai.api.key=  sk-test-key  
                notion.api.key=  secret_test  
                notion.workspace.id=  workspace-123  
                """;
        Files.writeString(propsFile, content);

        SearchConfig config = ConfigLoader.loadFromProperties(propsFile.toString());

        assertEquals("sk-test-key", config.getOpenaiApiKey());
        assertEquals("secret_test", config.getNotionApiKey());
        assertEquals("workspace-123", config.getNotionWorkspaceId());
    }

    @Test
    void testMissingRequiredFieldsInProperties(@TempDir Path tempDir) throws IOException {
        Path propsFile = tempDir.resolve("test.properties");
        String content = """
                notion.api.key=secret_test
                notion.workspace.id=workspace-123
                """;
        Files.writeString(propsFile, content);

        assertThrows(ConfigurationException.class, () -> {
            ConfigLoader.loadFromProperties(propsFile.toString());
        });
    }

    @Test
    void testLoadFromClasspath() {
        // This test assumes application.conf exists in src/test/resources
        // If the file doesn't exist, it should throw ConfigurationException
        assertThrows(ConfigurationException.class, () -> {
            ConfigLoader.loadConfig();
        });
    }
}
