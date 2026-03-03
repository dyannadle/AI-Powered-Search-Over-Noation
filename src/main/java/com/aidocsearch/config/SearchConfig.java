package com.aidocsearch.config;

import java.util.Objects;

/**
 * Configuration class for the AI Document Search system.
 * Contains all configuration parameters with validation.
 */
public class SearchConfig {
    // Notion configuration
    private final String notionApiKey;
    private final String notionWorkspaceId;

    // Google Drive configuration
    private final String googleCredentialsPath;

    // OpenAI configuration
    private final String openaiApiKey;
    private final String openaiEmbeddingModel;

    // ChromaDB configuration
    private final String chromaUrl;
    private final String chromaPersistDirectory;
    private final String chromaCollectionName;

    // Document processing configuration
    private final int chunkSize;
    private final int chunkOverlap;

    // Query configuration
    private final int defaultResultLimit;
    private final int maxResultLimit;
    private final double similarityThreshold;
    private final int maxQueryLength;

    // Performance configuration
    private final int maxConcurrentQueries;
    private final int embeddingBatchSize;
    private final int queryTimeoutSeconds;

    // Retry configuration
    private final int retryMaxAttempts;
    private final long retryInitialDelayMs;
    private final long retryMaxDelayMs;
    private final double retryBackoffMultiplier;

    // Security configuration
    private final boolean encryptCredentials;
    private final boolean enforceHttps;

    private SearchConfig(Builder builder) {
        this.notionApiKey = builder.notionApiKey;
        this.notionWorkspaceId = builder.notionWorkspaceId;
        this.googleCredentialsPath = builder.googleCredentialsPath;
        this.openaiApiKey = builder.openaiApiKey;
        this.openaiEmbeddingModel = builder.openaiEmbeddingModel;
        this.chromaUrl = builder.chromaUrl;
        this.chromaPersistDirectory = builder.chromaPersistDirectory;
        this.chromaCollectionName = builder.chromaCollectionName;
        this.chunkSize = builder.chunkSize;
        this.chunkOverlap = builder.chunkOverlap;
        this.defaultResultLimit = builder.defaultResultLimit;
        this.maxResultLimit = builder.maxResultLimit;
        this.similarityThreshold = builder.similarityThreshold;
        this.maxQueryLength = builder.maxQueryLength;
        this.maxConcurrentQueries = builder.maxConcurrentQueries;
        this.embeddingBatchSize = builder.embeddingBatchSize;
        this.queryTimeoutSeconds = builder.queryTimeoutSeconds;
        this.retryMaxAttempts = builder.retryMaxAttempts;
        this.retryInitialDelayMs = builder.retryInitialDelayMs;
        this.retryMaxDelayMs = builder.retryMaxDelayMs;
        this.retryBackoffMultiplier = builder.retryBackoffMultiplier;
        this.encryptCredentials = builder.encryptCredentials;
        this.enforceHttps = builder.enforceHttps;
    }

    public String getNotionApiKey() {
        return notionApiKey;
    }

    public String getNotionWorkspaceId() {
        return notionWorkspaceId;
    }

    public String getGoogleCredentialsPath() {
        return googleCredentialsPath;
    }

    public String getOpenaiApiKey() {
        return openaiApiKey;
    }

    public String getOpenaiEmbeddingModel() {
        return openaiEmbeddingModel;
    }

    public String getChromaUrl() {
        return chromaUrl;
    }

    public String getChromaPersistDirectory() {
        return chromaPersistDirectory;
    }

