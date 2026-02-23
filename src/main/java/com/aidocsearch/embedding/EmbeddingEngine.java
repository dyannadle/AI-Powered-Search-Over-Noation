package com.aidocsearch.embedding;

import com.aidocsearch.config.SearchConfig;
import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.embedding.EmbeddingResult;
import com.theokanning.openai.service.OpenAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Embedding engine that generates vector embeddings using OpenAI's API.
 * Supports batch processing, retry logic with exponential backoff, and rate limiting.
 */
public class EmbeddingEngine {
    private static final Logger logger = LoggerFactory.getLogger(EmbeddingEngine.class);
    
    private final OpenAiService openAiService;
    private final String model;
    private final int maxRetries;
    private final long initialDelayMs;
    private final long maxDelayMs;
    private final double backoffMultiplier;
    private final int batchSize;
    
    /**
     * Creates an EmbeddingEngine with the provided configuration.
     *
     * @param config the search configuration containing API keys and retry settings
     * @throws IllegalArgumentException if config is null or API key is missing
     */
    public EmbeddingEngine(SearchConfig config) {
        Objects.requireNonNull(config, "SearchConfig cannot be null");
        
        String apiKey = config.getOpenaiApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("OpenAI API key is required");
        }
        
        this.model = config.getOpenaiEmbeddingModel();
        this.maxRetries = config.getRetryMaxAttempts();
        this.initialDelayMs = config.getRetryInitialDelayMs();
        this.maxDelayMs = config.getRetryMaxDelayMs();
        this.backoffMultiplier = config.getRetryBackoffMultiplier();
        this.batchSize = config.getEmbeddingBatchSize();
        
        // Create OpenAI service with timeout
        Duration timeout = Duration.ofSeconds(60);
        this.openAiService = new OpenAiService(apiKey, timeout);
        
