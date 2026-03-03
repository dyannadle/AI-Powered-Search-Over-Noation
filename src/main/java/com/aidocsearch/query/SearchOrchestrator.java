package com.aidocsearch.query;

import com.aidocsearch.config.SearchConfig;
import com.aidocsearch.embedding.EmbeddingEngine;
import com.aidocsearch.models.Document;
import com.aidocsearch.models.DocumentChunk;
import com.aidocsearch.models.SearchResult;
import com.aidocsearch.processing.DocumentProcessor;
import com.aidocsearch.storage.ChromaVectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Orchestrates the full AI-powered search pipeline.
 * 
 * Pipeline:
 * 1. INGEST: Document → Chunk → Embed → Store in ChromaDB
 * 2. SEARCH: Query → Understand (LangChain4j) → Embed → Retrieve → Synthesize
 * Answer (LangChain4j)
 * 
 * Uses two LangChain4j custom chains:
 * - {@link QueryUnderstandingChain}: reformulates queries for better retrieval
 * - {@link AnswerSynthesisChain}: generates RAG-style answers from retrieved
 * context
 */
public class SearchOrchestrator {
    private static final Logger logger = LoggerFactory.getLogger(SearchOrchestrator.class);

    private final EmbeddingEngine embeddingEngine;
    private final DocumentProcessor documentProcessor;
    private final ChromaVectorStore vectorStore;
    private final QueryUnderstandingChain queryUnderstandingChain;
    private final AnswerSynthesisChain answerSynthesisChain;
    private final int defaultLimit;

    public SearchOrchestrator(SearchConfig config) {
        Objects.requireNonNull(config, "SearchConfig cannot be null");
        this.embeddingEngine = new EmbeddingEngine(config);
        this.documentProcessor = new DocumentProcessor(config);
        this.vectorStore = new ChromaVectorStore(config);
        this.queryUnderstandingChain = new QueryUnderstandingChain(config);
        this.answerSynthesisChain = new AnswerSynthesisChain(config);
        this.defaultLimit = config.getDefaultResultLimit();
        logger.info("SearchOrchestrator initialized with LangChain4j chains");
    }

    // ==================== INGESTION PIPELINE ====================

    /**
     * Ingests a single document: chunks → embeds → stores in ChromaDB.
     */
    public void ingestDocument(Document document) {
        logger.info("Ingesting document: '{}' ({})", document.getTitle(), document.getSource());
        List<DocumentChunk> chunks = documentProcessor.process(document);

        if (chunks.isEmpty()) {
            logger.warn("No chunks generated for document: {}", document.getId());
            return;
        }

        List<String> chunkContents = chunks.stream()
                .map(DocumentChunk::getContent)
                .toList();

        List<List<Float>> embeddings = embeddingEngine.generateEmbeddingsBatch(chunkContents);
        vectorStore.storeChunks(chunks, embeddings);
        logger.info("Ingested {} chunks for '{}'", chunks.size(), document.getTitle());
    }

    /**
     * Ingests a batch of documents.
     */
    public int ingestDocuments(List<Document> documents) {
        logger.info("Starting batch ingestion of {} documents", documents.size());
        int successCount = 0;

        for (Document doc : documents) {
            try {
                ingestDocument(doc);
                successCount++;
            } catch (Exception e) {
                logger.error("Failed to ingest document '{}': {}", doc.getTitle(), e.getMessage());
            }
        }

        logger.info("Batch ingestion complete: {}/{} documents ingested", successCount, documents.size());
        return successCount;
    }

    // ==================== SEARCH PIPELINE ====================

    /**
     * Full search pipeline: understand → embed → retrieve → synthesize.
     * Returns a structured SearchResponse with both raw results and synthesized
     * answer.
     */
    public SearchResponse search(String query) {
        return search(query, defaultLimit);
    }

    public SearchResponse search(String query, int limit) {
        logger.info("Search pipeline started for query: '{}'", query);

        if (query == null || query.trim().isEmpty()) {
            return new SearchResponse(Collections.emptyList(), "Please provide a search query.", null);
        }

        // Step 1: Query Understanding (LangChain4j)
        ParsedQuery parsedQuery = queryUnderstandingChain.parse(query);
        logger.info("Query understood: intent={}, source={}", parsedQuery.getIntent(), parsedQuery.getSourceFilter());

        // Step 2: Generate embedding for the reformulated query
        String searchText = parsedQuery.getReformulatedQuery();
        List<Float> queryEmbedding = embeddingEngine.generateEmbedding(searchText);

        // Step 3: Retrieve from ChromaDB
        List<SearchResult> results = vectorStore.search(queryEmbedding, limit);

        // If sub-queries exist, search for each and merge results
        if (parsedQuery.hasSubQueries()) {
            List<SearchResult> allResults = new ArrayList<>(results);
            for (String subQuery : parsedQuery.getSubQueries()) {
                List<Float> subEmbedding = embeddingEngine.generateEmbedding(subQuery);
                allResults.addAll(vectorStore.search(subEmbedding, limit / 2));
            }
            // Deduplicate by chunk ID and re-sort
            results = allResults.stream()
                    .collect(java.util.stream.Collectors.toMap(
                            r -> r.getChunk().getId(),
                            r -> r,
                            (a, b) -> a.getRelevanceScore() >= b.getRelevanceScore() ? a : b))
                    .values().stream()
                    .sorted()
                    .limit(limit)
                    .toList();
        }

        // Step 4: Answer Synthesis (LangChain4j RAG)
        String synthesizedAnswer = answerSynthesisChain.synthesize(query, results);

        logger.info("Search pipeline complete: {} results found", results.size());
        return new SearchResponse(results, synthesizedAnswer, parsedQuery);
    }

    public void close() {
        embeddingEngine.close();
    }

    // ==================== RESPONSE MODEL ====================

    /**
     * Holds the complete search response: raw results, synthesized answer, and
     * parsed query.
     */
    public static class SearchResponse {
        private final List<SearchResult> results;
        private final String synthesizedAnswer;
        private final ParsedQuery parsedQuery;

        public SearchResponse(List<SearchResult> results, String synthesizedAnswer, ParsedQuery parsedQuery) {
            this.results = results;
            this.synthesizedAnswer = synthesizedAnswer;
            this.parsedQuery = parsedQuery;
        }

        public List<SearchResult> getResults() {
            return results;
        }

        public String getSynthesizedAnswer() {
            return synthesizedAnswer;
        }

        public ParsedQuery getParsedQuery() {
            return parsedQuery;
        }
    }
}
