/**
 * Configuration management for the AI Document Search system.
 * 
 * <p>This package provides configuration loading from multiple sources with the following priority:
 * <ol>
 *   <li>Environment variables (highest priority)</li>
 *   <li>System properties</li>
 *   <li>application.properties file</li>
 *   <li>application.conf file (HOCON format)</li>
 *   <li>Default values (lowest priority)</li>
 * </ol>
 * 
 * <p>The {@link com.aidocsearch.config.ConfigLoader} class handles loading configuration from
 * properties or HOCON files, while {@link com.aidocsearch.config.SearchConfig} provides a
 * type-safe, immutable configuration object with validation.
 * 
 * <p>Example usage:
 * <pre>{@code
 * // Load from default location
 * SearchConfig config = ConfigLoader.loadConfig();
 * 
 * // Load from specific file
 * SearchConfig config = ConfigLoader.loadFromProperties("custom.properties");
 * 
 * // Build programmatically
 * SearchConfig config = SearchConfig.builder()
 *     .openaiApiKey("sk-...")
 *     .notionApiKey("secret_...")
 *     .build();
 * }</pre>
 */
package com.aidocsearch.config;
