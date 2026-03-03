package com.aidocsearch.storage;

import com.aidocsearch.config.SearchConfig;
import com.aidocsearch.models.DocumentChunk;
import com.aidocsearch.models.DocumentMetadata;
import com.aidocsearch.models.SearchResult;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ChromaDB vector store client for storing and retrieving document embeddings.
 * 
 * Communicates with ChromaDB's REST API to:
 * 1. Create/get collections on initialization
 * 2. Store document chunks with their embedding vectors
 * 3. Perform similarity searches against stored embeddings
 * 4. Parse response JSON into SearchResult objects with relevance scores
 */
public class ChromaVectorStore {
    private static final Logger logger = LoggerFactory.getLogger(ChromaVectorStore.class);
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final Gson gson;
    private final String baseUrl;
    private final String collectionName;
    private String collectionId;

    public ChromaVectorStore(SearchConfig config) {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(java.time.Duration.ofSeconds(30))
                .readTimeout(java.time.Duration.ofSeconds(60))
                .writeTimeout(java.time.Duration.ofSeconds(60))
                .build();
        this.gson = new Gson();
        this.baseUrl = config.getChromaUrl() != null ? config.getChromaUrl() : "http://localhost:8000/api/v1";
        this.collectionName = config.getChromaCollectionName();

        logger.info("ChromaVectorStore initialized: url={}, collection={}", baseUrl, collectionName);
        initializeCollection();
    }

    /**
     * Creates or retrieves the collection in ChromaDB.
     */
    private void initializeCollection() {
        try {
            Map<String, Object> body = Map.of(
                    "name", collectionName,
                    "get_or_create", true);
            String responseJson = sendRequest("/collections", body);
            if (responseJson != null) {
                JsonObject response = gson.fromJson(responseJson, JsonObject.class);
                this.collectionId = response.get("id").getAsString();
                logger.info("Collection '{}' ready with id: {}", collectionName, collectionId);
            }
        } catch (Exception e) {
            logger.error("Failed to initialize ChromaDB collection: {}", e.getMessage(), e);
        }
    }

    /**
     * Stores document chunks with their embedding vectors in ChromaDB.
     * Performs a batch upsert operation.
     */
    public void storeChunks(List<DocumentChunk> chunks, List<List<Float>> embeddings) {
        if (chunks.isEmpty()) {
            return;
        }
        logger.info("Storing {} chunks in ChromaDB collection '{}'", chunks.size(), collectionName);

        List<String> ids = new ArrayList<>();
        List<Map<String, String>> metadatas = new ArrayList<>();
        List<String> documents = new ArrayList<>();

        for (DocumentChunk chunk : chunks) {
            ids.add(chunk.getId());
            Map<String, String> meta = new HashMap<>();
            meta.put("document_id", chunk.getDocumentId());
            meta.put("chunk_index", String.valueOf(chunk.getChunkIndex()));
            if (chunk.getMetadata() != null) {
                if (chunk.getMetadata().getAuthor() != null) {
                    meta.put("author", chunk.getMetadata().getAuthor());
                }
                if (chunk.getMetadata().getUrl() != null) {
                    meta.put("source_url", chunk.getMetadata().getUrl());
                }
            }
            metadatas.add(meta);
            documents.add(chunk.getContent());
        }

        Map<String, Object> requestBody = Map.of(
                "ids", ids,
                "embeddings", embeddings,
                "metadatas", metadatas,
                "documents", documents);

        String path = collectionId != null
                ? "/collections/" + collectionId + "/upsert"
                : "/collections/" + collectionName + "/upsert";

        sendRequest(path, requestBody);
        logger.info("Successfully stored {} chunks", chunks.size());
    }

    /**
     * Performs a similarity search in ChromaDB and returns parsed SearchResult
     * objects.
     * 
     * @param queryEmbedding the embedding vector of the search query
     * @param limit          maximum number of results to return
     * @return list of search results ordered by similarity
     */
    public List<SearchResult> search(List<Float> queryEmbedding, int limit) {
        logger.info("Searching ChromaDB with limit: {}", limit);

        Map<String, Object> requestBody = Map.of(
                "query_embeddings", List.of(queryEmbedding),
                "n_results", limit,
                "include", List.of("documents", "metadatas", "distances"));

        String path = collectionId != null
                ? "/collections/" + collectionId + "/query"
                : "/collections/" + collectionName + "/query";

        String responseJson = sendRequest(path, requestBody);
        return parseSearchResponse(responseJson);
    }