        logger.info("EmbeddingEngine initialized with model: {}, maxRetries: {}, batchSize: {}", 
                    model, maxRetries, batchSize);
    }
    
    /**
     * Generates an embedding vector for a single text.
     *
     * @param text the text to embed
     * @return the embedding vector as a list of floats
     * @throws EmbeddingException if embedding generation fails after all retries
     * @throws IllegalArgumentException if text is null or empty
     */
    public List<Float> generateEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be null or empty");
        }
        
        List<String> texts = List.of(text);
        List<List<Float>> embeddings = generateEmbeddingsBatch(texts);
        return embeddings.get(0);
    }
    
    /**
     * Generates embedding vectors for multiple texts efficiently in batches.
     *
     * @param texts the list of texts to embed
     * @return a list of embedding vectors, one for each input text
     * @throws EmbeddingException if embedding generation fails after all retries
     * @throws IllegalArgumentException if texts is null or empty
     */
    public List<List<Float>> generateEmbeddingsBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            throw new IllegalArgumentException("Texts list cannot be null or empty");
        }
        
        // Validate all texts are non-empty
        for (int i = 0; i < texts.size(); i++) {
            if (texts.get(i) == null || texts.get(i).trim().isEmpty()) {
                throw new IllegalArgumentException("Text at index " + i + " is null or empty");
            }
        }
        
        List<List<Float>> allEmbeddings = new ArrayList<>();
        
        // Process in batches
        for (int i = 0; i < texts.size(); i += batchSize) {
            int end = Math.min(i + batchSize, texts.size());
            List<String> batch = texts.subList(i, end);
            
            logger.debug("Processing batch {}-{} of {} texts", i, end - 1, texts.size());
            
            List<List<Float>> batchEmbeddings = generateEmbeddingsBatchInternal(batch);
            allEmbeddings.addAll(batchEmbeddings);
        }
        
        return allEmbeddings;
    }
    
    /**
     * Internal method to generate embeddings for a batch with retry logic.
     */
    private List<List<Float>> generateEmbeddingsBatchInternal(List<String> texts) {
        return retryWithBackoff(() -> {
            try {
                EmbeddingRequest request = EmbeddingRequest.builder()
                        .model(model)
                        .input(texts)
                        .build();
                
                EmbeddingResult result = openAiService.createEmbeddings(request);
                
                // Extract embeddings and convert to List<Float>
                return result.getData().stream()
                        .map(embedding -> embedding.getEmbedding().stream()
                                .map(Double::floatValue)
                                .collect(Collectors.toList()))
                        .collect(Collectors.toList());
                        
            } catch (Exception e) {
                // Check if it's a rate limit error
                if (isRateLimitError(e)) {
                    logger.warn("Rate limit encountered, will retry with backoff");
                    throw new RateLimitException("Rate limit exceeded", e);
                }
                // Check if it's a retryable error
                if (isRetryableError(e)) {
                    logger.warn("Retryable error encountered: {}", e.getMessage());
                    throw new RetryableException("Transient error occurred", e);
                }
                // Non-retryable error
                throw new EmbeddingException("Failed to generate embeddings", e);
            }
        });
    }
    
    /**
     * Executes a function with exponential backoff retry logic.
     *
     * @param function the function to execute
     * @return the result of the function
     * @throws EmbeddingException if all retries are exhausted
     */
    private <T> T retryWithBackoff(RetryableFunction<T> function) throws EmbeddingException {
        int attempt = 0;
        long currentDelay = initialDelayMs;
        Exception lastException = null;
        
        while (attempt < maxRetries) {
            try {
                return function.execute();
            } catch (RateLimitException | RetryableException e) {
                lastException = e;
                attempt++;
                
                if (attempt >= maxRetries) {
                    logger.error("Max retries ({}) exhausted for embedding generation", maxRetries);
                    break;
                }
                
                // Calculate delay with exponential backoff
                long delayMs = Math.min(currentDelay, maxDelayMs);
                logger.info("Retry attempt {}/{} after {}ms delay", attempt, maxRetries, delayMs);
                
                try {
                    TimeUnit.MILLISECONDS.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new EmbeddingException("Retry interrupted", ie);
                }
                
                // Increase delay for next retry
                currentDelay = (long) (currentDelay * backoffMultiplier);
                
            } catch (EmbeddingException e) {
                // Non-retryable error, throw immediately
                throw e;
            } catch (Exception e) {
                // Unexpected exception, wrap and throw
                throw new EmbeddingException("Unexpected error during embedding generation", e);
            }
        }
        
        throw new EmbeddingException(
                "Failed to generate embeddings after " + maxRetries + " attempts", 
                lastException);
    }
    
    /**
     * Checks if an exception indicates a rate limit error.
     */
    private boolean isRateLimitError(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return false;
        }
        message = message.toLowerCase();
        return message.contains("rate limit") || 
               message.contains("429") ||
               message.contains("too many requests");
    }
    
    /**
     * Checks if an exception is retryable (network errors, timeouts, etc.).
     */
    private boolean isRetryableError(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return false;
        }
        message = message.toLowerCase();
        return message.contains("timeout") ||
               message.contains("connection") ||
               message.contains("network") ||
               message.contains("503") ||
               message.contains("502") ||
               message.contains("500");
    }
    
    /**
     * Closes the OpenAI service and releases resources.
     */
    public void close() {
        if (openAiService != null) {
            openAiService.shutdownExecutor();
            logger.info("EmbeddingEngine closed");
        }
    }
    
    /**
     * Functional interface for retryable operations.
     */
    @FunctionalInterface
    private interface RetryableFunction<T> {
        T execute() throws Exception;
    }
    
    /**
     * Exception indicating a rate limit was hit.
     */
    private static class RateLimitException extends RuntimeException {
        public RateLimitException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    /**
     * Exception indicating a retryable error occurred.
     */
    private static class RetryableException extends RuntimeException {
        public RetryableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
