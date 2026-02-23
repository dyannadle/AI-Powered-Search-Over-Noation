package com.aidocsearch.config;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.DoubleRange;
import net.jqwik.api.constraints.NotBlank;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based test for configuration file loading.
 * 
 * **Validates: Requirements 6.5**
 * 
 * Property 23: Configuration file loading
 * For any valid configuration file, the system should successfully load and 
 * apply all configuration parameters.
 */
class ConfigurationLoadingPropertyTest {

    @Property(tries = 100)
    void validPropertiesFileShouldLoadSuccessfully(
            @ForAll("asciiString") String openaiApiKey,
            @ForAll("documentSourceConfig") DocumentSourceConfig sourceConfig,
            @ForAll("asciiString") String chromaPersistDir,
            @ForAll("asciiString") String chromaCollectionName,
            @ForAll @IntRange(min = 100, max = 5000) int chunkSize,
            @ForAll @IntRange(min = 0, max = 500) int chunkOverlap,
            @ForAll @IntRange(min = 1, max = 10) int defaultResultLimit,
            @ForAll @IntRange(min = 10, max = 100) int maxResultLimit,
            @ForAll @DoubleRange(min = 0.0, max = 1.0) double similarityThreshold,
            @ForAll @IntRange(min = 100, max = 2000) int maxQueryLength,
            @ForAll boolean encryptCredentials,
            @ForAll boolean enforceHttps
    ) throws IOException {
        // Ensure chunk overlap is less than chunk size
        Assume.that(chunkOverlap < chunkSize);
        // Ensure default result limit doesn't exceed max
        Assume.that(defaultResultLimit <= maxResultLimit);
        
        // Create temporary directory for test
        Path tempDir = Files.createTempDirectory("config-test-");
        try {
            // Arrange: Create a valid properties file
            Path propsFile = tempDir.resolve("test.properties");
        StringBuilder content = new StringBuilder();
        content.append("openai.api.key=").append(openaiApiKey).append("\n");
        
        // Add document source configuration
        if (sourceConfig.hasNotion) {
            content.append("notion.api.key=").append(sourceConfig.notionApiKey).append("\n");
            content.append("notion.workspace.id=").append(sourceConfig.notionWorkspaceId).append("\n");
        }
        if (sourceConfig.hasGoogleDrive) {
            content.append("google.drive.credentials.path=").append(sourceConfig.googleCredentialsPath).append("\n");
        }
        
        content.append("chroma.persist.directory=").append(chromaPersistDir).append("\n");
        content.append("chroma.collection.name=").append(chromaCollectionName).append("\n");
        content.append("processing.chunk.size=").append(chunkSize).append("\n");
        content.append("processing.chunk.overlap=").append(chunkOverlap).append("\n");
        content.append("query.default.result.limit=").append(defaultResultLimit).append("\n");
        content.append("query.max.result.limit=").append(maxResultLimit).append("\n");
        content.append("query.similarity.threshold=").append(similarityThreshold).append("\n");
        content.append("query.max.query.length=").append(maxQueryLength).append("\n");
        content.append("security.encrypt.credentials=").append(encryptCredentials).append("\n");
        content.append("security.enforce.https=").append(enforceHttps).append("\n");
        
        Files.writeString(propsFile, content.toString());
        
        // Act: Load configuration from the properties file
        SearchConfig config = ConfigLoader.loadFromProperties(propsFile.toString());
        
        // Assert: Verify all configuration parameters are correctly loaded and applied
        assertNotNull(config, "Configuration should be loaded successfully");
        
        // Verify OpenAI configuration
        assertEquals(openaiApiKey, config.getOpenaiApiKey(), 
                "OpenAI API key should be loaded correctly");
        
        // Verify document source configuration
        if (sourceConfig.hasNotion) {
            assertEquals(sourceConfig.notionApiKey, config.getNotionApiKey(),
                    "Notion API key should be loaded correctly");
            assertEquals(sourceConfig.notionWorkspaceId, config.getNotionWorkspaceId(),
                    "Notion workspace ID should be loaded correctly");
        }
        if (sourceConfig.hasGoogleDrive) {
            assertEquals(sourceConfig.googleCredentialsPath, config.getGoogleCredentialsPath(),
                    "Google Drive credentials path should be loaded correctly");
        }
        
        // Verify ChromaDB configuration
        assertEquals(chromaPersistDir, config.getChromaPersistDirectory(),
                "ChromaDB persist directory should be loaded correctly");
        assertEquals(chromaCollectionName, config.getChromaCollectionName(),
                "ChromaDB collection name should be loaded correctly");
        
        // Verify document processing configuration
        assertEquals(chunkSize, config.getChunkSize(),
                "Chunk size should be loaded correctly");
        assertEquals(chunkOverlap, config.getChunkOverlap(),
                "Chunk overlap should be loaded correctly");
        
        // Verify query configuration
        assertEquals(defaultResultLimit, config.getDefaultResultLimit(),
                "Default result limit should be loaded correctly");
        assertEquals(maxResultLimit, config.getMaxResultLimit(),
                "Max result limit should be loaded correctly");
        assertEquals(similarityThreshold, config.getSimilarityThreshold(), 0.0001,
                "Similarity threshold should be loaded correctly");
        assertEquals(maxQueryLength, config.getMaxQueryLength(),
                "Max query length should be loaded correctly");
        
        // Verify security configuration
        assertEquals(encryptCredentials, config.isEncryptCredentials(),
                "Encrypt credentials flag should be loaded correctly");
        assertEquals(enforceHttps, config.isEnforceHttps(),
                "Enforce HTTPS flag should be loaded correctly");
        } finally {
            // Clean up temporary directory
            Files.deleteIfExists(tempDir.resolve("test.properties"));
            Files.deleteIfExists(tempDir);
        }
    }

