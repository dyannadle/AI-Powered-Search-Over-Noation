package com.aidocsearch.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads configuration from properties or HOCON files with environment variable override support.
 * 
 * Loading priority (highest to lowest):
 * 1. Environment variables
 * 2. System properties
 * 3. application.properties file
 * 4. application.conf file (HOCON)
 * 5. Default values in SearchConfig.Builder
 */
public class ConfigLoader {
    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);
    
    private static final String PROPERTIES_FILE = "application.properties";
    private static final String HOCON_FILE = "application.conf";
    
    /**
     * Load configuration from default locations (classpath or current directory).
     * 
     * @return SearchConfig instance with loaded configuration
     * @throws ConfigurationException if configuration cannot be loaded or is invalid
     */
    public static SearchConfig loadConfig() {
        // Try to load from properties file first
        SearchConfig config = tryLoadFromProperties();
        if (config != null) {
            logger.info("Configuration loaded from properties file");
            return config;
        }
        
        // Fall back to HOCON file
        config = tryLoadFromHocon();
        if (config != null) {
            logger.info("Configuration loaded from HOCON file");
            return config;
        }
        
        throw new ConfigurationException("No configuration file found. Please provide " + 
                PROPERTIES_FILE + " or " + HOCON_FILE);
    }
    
    /**
     * Load configuration from a specific properties file.
     * 
     * @param propertiesFile path to properties file
     * @return SearchConfig instance with loaded configuration
     * @throws ConfigurationException if configuration cannot be loaded or is invalid
     */
    public static SearchConfig loadFromProperties(String propertiesFile) {
        try {
            Properties props = new Properties();
            
            // Try to load from file system first
            File file = new File(propertiesFile);
            if (file.exists()) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    props.load(fis);
                }
            } else {
                // Try to load from classpath
                try (InputStream is = ConfigLoader.class.getClassLoader().getResourceAsStream(propertiesFile)) {
                    if (is == null) {
                        throw new ConfigurationException("Properties file not found: " + propertiesFile);
                    }
                    props.load(is);
                }
            }
            
            return buildConfigFromProperties(props);
        } catch (IOException e) {
            throw new ConfigurationException("Failed to load properties file: " + propertiesFile, e);
        }
    }
    
    /**
     * Load configuration from a specific HOCON file.
     * 
     * @param hoconFile path to HOCON file
     * @return SearchConfig instance with loaded configuration
     * @throws ConfigurationException if configuration cannot be loaded or is invalid
     */
    public static SearchConfig loadFromHocon(String hoconFile) {
        try {
            File file = new File(hoconFile);
            Config config;
            
            if (file.exists()) {
                config = ConfigFactory.parseFile(file);
            } else {
                config = ConfigFactory.parseResources(hoconFile);
            }
            
            // Resolve with system properties and environment variables
            config = config.resolve();
            
            return buildConfigFromHocon(config);
        } catch (Exception e) {
            throw new ConfigurationException("Failed to load HOCON file: " + hoconFile, e);
        }
    }
    
    private static SearchConfig tryLoadFromProperties() {
        try {
            // Check file system first
            File file = new File(PROPERTIES_FILE);
            if (file.exists()) {
                return loadFromProperties(PROPERTIES_FILE);
            }
            
            // Check classpath
            try (InputStream is = ConfigLoader.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
                if (is != null) {
                    return loadFromProperties(PROPERTIES_FILE);
                }
            }
        } catch (Exception e) {
            logger.debug("Could not load from properties file", e);
        }
        return null;
    }
    
    private static SearchConfig tryLoadFromHocon() {
        try {
            // Check file system first
            File file = new File(HOCON_FILE);
            if (file.exists()) {
                return loadFromHocon(HOCON_FILE);
            }
            
            // Check classpath
            Config config = ConfigFactory.load(HOCON_FILE.replace(".conf", ""));
            if (config.hasPath("ai-document-search")) {
                return buildConfigFromHocon(config);
            }
        } catch (Exception e) {
            logger.debug("Could not load from HOCON file", e);
        }
        return null;
    }
    
    private static SearchConfig buildConfigFromProperties(Properties props) {
        SearchConfig.Builder builder = SearchConfig.builder();
        
        // Notion configuration
        setIfPresent(props, "notion.api.key", value -> builder.notionApiKey(value));
        setIfPresent(props, "notion.workspace.id", value -> builder.notionWorkspaceId(value));
        
        // Google Drive configuration
        setIfPresent(props, "google.drive.credentials.path", value -> builder.googleCredentialsPath(value));
        
        // OpenAI configuration
        setIfPresent(props, "openai.api.key", value -> builder.openaiApiKey(value));
        setIfPresent(props, "openai.embedding.model", value -> builder.openaiEmbeddingModel(value));
        
        // ChromaDB configuration
        setIfPresent(props, "chroma.persist.directory", value -> builder.chromaPersistDirectory(value));
        setIfPresent(props, "chroma.collection.name", value -> builder.chromaCollectionName(value));
        
        // Document processing configuration
        setIfPresentInt(props, "processing.chunk.size", value -> builder.chunkSize(value));
        setIfPresentInt(props, "processing.chunk.overlap", value -> builder.chunkOverlap(value));
        
        // Query configuration
        setIfPresentInt(props, "query.default.result.limit", value -> builder.defaultResultLimit(value));
        setIfPresentInt(props, "query.max.result.limit", value -> builder.maxResultLimit(value));
        setIfPresentDouble(props, "query.similarity.threshold", value -> builder.similarityThreshold(value));
        setIfPresentInt(props, "query.max.query.length", value -> builder.maxQueryLength(value));
        
        // Performance configuration
        setIfPresentInt(props, "performance.max.concurrent.queries", value -> builder.maxConcurrentQueries(value));
        setIfPresentInt(props, "performance.embedding.batch.size", value -> builder.embeddingBatchSize(value));
        setIfPresentInt(props, "performance.query.timeout.seconds", value -> builder.queryTimeoutSeconds(value));
        
        // Retry configuration
        setIfPresentInt(props, "retry.max.attempts", value -> builder.retryMaxAttempts(value));
        setIfPresentLong(props, "retry.initial.delay.ms", value -> builder.retryInitialDelayMs(value));
        setIfPresentLong(props, "retry.max.delay.ms", value -> builder.retryMaxDelayMs(value));
        setIfPresentDouble(props, "retry.backoff.multiplier", value -> builder.retryBackoffMultiplier(value));
        
        // Security configuration
        setIfPresentBoolean(props, "security.encrypt.credentials", value -> builder.encryptCredentials(value));
        setIfPresentBoolean(props, "security.enforce.https", value -> builder.enforceHttps(value));
        
        // Apply environment variable overrides
        applyEnvironmentOverrides(builder);
        
        return builder.build();
    }
    
    private static SearchConfig buildConfigFromHocon(Config config) {
        SearchConfig.Builder builder = SearchConfig.builder();
        
        String prefix = "ai-document-search";
        
        // Notion configuration
        if (config.hasPath(prefix + ".notion.api-key")) {
            builder.notionApiKey(config.getString(prefix + ".notion.api-key"));
        }
        if (config.hasPath(prefix + ".notion.workspace-id")) {
            builder.notionWorkspaceId(config.getString(prefix + ".notion.workspace-id"));
        }
        
        // Google Drive configuration
        if (config.hasPath(prefix + ".google-drive.credentials-path")) {
            builder.googleCredentialsPath(config.getString(prefix + ".google-drive.credentials-path"));
        }
        
        // OpenAI configuration
        if (config.hasPath(prefix + ".openai.api-key")) {
            builder.openaiApiKey(config.getString(prefix + ".openai.api-key"));
        }
        if (config.hasPath(prefix + ".openai.embedding-model")) {
            builder.openaiEmbeddingModel(config.getString(prefix + ".openai.embedding-model"));
        }
        
        // ChromaDB configuration
        if (config.hasPath(prefix + ".chroma.persist-directory")) {
            builder.chromaPersistDirectory(config.getString(prefix + ".chroma.persist-directory"));
        }
        if (config.hasPath(prefix + ".chroma.collection-name")) {
            builder.chromaCollectionName(config.getString(prefix + ".chroma.collection-name"));
        }
        
        // Document processing configuration
        if (config.hasPath(prefix + ".processing.chunk-size")) {
            builder.chunkSize(config.getInt(prefix + ".processing.chunk-size"));
        }
        if (config.hasPath(prefix + ".processing.chunk-overlap")) {
            builder.chunkOverlap(config.getInt(prefix + ".processing.chunk-overlap"));
        }
        
        // Query configuration
        if (config.hasPath(prefix + ".query.default-result-limit")) {
            builder.defaultResultLimit(config.getInt(prefix + ".query.default-result-limit"));
        }
        if (config.hasPath(prefix + ".query.max-result-limit")) {
            builder.maxResultLimit(config.getInt(prefix + ".query.max-result-limit"));
        }
        if (config.hasPath(prefix + ".query.similarity-threshold")) {
            builder.similarityThreshold(config.getDouble(prefix + ".query.similarity-threshold"));
        }
        if (config.hasPath(prefix + ".query.max-query-length")) {
            builder.maxQueryLength(config.getInt(prefix + ".query.max-query-length"));
        }
        
        // Performance configuration
        if (config.hasPath(prefix + ".performance.max-concurrent-queries")) {
            builder.maxConcurrentQueries(config.getInt(prefix + ".performance.max-concurrent-queries"));
        }
        if (config.hasPath(prefix + ".performance.embedding-batch-size")) {
            builder.embeddingBatchSize(config.getInt(prefix + ".performance.embedding-batch-size"));
        }
        if (config.hasPath(prefix + ".performance.query-timeout-seconds")) {
            builder.queryTimeoutSeconds(config.getInt(prefix + ".performance.query-timeout-seconds"));
        }
        
        // Retry configuration
        if (config.hasPath(prefix + ".retry.max-attempts")) {
            builder.retryMaxAttempts(config.getInt(prefix + ".retry.max-attempts"));
        }
        if (config.hasPath(prefix + ".retry.initial-delay-ms")) {
            builder.retryInitialDelayMs(config.getLong(prefix + ".retry.initial-delay-ms"));
        }
        if (config.hasPath(prefix + ".retry.max-delay-ms")) {
            builder.retryMaxDelayMs(config.getLong(prefix + ".retry.max-delay-ms"));
        }
        if (config.hasPath(prefix + ".retry.backoff-multiplier")) {
            builder.retryBackoffMultiplier(config.getDouble(prefix + ".retry.backoff-multiplier"));
        }
        
        // Security configuration
        if (config.hasPath(prefix + ".security.encrypt-credentials")) {
            builder.encryptCredentials(config.getBoolean(prefix + ".security.encrypt-credentials"));
        }
        if (config.hasPath(prefix + ".security.enforce-https")) {
            builder.enforceHttps(config.getBoolean(prefix + ".security.enforce-https"));
        }
        
        // Apply environment variable overrides
        applyEnvironmentOverrides(builder);
        
        return builder.build();
    }
    
    private static void applyEnvironmentOverrides(SearchConfig.Builder builder) {
        // Environment variables take precedence over file configuration
        String notionApiKey = System.getenv("NOTION_API_KEY");
        if (notionApiKey != null && !notionApiKey.isEmpty()) {
            builder.notionApiKey(notionApiKey);
        }
        
        String notionWorkspaceId = System.getenv("NOTION_WORKSPACE_ID");
        if (notionWorkspaceId != null && !notionWorkspaceId.isEmpty()) {
            builder.notionWorkspaceId(notionWorkspaceId);
        }
        
        String googleCredentialsPath = System.getenv("GOOGLE_CREDENTIALS_PATH");
        if (googleCredentialsPath != null && !googleCredentialsPath.isEmpty()) {
            builder.googleCredentialsPath(googleCredentialsPath);
        }
        
        String openaiApiKey = System.getenv("OPENAI_API_KEY");
        if (openaiApiKey != null && !openaiApiKey.isEmpty()) {
            builder.openaiApiKey(openaiApiKey);
        }
    }
    
    private static void setIfPresent(Properties props, String key, java.util.function.Consumer<String> setter) {
        String value = props.getProperty(key);
        if (value != null && !value.trim().isEmpty()) {
            setter.accept(value.trim());
        }
    }
    
    private static void setIfPresentInt(Properties props, String key, java.util.function.Consumer<Integer> setter) {
        String value = props.getProperty(key);
        if (value != null && !value.trim().isEmpty()) {
            try {
                setter.accept(Integer.parseInt(value.trim()));
            } catch (NumberFormatException e) {
                throw new ConfigurationException("Invalid integer value for " + key + ": " + value);
            }
        }
    }
    
    private static void setIfPresentLong(Properties props, String key, java.util.function.Consumer<Long> setter) {
        String value = props.getProperty(key);
        if (value != null && !value.trim().isEmpty()) {
            try {
                setter.accept(Long.parseLong(value.trim()));
            } catch (NumberFormatException e) {
                throw new ConfigurationException("Invalid long value for " + key + ": " + value);
            }
        }
    }
    
    private static void setIfPresentDouble(Properties props, String key, java.util.function.Consumer<Double> setter) {
        String value = props.getProperty(key);
        if (value != null && !value.trim().isEmpty()) {
            try {
                setter.accept(Double.parseDouble(value.trim()));
            } catch (NumberFormatException e) {
                throw new ConfigurationException("Invalid double value for " + key + ": " + value);
            }
        }
    }
    
    private static void setIfPresentBoolean(Properties props, String key, java.util.function.Consumer<Boolean> setter) {
        String value = props.getProperty(key);
        if (value != null && !value.trim().isEmpty()) {
            setter.accept(Boolean.parseBoolean(value.trim()));
        }
    }
}
