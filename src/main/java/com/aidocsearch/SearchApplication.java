package com.aidocsearch;

import com.aidocsearch.config.ConfigLoader;
import com.aidocsearch.config.SearchConfig;
import com.aidocsearch.connectors.GoogleDriveConnector;
import com.aidocsearch.connectors.NotionConnector;
import com.aidocsearch.models.Document;
import com.aidocsearch.models.SearchResult;
import com.aidocsearch.query.SearchOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Main entry point for the AI Document Search application.
 * 
 * Supports two modes:
 * - {@code --ingest}: Fetches documents from Notion/Google Drive, processes,
 * and stores embeddings
 * - {@code --search} (default): Interactive CLI for querying stored documents
 * 
 * Usage:
 * java -jar ai-document-search.jar --ingest # Ingest from all configured
 * sources
 * java -jar ai-document-search.jar --search # Interactive search mode
 * java -jar ai-document-search.jar # Default: interactive search
 */
public class SearchApplication {
    private static final Logger logger = LoggerFactory.getLogger(SearchApplication.class);

    public static void main(String[] args) {
        logger.info("Starting AI Document Search Application...");

        try {
            SearchConfig config = ConfigLoader.loadConfig();
            SearchOrchestrator orchestrator = new SearchOrchestrator(config);

            String mode = args.length > 0 ? args[0] : "--search";

            switch (mode) {
                case "--ingest":
                    runIngestion(config, orchestrator);
                    break;
                case "--search":
                default:
                    runInteractiveSearch(orchestrator);
                    break;
                case "--help":
                    printHelp();
                    break;
            }

            orchestrator.close();
        } catch (Exception e) {
            logger.error("Application failed", e);
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Ingestion mode: fetches documents from all configured sources and indexes
     * them.
     */
    private static void runIngestion(SearchConfig config, SearchOrchestrator orchestrator) {
        System.out.println("\n=== Document Ingestion ===\n");
        List<Document> allDocuments = new ArrayList<>();

        // Fetch from Notion
        if (config.getNotionApiKey() != null && !config.getNotionApiKey().isEmpty()) {
            System.out.println("📥 Fetching from Notion...");
            try {
                NotionConnector notionConnector = new NotionConnector(config);
                List<Document> notionDocs = notionConnector.fetchDocuments();
                allDocuments.addAll(notionDocs);
                System.out.printf("   Found %d documents from Notion%n", notionDocs.size());
            } catch (Exception e) {
                System.err.println("   ⚠ Notion fetch failed: " + e.getMessage());
                logger.error("Notion ingestion failed", e);
            }
        } else {
            System.out.println("⏭ Skipping Notion (no API key configured)");
        }

        // Fetch from Google Drive
        if (config.getGoogleCredentialsPath() != null && !config.getGoogleCredentialsPath().isEmpty()) {
            System.out.println("📥 Fetching from Google Drive...");
            try {
                GoogleDriveConnector driveConnector = new GoogleDriveConnector(config);
                List<Document> driveDocs = driveConnector.fetchDocuments();
                allDocuments.addAll(driveDocs);
                System.out.printf("   Found %d documents from Google Drive%n", driveDocs.size());
            } catch (Exception e) {
                System.err.println("   ⚠ Google Drive fetch failed: " + e.getMessage());
                logger.error("Google Drive ingestion failed", e);
            }
        } else {
            System.out.println("⏭ Skipping Google Drive (no credentials configured)");
        }

        if (allDocuments.isEmpty()) {
            System.out.println("\n⚠ No documents found from any source. Check your configuration.");
            return;
        }

        // Process and index
        System.out.printf("%n🔄 Indexing %d documents...%n", allDocuments.size());
        int ingested = orchestrator.ingestDocuments(allDocuments);
        System.out.printf("✅ Successfully indexed %d/%d documents%n", ingested, allDocuments.size());
    }

    /**
     * Interactive search mode with RAG-powered answers.
     */
    private static void runInteractiveSearch(SearchOrchestrator orchestrator) {
        System.out.println("\n╔══════════════════════════════════════════╗");
        System.out.println("║     AI Document Search (RAG-powered)     ║");
        System.out.println("╠══════════════════════════════════════════╣");
        System.out.println("║  Type a question in natural language     ║");
        System.out.println("║  Type 'exit' to quit                     ║");
        System.out.println("╚══════════════════════════════════════════╝\n");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("🔍 > ");
                if (!scanner.hasNextLine())
                    break;
                String query = scanner.nextLine();

                if ("exit".equalsIgnoreCase(query.trim())) {
                    break;
                }
                if (query.trim().isEmpty()) {
                    continue;
                }

                System.out.println("\n⏳ Searching...\n");

                SearchOrchestrator.SearchResponse response = orchestrator.search(query);

                // Display parsed query info
                if (response.getParsedQuery() != null) {
                    System.out.printf("📋 Intent: %s | Source: %s%n",
                            response.getParsedQuery().getIntent(),
                            response.getParsedQuery().hasSourceFilter()
                                    ? response.getParsedQuery().getSourceFilter()
                                    : "all");
                    if (!response.getParsedQuery().getReformulatedQuery().equals(query)) {
                        System.out.println("🔄 Reformulated: " + response.getParsedQuery().getReformulatedQuery());
                    }
                }

                // Display synthesized answer
                System.out.println("\n" + response.getSynthesizedAnswer());

                // Display raw results
                displayResults(response.getResults());

                System.out.println("─".repeat(50) + "\n");
            }
        }

        System.out.println("Goodbye! 👋");
    }

    private static void displayResults(List<SearchResult> results) {
        if (results.isEmpty()) {
            return;
        }

        System.out.println("\n📄 Raw Result Chunks:");
        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            System.out.printf("  %d. [%s] (Score: %.2f)%n",
                    i + 1, result.getSourceDocument(), result.getRelevanceScore());
            String content = result.getChunk().getContent();
            String snippet = content.length() > 120 ? content.substring(0, 120) + "..." : content;
            System.out.println("     " + snippet);
        }
    }

    private static void printHelp() {
        System.out.println("AI Document Search — Semantic search over Notion & Google Drive\n");
        System.out.println("Usage:");
        System.out.println("  --ingest    Fetch documents from configured sources and index them");
        System.out.println("  --search    Interactive search mode (default)");
        System.out.println("  --help      Show this help message\n");
        System.out.println("Environment Variables:");
        System.out.println("  OPENAI_API_KEY          Required for embeddings and LLM chains");
        System.out.println("  NOTION_API_KEY          Notion integration API key");
        System.out.println("  NOTION_WORKSPACE_ID     Notion workspace ID");
        System.out.println("  GOOGLE_CREDENTIALS_PATH Path to Google service account JSON");
    }
}