    /**
     * Parses ChromaDB's query response JSON into SearchResult objects.
     * 
     * ChromaDB response format:
     * {
     * "ids": [["id1", "id2"]],
     * "documents": [["doc1", "doc2"]],
     * "metadatas": [[{...}, {...}]],
     * "distances": [[0.1, 0.5]]
     * }
     */
    private List<SearchResult> parseSearchResponse(String responseJson) {
        List<SearchResult> results = new ArrayList<>();

        if (responseJson == null || responseJson.isEmpty()) {
            return results;
        }

        try {
            JsonObject response = gson.fromJson(responseJson, JsonObject.class);

            JsonArray idsOuter = response.getAsJsonArray("ids");
            JsonArray documentsOuter = response.getAsJsonArray("documents");
            JsonArray metadatasOuter = response.getAsJsonArray("metadatas");
            JsonArray distancesOuter = response.getAsJsonArray("distances");

            if (idsOuter == null || idsOuter.isEmpty()) {
                return results;
            }

            JsonArray ids = idsOuter.get(0).getAsJsonArray();
            JsonArray documents = documentsOuter != null ? documentsOuter.get(0).getAsJsonArray() : null;
            JsonArray metadatas = metadatasOuter != null ? metadatasOuter.get(0).getAsJsonArray() : null;
            JsonArray distances = distancesOuter != null ? distancesOuter.get(0).getAsJsonArray() : null;

            for (int i = 0; i < ids.size(); i++) {
                String id = ids.get(i).getAsString();
                String content = documents != null ? documents.get(i).getAsString() : "";
                float distance = distances != null ? distances.get(i).getAsFloat() : 0.0f;

                // ChromaDB returns distances (lower = better); convert to similarity score
                // (higher = better)
                float relevanceScore = 1.0f / (1.0f + distance);

                // Extract metadata
                String documentId = "";
                String sourceUrl = "";
                String author = "";
                int chunkIndex = 0;

                if (metadatas != null && i < metadatas.size()) {
                    JsonObject meta = metadatas.get(i).getAsJsonObject();
                    documentId = getStringOrDefault(meta, "document_id", "");
                    sourceUrl = getStringOrDefault(meta, "source_url", "");
                    author = getStringOrDefault(meta, "author", "Unknown");
                    chunkIndex = getIntOrDefault(meta, "chunk_index", 0);
                }

                // Build the DocumentChunk
                DocumentMetadata chunkMetadata = DocumentMetadata.builder()
                        .author(author)
                        .createdAt(Instant.now())
                        .modifiedAt(Instant.now())
                        .url(sourceUrl)
                        .permissions(List.of("read"))
                        .build();

                DocumentChunk chunk = DocumentChunk.builder()
                        .id(id)
                        .documentId(documentId)
                        .content(content)
                        .chunkIndex(chunkIndex)
                        .metadata(chunkMetadata)
                        .build();

                // Build the SearchResult
                SearchResult searchResult = SearchResult.builder()
                        .chunk(chunk)
                        .relevanceScore(relevanceScore)
                        .sourceDocument(documentId)
                        .build();

                results.add(searchResult);
            }

            logger.info("Parsed {} results from ChromaDB response", results.size());
        } catch (Exception e) {
            logger.error("Failed to parse ChromaDB response: {}", e.getMessage(), e);
        }

        return results;
    }

    /**
     * Deletes the current collection from ChromaDB.
     */
    public void deleteCollection() {
        if (collectionId != null) {
            try {
                Request request = new Request.Builder()
                        .url(baseUrl + "/collections/" + collectionId)
                        .delete()
                        .build();
                try (Response response = httpClient.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        logger.info("Deleted collection '{}'", collectionName);
                    }
                }
            } catch (IOException e) {
                logger.error("Failed to delete collection: {}", e.getMessage());
            }
        }
    }

    /**
     * Returns info about the current collection (item count, etc.).
     */
    public JsonObject getCollectionInfo() {
        try {
            Request request = new Request.Builder()
                    .url(baseUrl + "/collections/" + collectionId)
                    .get()
                    .build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    return gson.fromJson(response.body().string(), JsonObject.class);
                }
            }
        } catch (IOException e) {
            logger.error("Failed to get collection info: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Sends a POST request to ChromaDB and returns the response body as a string.
     */
    private String sendRequest(String path, Object body) {
        String json = gson.toJson(body);
        RequestBody rb = RequestBody.create(json, JSON_MEDIA_TYPE);
        Request request = new Request.Builder()
                .url(baseUrl + path)
                .post(rb)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : null;
            if (!response.isSuccessful()) {
                logger.error("ChromaDB request to {} failed ({}): {}", path, response.code(), responseBody);
            }
            return responseBody;
        } catch (IOException e) {
            logger.error("Error communicating with ChromaDB at {}: {}", path, e.getMessage());
            return null;
        }
    }

    private String getStringOrDefault(JsonObject obj, String key, String defaultValue) {
        JsonElement element = obj.get(key);
        return element != null && !element.isJsonNull() ? element.getAsString() : defaultValue;
    }

    private int getIntOrDefault(JsonObject obj, String key, int defaultValue) {
        JsonElement element = obj.get(key);
        if (element != null && !element.isJsonNull()) {
            try {
                return Integer.parseInt(element.getAsString());
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}
