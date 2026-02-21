# Implementation Notes

## ChromaDB Client

Since there's no stable ChromaDB Java client available in Maven Central, we'll implement our own HTTP client wrapper using OkHttp and Gson. The ChromaDB REST API is straightforward and well-documented.

### ChromaDB REST API Endpoints

The VectorStore implementation will use these endpoints:

- `POST /api/v1/collections` - Create a collection
- `GET /api/v1/collections/{name}` - Get collection
- `POST /api/v1/collections/{name}/add` - Add embeddings
- `POST /api/v1/collections/{name}/query` - Query embeddings
- `POST /api/v1/collections/{name}/update` - Update embeddings
- `POST /api/v1/collections/{name}/delete` - Delete embeddings

### Implementation Approach

The `VectorStore` class will:
1. Use OkHttp for HTTP communication
2. Use Gson for JSON serialization/deserialization
3. Implement retry logic with exponential backoff
4. Handle ChromaDB-specific error responses

## Google Drive API Version

Using `v3-rev20240123-2.0.0` which is the latest stable version available in Maven Central.

## Dependencies Summary

All required dependencies are now properly configured:
- ✅ LangChain4j (0.35.0)
- ✅ OpenAI Java SDK (0.18.2)
- ✅ ChromaDB (custom HTTP client implementation)
- ✅ Notion SDK (1.11.0)
- ✅ Google Drive API (v3-rev20240123-2.0.0)
- ✅ SLF4J + Logback for logging
- ✅ Typesafe Config for configuration
- ✅ jqwik for property-based testing
- ✅ JUnit 5 + Mockito for unit testing
