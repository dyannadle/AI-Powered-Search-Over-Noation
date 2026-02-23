package com.aidocsearch.config;

/**
 * Exception thrown when configuration is invalid or cannot be loaded.
 */
public class ConfigurationException extends RuntimeException {
    
    public ConfigurationException(String message) {
        super(message);
    }
    
    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
