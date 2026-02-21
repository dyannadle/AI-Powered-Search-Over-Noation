# AI Document Search

An AI-powered semantic search system that enables natural language queries over documents stored in Notion and Google Drive. The system uses OpenAI embeddings for vector representations, ChromaDB for efficient storage and retrieval, and LangChain4j for intelligent query orchestration.

## Features

- **Multi-Source Integration**: Search across Notion workspaces and Google Drive simultaneously
- **Semantic Search**: Natural language queries that understand meaning, not just keywords
- **Intelligent Chunking**: Documents are split intelligently while preserving context
- **LangChain Integration**: Advanced query processing with reformulation and synthesis
- **Property-Based Testing**: Comprehensive correctness guarantees through property tests

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- API credentials for:
  - Notion API
  - Google Drive API
  - OpenAI API

## Project Structure

```
ai-document-search/
├── src/
│   ├── main/
│   │   ├── java/com/aidocsearch/
│   │   │   ├── models/          # Core data models
│   │   │   ├── config/          # Configuration management
│   │   │   ├── connectors/      # Notion and Google Drive connectors
│   │   │   ├── processing/      # Document processing pipeline
│   │   │   ├── storage/         # Vector store integration
│   │   │   └── query/           # Query processing and retrieval
│   │   └── resources/
│   │       ├── application.conf # Main configuration file
│   │       └── logback.xml      # Logging configuration
│   └── test/
│       └── java/com/aidocsearch/
│           ├── unit/            # Unit tests
│           ├── property/        # Property-based tests
│           ├── integration/     # Integration tests
│           └── fixtures/        # Test data and utilities
├── pom.xml                      # Maven dependencies
└── README.md
```

## Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd ai-document-search
   ```

2. **Configure API credentials**
   
   Option A: Using environment variables (recommended)
   ```bash
   export NOTION_API_KEY="your-notion-api-key"
   export NOTION_WORKSPACE_ID="your-workspace-id"
   export GOOGLE_CREDENTIALS_PATH="/path/to/credentials.json"
   export OPENAI_API_KEY="your-openai-api-key"
   ```

   Option B: Using properties file
   ```bash
   cp src/main/resources/application.properties.example src/main/resources/application.properties
   # Edit application.properties with your credentials
   ```

3. **Build the project**
   ```bash
   mvn clean install
   ```

4. **Run tests**
   ```bash
   mvn test
   ```

## Dependencies

### Core Libraries
- **LangChain4j**: LLM orchestration and retrieval chains
- **OpenAI Java SDK**: Embedding generation
- **ChromaDB Java Client**: Vector storage and similarity search
- **Notion SDK**: Notion API integration
- **Google Drive API**: Google Drive integration

### Utilities
- **SLF4J + Logback**: Logging framework
- **Typesafe Config**: Configuration management
- **Lombok**: Reduce boilerplate code

### Testing
- **JUnit 5**: Unit testing framework
- **jqwik**: Property-based testing
- **Mockito**: Mocking framework

## Configuration

The application uses HOCON format for configuration (`application.conf`). Key configuration sections:

- **notion**: Notion API credentials
- **google-drive**: Google Drive credentials
- **openai**: OpenAI API settings
- **chroma**: Vector store configuration
- **processing**: Document chunking parameters
- **query**: Search behavior settings
- **retry**: Error handling and retry logic
- **security**: Security settings

## Logging

Logs are written to:
- Console (development)
- `logs/ai-document-search.log` (all logs)
- `logs/errors.log` (errors only)

Log levels can be configured in `src/main/resources/logback.xml`.

## Development

### Building
```bash
mvn clean compile
```

### Running Tests
```bash
# All tests
mvn test

# Unit tests only
mvn test -Dtest="com.aidocsearch.unit.**"

# Property tests only
mvn test -Dtest="com.aidocsearch.property.**"
```

### Packaging
```bash
mvn clean package
```

## License

[Add your license here]

## Contributing

[Add contribution guidelines here]