    @Property(tries = 100)
    void validHoconFileShouldLoadSuccessfully(
            @ForAll("asciiString") String openaiApiKey,
            @ForAll("documentSourceConfig") DocumentSourceConfig sourceConfig,
            @ForAll @IntRange(min = 100, max = 5000) int chunkSize,
            @ForAll @IntRange(min = 0, max = 500) int chunkOverlap,
            @ForAll @DoubleRange(min = 0.0, max = 1.0) double similarityThreshold
    ) throws IOException {
        // Ensure chunk overlap is less than chunk size
        Assume.that(chunkOverlap < chunkSize);
        
        // Create temporary directory for test
        Path tempDir = Files.createTempDirectory("config-test-");
        try {
            // Arrange: Create a valid HOCON file
            Path hoconFile = tempDir.resolve("test.conf");
        StringBuilder content = new StringBuilder();
        content.append("ai-document-search {\n");
        content.append("  openai {\n");
        content.append("    api-key = \"").append(openaiApiKey).append("\"\n");
        content.append("  }\n");
        
        if (sourceConfig.hasNotion) {
            content.append("  notion {\n");
            content.append("    api-key = \"").append(sourceConfig.notionApiKey).append("\"\n");
            content.append("    workspace-id = \"").append(sourceConfig.notionWorkspaceId).append("\"\n");
            content.append("  }\n");
        }
        if (sourceConfig.hasGoogleDrive) {
            content.append("  google-drive {\n");
            content.append("    credentials-path = \"").append(sourceConfig.googleCredentialsPath).append("\"\n");
            content.append("  }\n");
        }
        
        content.append("  processing {\n");
        content.append("    chunk-size = ").append(chunkSize).append("\n");
        content.append("    chunk-overlap = ").append(chunkOverlap).append("\n");
        content.append("  }\n");
        content.append("  query {\n");
        content.append("    similarity-threshold = ").append(similarityThreshold).append("\n");
        content.append("  }\n");
        content.append("}\n");
        
        Files.writeString(hoconFile, content.toString());
        
        // Act: Load configuration from the HOCON file
        SearchConfig config = ConfigLoader.loadFromHocon(hoconFile.toString());
        
        // Assert: Verify all configuration parameters are correctly loaded and applied
        assertNotNull(config, "Configuration should be loaded successfully");
        assertEquals(openaiApiKey, config.getOpenaiApiKey(),
                "OpenAI API key should be loaded correctly from HOCON");
        assertEquals(chunkSize, config.getChunkSize(),
                "Chunk size should be loaded correctly from HOCON");
        assertEquals(chunkOverlap, config.getChunkOverlap(),
                "Chunk overlap should be loaded correctly from HOCON");
        assertEquals(similarityThreshold, config.getSimilarityThreshold(), 0.0001,
                "Similarity threshold should be loaded correctly from HOCON");
        } finally {
            // Clean up temporary directory
            Files.deleteIfExists(tempDir.resolve("test.conf"));
            Files.deleteIfExists(tempDir);
        }
    }

