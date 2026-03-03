# 🔍 AI Document Search

<div align="center">

**AI-powered semantic search over Notion and Google Drive documents**

[![Java](https://img.shields.io/badge/Java-17+-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Maven](https://img.shields.io/badge/Maven-3.6+-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)](https://maven.apache.org/)
[![LangChain4j](https://img.shields.io/badge/LangChain4j-0.35-2B6CB0?style=for-the-badge)](https://github.com/langchain4j/langchain4j)
[![OpenAI](https://img.shields.io/badge/OpenAI-Embeddings-412991?style=for-the-badge&logo=openai&logoColor=white)](https://openai.com/)
[![ChromaDB](https://img.shields.io/badge/ChromaDB-Vector%20Store-FF6F00?style=for-the-badge)](https://www.trychroma.com/)

</div>

---

## ✨ Features

| Feature | Description |
|---------|-------------|
| 🔗 **Multi-Source Integration** | Search across Notion workspaces and Google Drive simultaneously |
| 🧠 **Semantic Search** | Natural language queries that understand meaning, not just keywords |
| 📄 **Intelligent Chunking** | Documents are split with configurable overlap to preserve context |
| ⚡ **Batch Embeddings** | Efficient batch processing with retry logic and exponential backoff |
| 🔄 **LangChain Orchestration** | Advanced query processing with LangChain4j for retrieval chains |
| 🧪 **Property-Based Testing** | Comprehensive correctness guarantees through jqwik property tests |
| 🔒 **Security-First Config** | Credential masking, HTTPS enforcement, and env-variable overrides |

---

## 📋 Prerequisites

- **Java 17** or higher
- **Maven 3.6** or higher
- **ChromaDB** running locally (default: `http://localhost:8000`)
- API credentials for:
  - [Notion API](https://developers.notion.com/)
  - [Google Drive API](https://developers.google.com/drive)
  - [OpenAI API](https://platform.openai.com/)

---

## 🏗️ Architecture

```
ai-document-search/
├── src/
│   ├── main/java/com/aidocsearch/
│   │   ├── SearchApplication.java       # CLI entry point
│   │   ├── config/                      # Configuration management
│   │   │   ├── ConfigLoader.java        # Multi-source config loading (HOCON/properties/env)
│   │   │   ├── SearchConfig.java        # Validated configuration with Builder pattern
│   │   │   └── ConfigurationException.java
│   │   ├── connectors/                  # External data source connectors
│   │   │   ├── NotionConnector.java     # Notion API integration
│   │   │   └── GoogleDriveConnector.java # Google Drive API integration
│   │   ├── embedding/                   # Vector embedding generation
│   │   │   ├── EmbeddingEngine.java     # OpenAI embeddings with retry & batching
│   │   │   └── EmbeddingException.java
│   │   ├── processing/                  # Document processing pipeline
│   │   │   └── DocumentProcessor.java   # Chunking with configurable overlap
│   │   ├── models/                      # Immutable domain models
│   │   │   ├── Document.java
│   │   │   ├── DocumentChunk.java
│   │   │   ├── DocumentMetadata.java
│   │   │   └── SearchResult.java
│   │   ├── storage/                     # Vector store integration
│   │   │   └── ChromaVectorStore.java   # ChromaDB HTTP client
│   │   ├── query/                       # Search orchestration
│   │   │   ├── QueryUnderstandingChain.java # Query reformulation via LLM
│   │   │   ├── AnswerSynthesisChain.java    # RAG answer generation
│   │   │   ├── ParsedQuery.java         # Decomposed query object
│   │   │   └── SearchOrchestrator.java  # End-to-end RAG pipeline
│   ├── main/resources/
│   │   ├── application.conf             # HOCON configuration
│   │   ├── application.properties.example
│   │   └── logback.xml                  # Logging configuration
│   └── test/java/com/aidocsearch/
│       ├── unit/                        # Unit tests
│       ├── property/                    # Property-based tests (jqwik)
│       ├── integration/                 # Integration tests
│       └── fixtures/                    # Test data and utilities
├── pom.xml
└── README.md
```

---

## 🚀 Quick Start

### 1. Clone the repository

```bash
git clone <repository-url>
cd ai-document-search
```

### 2. Configure API credentials

**Option A: Environment variables (recommended)**

```bash
export OPENAI_API_KEY="your-openai-api-key"
export NOTION_API_KEY="your-notion-api-key"
export NOTION_WORKSPACE_ID="your-workspace-id"
export GOOGLE_CREDENTIALS_PATH="/path/to/credentials.json"
```

**Option B: Properties file**

```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
# Edit application.properties with your credentials
```

### 3. Build the project

```bash
mvn clean install
```

### 4. Run the application

```

Or run the packaged JAR:

```bash
mvn clean package
java -jar target/ai-document-search-1.0.0-SNAPSHOT.jar
```

---

## 🧪 Testing

```bash
# Run all tests (excludes API-dependent embedding tests)
mvn test

# Run unit tests only
mvn test -Dtest="com.aidocsearch.unit.**"

# Run property tests only
mvn test -Dtest="com.aidocsearch.property.**"

# Run embedding generation tests (requires OPENAI_API_KEY)
mvn test -Dtest="EmbeddingGenerationPropertyTest"
```

---

## ⚙️ Configuration

The application uses [HOCON](https://github.com/lightbend/config) format (`application.conf`). Configuration can be loaded from:

1. **Environment variables** (highest priority)
2. **System properties**
3. **`application.properties`** file
4. **`application.conf`** file (HOCON)
5. **Default values** in `SearchConfig.Builder`

### Key Configuration Sections

| Section | Description |
|---------|-------------|
| `notion` | Notion API key and workspace ID |
| `google-drive` | Google Drive credentials path |
| `openai` | OpenAI API key and embedding model |
| `chroma` | ChromaDB persist directory and collection name |
| `processing` | Document chunk size and overlap |
| `query` | Result limits, similarity threshold, max query length |
| `performance` | Concurrent queries, batch size, timeouts |
| `retry` | Max attempts, backoff multiplier, delay settings |
| `security` | Credential encryption and HTTPS enforcement |

---

## 📦 Dependencies

### Core Libraries

| Library | Purpose |
|---------|---------|
| [LangChain4j](https://github.com/langchain4j/langchain4j) | LLM orchestration and retrieval chains |
| [OpenAI Java SDK](https://github.com/TheoKanning/openai-java) | Embedding generation via OpenAI API |
| [OkHttp](https://square.github.io/okhttp/) | HTTP client for ChromaDB communication |
| [Notion SDK](https://github.com/seratch/notion-sdk-jvm) | Notion API integration |
| [Google Drive API](https://developers.google.com/drive) | Google Drive document access |
| [Typesafe Config](https://github.com/lightbend/config) | HOCON configuration management |
| [SLF4J + Logback](https://logback.qos.ch/) | Structured logging |

### Testing

| Library | Purpose |
|---------|---------|
| [JUnit 5](https://junit.org/junit5/) | Unit testing framework |
| [jqwik](https://jqwik.net/) | Property-based testing |
| [Mockito](https://site.mockito.org/) | Mocking framework |

---

## 📝 Logging

Logs are written to:

| Destination | Content |
|-------------|---------|
| **Console** | Development-mode logs |
| `logs/ai-document-search.log` | All application logs |
| `logs/errors.log` | Error-level logs only |

Configure log levels in `src/main/resources/logback.xml`.

---

## 🔄 How It Works

```
┌─────────────┐     ┌──────────────────┐     ┌───────────────────┐
│   Notion    │────▶│                  │────▶│                   │
│   API       │     │  DocumentProcessor│     │  EmbeddingEngine  │
│             │     │  (Chunking)       │     │  (OpenAI API)     │
├─────────────┤     │                  │     │                   │
│ Google Drive│────▶│                  │────▶│                   │
│   API       │     └──────────────────┘     └───────┬───────────┘
└─────────────┘                                      │
                                                     ▼
┌─────────────┐     ┌──────────────────┐     ┌───────────────────┐
│  User Query │────▶│ SearchOrchestrator│────▶│  ChromaVectorStore│
│  (CLI)      │     │  (Coordination)  │◀────│  (Similarity      │
│             │◀────│                  │     │   Search)         │
└─────────────┘     └──────────────────┘     └───────────────────┘
```

1. **Ingest**: Documents are fetched from Notion/Google Drive via connectors
2. **Process**: Documents are split into overlapping chunks for context preservation
3. **Embed**: Each chunk is converted to a 1536-dimensional vector via OpenAI
4. **Store**: Embeddings are stored in ChromaDB for efficient similarity search
5. **Query**: User queries are embedded and matched against stored vectors
6. **Rank**: Results are ranked by relevance score and recency

---

## 📄 License

[Add your license here]

## 🤝 Contributing

[Add contribution guidelines here]