    public String getChromaCollectionName() {
        return chromaCollectionName;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public int getChunkOverlap() {
        return chunkOverlap;
    }

    public int getDefaultResultLimit() {
        return defaultResultLimit;
    }

    public int getMaxResultLimit() {
        return maxResultLimit;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public int getMaxQueryLength() {
        return maxQueryLength;
    }

    public int getMaxConcurrentQueries() {
        return maxConcurrentQueries;
    }

    public int getEmbeddingBatchSize() {
        return embeddingBatchSize;
    }

    public int getQueryTimeoutSeconds() {
        return queryTimeoutSeconds;
    }

    public int getRetryMaxAttempts() {
        return retryMaxAttempts;
    }

    public long getRetryInitialDelayMs() {
        return retryInitialDelayMs;
    }

    public long getRetryMaxDelayMs() {
        return retryMaxDelayMs;
    }

    public double getRetryBackoffMultiplier() {
        return retryBackoffMultiplier;
    }

    public boolean isEncryptCredentials() {
        return encryptCredentials;
    }

    public boolean isEnforceHttps() {
        return enforceHttps;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SearchConfig that = (SearchConfig) o;
        return chunkSize == that.chunkSize &&
                chunkOverlap == that.chunkOverlap &&
                defaultResultLimit == that.defaultResultLimit &&
                maxResultLimit == that.maxResultLimit &&
                Double.compare(that.similarityThreshold, similarityThreshold) == 0 &&
                maxQueryLength == that.maxQueryLength &&
                maxConcurrentQueries == that.maxConcurrentQueries &&
                embeddingBatchSize == that.embeddingBatchSize &&
                queryTimeoutSeconds == that.queryTimeoutSeconds &&
                retryMaxAttempts == that.retryMaxAttempts &&
                retryInitialDelayMs == that.retryInitialDelayMs &&
                retryMaxDelayMs == that.retryMaxDelayMs &&
                Double.compare(that.retryBackoffMultiplier, retryBackoffMultiplier) == 0 &&
                encryptCredentials == that.encryptCredentials &&
                enforceHttps == that.enforceHttps &&
                Objects.equals(notionApiKey, that.notionApiKey) &&
                Objects.equals(notionWorkspaceId, that.notionWorkspaceId) &&
                Objects.equals(googleCredentialsPath, that.googleCredentialsPath) &&
                Objects.equals(openaiApiKey, that.openaiApiKey) &&
                Objects.equals(openaiEmbeddingModel, that.openaiEmbeddingModel) &&
                Objects.equals(chromaPersistDirectory, that.chromaPersistDirectory) &&
                Objects.equals(chromaCollectionName, that.chromaCollectionName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(notionApiKey, notionWorkspaceId, googleCredentialsPath,
                openaiApiKey, openaiEmbeddingModel, chromaPersistDirectory, chromaCollectionName,
                chunkSize, chunkOverlap, defaultResultLimit, maxResultLimit, similarityThreshold,
                maxQueryLength, maxConcurrentQueries, embeddingBatchSize, queryTimeoutSeconds,
                retryMaxAttempts, retryInitialDelayMs, retryMaxDelayMs, retryBackoffMultiplier,
                encryptCredentials, enforceHttps);
    }

    @Override
    public String toString() {
        return "SearchConfig{" +
                "notionApiKey='" + maskSensitive(notionApiKey) + '\'' +
                ", notionWorkspaceId='" + notionWorkspaceId + '\'' +
                ", googleCredentialsPath='" + googleCredentialsPath + '\'' +
                ", openaiApiKey='" + maskSensitive(openaiApiKey) + '\'' +
                ", openaiEmbeddingModel='" + openaiEmbeddingModel + '\'' +
                ", chromaPersistDirectory='" + chromaPersistDirectory + '\'' +
                ", chromaCollectionName='" + chromaCollectionName + '\'' +
                ", chunkSize=" + chunkSize +
                ", chunkOverlap=" + chunkOverlap +
                ", defaultResultLimit=" + defaultResultLimit +
                ", maxResultLimit=" + maxResultLimit +
                ", similarityThreshold=" + similarityThreshold +
                ", maxQueryLength=" + maxQueryLength +
                ", maxConcurrentQueries=" + maxConcurrentQueries +
                ", embeddingBatchSize=" + embeddingBatchSize +
                ", queryTimeoutSeconds=" + queryTimeoutSeconds +
                ", retryMaxAttempts=" + retryMaxAttempts +
                ", retryInitialDelayMs=" + retryInitialDelayMs +
                ", retryMaxDelayMs=" + retryMaxDelayMs +
                ", retryBackoffMultiplier=" + retryBackoffMultiplier +
                ", encryptCredentials=" + encryptCredentials +
                ", enforceHttps=" + enforceHttps +
                '}';
    }

    private String maskSensitive(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return "***";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String notionApiKey;
        private String notionWorkspaceId;
        private String googleCredentialsPath;
        private String openaiApiKey;
        private String openaiEmbeddingModel = "text-embedding-ada-002";
        private String chromaUrl = "http://localhost:8000/api/v1";
        private String chromaPersistDirectory = "./data/chroma";
        private String chromaCollectionName = "document-embeddings";
        private int chunkSize = 1000;
        private int chunkOverlap = 200;
        private int defaultResultLimit = 5;
        private int maxResultLimit = 20;
        private double similarityThreshold = 0.7;
        private int maxQueryLength = 500;
        private int maxConcurrentQueries = 10;
        private int embeddingBatchSize = 100;
        private int queryTimeoutSeconds = 3;
        private int retryMaxAttempts = 3;
        private long retryInitialDelayMs = 1000;
        private long retryMaxDelayMs = 60000;
        private double retryBackoffMultiplier = 2.0;
        private boolean encryptCredentials = true;
        private boolean enforceHttps = true;

        public Builder notionApiKey(String notionApiKey) {
            this.notionApiKey = notionApiKey;
            return this;
        }

        public Builder notionWorkspaceId(String notionWorkspaceId) {
            this.notionWorkspaceId = notionWorkspaceId;
            return this;
        }

        public Builder googleCredentialsPath(String googleCredentialsPath) {
            this.googleCredentialsPath = googleCredentialsPath;
            return this;
        }

        public Builder openaiApiKey(String openaiApiKey) {
            this.openaiApiKey = openaiApiKey;
            return this;
        }

        public Builder openaiEmbeddingModel(String openaiEmbeddingModel) {
            this.openaiEmbeddingModel = openaiEmbeddingModel;
            return this;
        }

        public Builder chromaUrl(String chromaUrl) {
            this.chromaUrl = chromaUrl;
            return this;
        }

        public Builder chromaPersistDirectory(String chromaPersistDirectory) {
            this.chromaPersistDirectory = chromaPersistDirectory;
            return this;
        }

        public Builder chromaCollectionName(String chromaCollectionName) {
            this.chromaCollectionName = chromaCollectionName;
            return this;
        }

        public Builder chunkSize(int chunkSize) {
            this.chunkSize = chunkSize;
            return this;
        }

        public Builder chunkOverlap(int chunkOverlap) {
            this.chunkOverlap = chunkOverlap;
            return this;
        }

        public Builder defaultResultLimit(int defaultResultLimit) {
            this.defaultResultLimit = defaultResultLimit;
            return this;
        }

        public Builder maxResultLimit(int maxResultLimit) {
            this.maxResultLimit = maxResultLimit;
            return this;
        }

        public Builder similarityThreshold(double similarityThreshold) {
            this.similarityThreshold = similarityThreshold;
            return this;
        }

        public Builder maxQueryLength(int maxQueryLength) {
            this.maxQueryLength = maxQueryLength;
            return this;
        }

        public Builder maxConcurrentQueries(int maxConcurrentQueries) {
            this.maxConcurrentQueries = maxConcurrentQueries;
            return this;
        }

        public Builder embeddingBatchSize(int embeddingBatchSize) {
            this.embeddingBatchSize = embeddingBatchSize;
            return this;
        }

        public Builder queryTimeoutSeconds(int queryTimeoutSeconds) {
            this.queryTimeoutSeconds = queryTimeoutSeconds;
            return this;
        }

        public Builder retryMaxAttempts(int retryMaxAttempts) {
            this.retryMaxAttempts = retryMaxAttempts;
            return this;
        }

        public Builder retryInitialDelayMs(long retryInitialDelayMs) {
            this.retryInitialDelayMs = retryInitialDelayMs;
            return this;
        }

        public Builder retryMaxDelayMs(long retryMaxDelayMs) {
            this.retryMaxDelayMs = retryMaxDelayMs;
            return this;
        }

        public Builder retryBackoffMultiplier(double retryBackoffMultiplier) {
            this.retryBackoffMultiplier = retryBackoffMultiplier;
            return this;
        }

        public Builder encryptCredentials(boolean encryptCredentials) {
            this.encryptCredentials = encryptCredentials;
            return this;
        }

        public Builder enforceHttps(boolean enforceHttps) {
            this.enforceHttps = enforceHttps;
            return this;
        }

        public SearchConfig build() {
            validate();
            return new SearchConfig(this);
        }

        private void validate() {
            // OpenAI API key is required
            if (openaiApiKey == null || openaiApiKey.trim().isEmpty()) {
                throw new ConfigurationException("OpenAI API key is required");
            }

            // At least one document source must be configured
            boolean hasNotionConfig = notionApiKey != null && !notionApiKey.trim().isEmpty();
            boolean hasGoogleConfig = googleCredentialsPath != null && !googleCredentialsPath.trim().isEmpty();

            if (!hasNotionConfig && !hasGoogleConfig) {
                throw new ConfigurationException(
                        "At least one document source (Notion or Google Drive) must be configured");
            }

            // Validate Notion configuration completeness
            if (hasNotionConfig && (notionWorkspaceId == null || notionWorkspaceId.trim().isEmpty())) {
                throw new ConfigurationException("Notion workspace ID is required when Notion API key is provided");
            }

            // Validate ChromaDB configuration
            if (chromaPersistDirectory == null || chromaPersistDirectory.trim().isEmpty()) {
                throw new ConfigurationException("ChromaDB persist directory is required");
            }
            if (chromaCollectionName == null || chromaCollectionName.trim().isEmpty()) {
                throw new ConfigurationException("ChromaDB collection name is required");
            }

            // Validate numeric ranges
            if (chunkSize <= 0) {
                throw new ConfigurationException("Chunk size must be positive");
            }
            if (chunkOverlap < 0) {
                throw new ConfigurationException("Chunk overlap must be non-negative");
            }
            if (chunkOverlap >= chunkSize) {
                throw new ConfigurationException("Chunk overlap must be less than chunk size");
            }
            if (defaultResultLimit <= 0) {
                throw new ConfigurationException("Default result limit must be positive");
            }
            if (maxResultLimit <= 0) {
                throw new ConfigurationException("Max result limit must be positive");
            }
            if (defaultResultLimit > maxResultLimit) {
                throw new ConfigurationException("Default result limit must not exceed max result limit");
            }
            if (similarityThreshold < 0.0 || similarityThreshold > 1.0) {
                throw new ConfigurationException("Similarity threshold must be between 0.0 and 1.0");
            }
            if (maxQueryLength <= 0) {
                throw new ConfigurationException("Max query length must be positive");
            }
            if (maxConcurrentQueries <= 0) {
                throw new ConfigurationException("Max concurrent queries must be positive");
            }
            if (embeddingBatchSize <= 0) {
                throw new ConfigurationException("Embedding batch size must be positive");
            }
            if (queryTimeoutSeconds <= 0) {
                throw new ConfigurationException("Query timeout must be positive");
            }
            if (retryMaxAttempts <= 0) {
                throw new ConfigurationException("Retry max attempts must be positive");
            }
            if (retryInitialDelayMs <= 0) {
                throw new ConfigurationException("Retry initial delay must be positive");
            }
            if (retryMaxDelayMs <= 0) {
                throw new ConfigurationException("Retry max delay must be positive");
            }
            if (retryInitialDelayMs > retryMaxDelayMs) {
                throw new ConfigurationException("Retry initial delay must not exceed max delay");
            }
            if (retryBackoffMultiplier <= 1.0) {
                throw new ConfigurationException("Retry backoff multiplier must be greater than 1.0");
            }
        }
    }
}