    @Property(tries = 100)
    void configurationWithDefaultValuesShouldLoadSuccessfully(
            @ForAll("asciiString") String openaiApiKey,
            @ForAll("documentSourceConfig") DocumentSourceConfig sourceConfig
    ) throws IOException {
        // Create temporary directory for test
        Path tempDir = Files.createTempDirectory("config-test-");
        try {
            // Arrange: Create a minimal properties file with only required fields
            Path propsFile = tempDir.resolve("test.properties");
        StringBuilder content = new StringBuilder();
        content.append("openai.api.key=").append(openaiApiKey).append("\n");
        
        if (sourceConfig.hasNotion) {
            content.append("notion.api.key=").append(sourceConfig.notionApiKey).append("\n");
            content.append("notion.workspace.id=").append(sourceConfig.notionWorkspaceId).append("\n");
        }
        if (sourceConfig.hasGoogleDrive) {
            content.append("google.drive.credentials.path=").append(sourceConfig.googleCredentialsPath).append("\n");
        }
        
        Files.writeString(propsFile, content.toString());
        
        // Act: Load configuration
        SearchConfig config = ConfigLoader.loadFromProperties(propsFile.toString());
        
        // Assert: Verify default values are applied for unspecified parameters
        assertNotNull(config, "Configuration should be loaded successfully");
        assertEquals(1000, config.getChunkSize(), "Default chunk size should be applied");
        assertEquals(200, config.getChunkOverlap(), "Default chunk overlap should be applied");
        assertEquals(5, config.getDefaultResultLimit(), "Default result limit should be applied");
        assertEquals(20, config.getMaxResultLimit(), "Default max result limit should be applied");
        assertEquals(0.7, config.getSimilarityThreshold(), 0.0001, "Default similarity threshold should be applied");
        assertEquals(500, config.getMaxQueryLength(), "Default max query length should be applied");
        assertTrue(config.isEncryptCredentials(), "Default encrypt credentials should be true");
        assertTrue(config.isEnforceHttps(), "Default enforce HTTPS should be true");
        } finally {
            // Clean up temporary directory
            Files.deleteIfExists(tempDir.resolve("test.properties"));
            Files.deleteIfExists(tempDir);
        }
    }

    @Property(tries = 50)
    void invalidConfigurationShouldFailToLoad(
            @ForAll("invalidConfigScenario") InvalidConfigScenario scenario
    ) throws IOException {
        // Create temporary directory for test
        Path tempDir = Files.createTempDirectory("config-test-");
        try {
            // Arrange: Create an invalid properties file
            Path propsFile = tempDir.resolve("test.properties");
            Files.writeString(propsFile, scenario.content);
            
            // Act & Assert: Loading should fail with ConfigurationException
            assertThrows(ConfigurationException.class, () -> {
                ConfigLoader.loadFromProperties(propsFile.toString());
            }, "Invalid configuration should fail to load: " + scenario.description);
        } finally {
            // Clean up temporary directory
            Files.deleteIfExists(tempDir.resolve("test.properties"));
            Files.deleteIfExists(tempDir);
        }
    }

    // Custom data class for document source configuration
    static class DocumentSourceConfig {
        final boolean hasNotion;
        final boolean hasGoogleDrive;
        final String notionApiKey;
        final String notionWorkspaceId;
        final String googleCredentialsPath;

        DocumentSourceConfig(boolean hasNotion, boolean hasGoogleDrive,
                           String notionApiKey, String notionWorkspaceId,
                           String googleCredentialsPath) {
            this.hasNotion = hasNotion;
            this.hasGoogleDrive = hasGoogleDrive;
            this.notionApiKey = notionApiKey;
            this.notionWorkspaceId = notionWorkspaceId;
            this.googleCredentialsPath = googleCredentialsPath;
        }
    }

    // Custom data class for invalid configuration scenarios
    static class InvalidConfigScenario {
        final String description;
        final String content;

        InvalidConfigScenario(String description, String content) {
            this.description = description;
            this.content = content;
        }
    }

    @Provide
    Arbitrary<String> asciiString() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .withChars('_', '-', '.', '/')
                .ofMinLength(1)
                .ofMaxLength(50);
    }

    @Provide
    Arbitrary<DocumentSourceConfig> documentSourceConfig() {
        Arbitrary<Boolean> hasNotion = Arbitraries.of(true, false);
        Arbitrary<Boolean> hasGoogleDrive = Arbitraries.of(true, false);
        Arbitrary<String> notionApiKey = Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .withChars('_', '-')
                .ofMinLength(10)
                .ofMaxLength(50);
        Arbitrary<String> notionWorkspaceId = Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .withChars('_', '-')
                .ofMinLength(10)
                .ofMaxLength(30);
        Arbitrary<String> googleCredentialsPath = Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .withChars('_', '-')
                .ofMinLength(5)
                .ofMaxLength(50)
                .map(s -> "/path/to/" + s + ".json");

        return Combinators.combine(hasNotion, hasGoogleDrive, notionApiKey, notionWorkspaceId, googleCredentialsPath)
                .as(DocumentSourceConfig::new)
                .filter(config -> config.hasNotion || config.hasGoogleDrive); // At least one source required
    }

    @Provide
    Arbitrary<InvalidConfigScenario> invalidConfigScenario() {
        return Arbitraries.of(
                // Missing OpenAI API key
                new InvalidConfigScenario(
                        "Missing OpenAI API key",
                        "notion.api.key=secret_test\nnotion.workspace.id=workspace-123\n"
                ),
                // Missing document source
                new InvalidConfigScenario(
                        "Missing document source",
                        "openai.api.key=sk-test-key\n"
                ),
                // Notion API key without workspace ID
                new InvalidConfigScenario(
                        "Notion API key without workspace ID",
                        "openai.api.key=sk-test-key\nnotion.api.key=secret_test\n"
                ),
                // Invalid chunk size (zero)
                new InvalidConfigScenario(
                        "Invalid chunk size (zero)",
                        "openai.api.key=sk-test-key\nnotion.api.key=secret_test\nnotion.workspace.id=workspace-123\nprocessing.chunk.size=0\n"
                ),
                // Invalid chunk size (negative)
                new InvalidConfigScenario(
                        "Invalid chunk size (negative)",
                        "openai.api.key=sk-test-key\nnotion.api.key=secret_test\nnotion.workspace.id=workspace-123\nprocessing.chunk.size=-100\n"
                ),
                // Chunk overlap >= chunk size
                new InvalidConfigScenario(
                        "Chunk overlap >= chunk size",
                        "openai.api.key=sk-test-key\nnotion.api.key=secret_test\nnotion.workspace.id=workspace-123\nprocessing.chunk.size=100\nprocessing.chunk.overlap=100\n"
                ),
                // Invalid similarity threshold (> 1.0)
                new InvalidConfigScenario(
                        "Invalid similarity threshold (> 1.0)",
                        "openai.api.key=sk-test-key\nnotion.api.key=secret_test\nnotion.workspace.id=workspace-123\nquery.similarity.threshold=1.5\n"
                ),
                // Invalid similarity threshold (< 0.0)
                new InvalidConfigScenario(
                        "Invalid similarity threshold (< 0.0)",
                        "openai.api.key=sk-test-key\nnotion.api.key=secret_test\nnotion.workspace.id=workspace-123\nquery.similarity.threshold=-0.1\n"
                ),
                // Default result limit > max result limit
                new InvalidConfigScenario(
                        "Default result limit > max result limit",
                        "openai.api.key=sk-test-key\nnotion.api.key=secret_test\nnotion.workspace.id=workspace-123\nquery.default.result.limit=30\nquery.max.result.limit=20\n"
                ),
                // Invalid retry backoff multiplier (<= 1.0)
                new InvalidConfigScenario(
                        "Invalid retry backoff multiplier (<= 1.0)",
                        "openai.api.key=sk-test-key\nnotion.api.key=secret_test\nnotion.workspace.id=workspace-123\nretry.backoff.multiplier=1.0\n"
                )
        );
    }
}
