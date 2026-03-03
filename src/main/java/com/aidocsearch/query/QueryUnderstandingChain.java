package com.aidocsearch.query;

import com.aidocsearch.config.SearchConfig;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * LangChain4j-powered chain that analyzes and reformulates user queries for
 * optimal retrieval.
 * 
 * This chain uses an LLM to:
 * 1. Expand abbreviations and clarify ambiguous terms
 * 2. Detect the user's intent (factual lookup, comparison, summarization, etc.)
 * 3. Identify source filters (e.g., "in my Notion" → source=notion)
 * 4. Decompose complex multi-part questions into focused sub-queries
 * 
 * Example:
 * Input: "what's the Q3 OKR status from notion?"
 * Output: ParsedQuery {
 * reformulated: "What is the status of Q3 OKRs (Objectives and Key Results)?",
 * intent: "status_check",
 * sourceFilter: "notion",
 * subQueries: []
 * }
 */
public class QueryUnderstandingChain {
    private static final Logger logger = LoggerFactory.getLogger(QueryUnderstandingChain.class);

    private static final String SYSTEM_PROMPT = """
            You are a query understanding assistant for a document search system.
            The user will provide a search query. Your job is to analyze it and return a structured response.

            Respond in EXACTLY this format (one field per line):
            REFORMULATED: <a clearer, expanded version of the query for semantic search>
            INTENT: <one of: factual_lookup, comparison, summarization, how_to, status_check, definition, general_search>
            SOURCE: <"notion" or "google_drive" if the user specifies a source, otherwise "all">
            SUB_QUERIES: <comma-separated list of sub-queries if the question is complex, otherwise "none">

            Rules:
            - Expand abbreviations (OKR → Objectives and Key Results, Q3 → third quarter)
            - Remove filler words but preserve meaning
            - If the user mentions "Notion", "notes", or "workspace", set SOURCE to "notion"
            - If the user mentions "Drive", "docs", "Google", or "files", set SOURCE to "google_drive"
            - Only decompose into sub-queries if the question has multiple distinct parts
            """;

    private final ChatLanguageModel chatModel;

    public QueryUnderstandingChain(SearchConfig config) {
        this.chatModel = OpenAiChatModel.builder()
                .apiKey(config.getOpenaiApiKey())
                .modelName("gpt-3.5-turbo")
                .temperature(0.0)
                .maxTokens(300)
                .build();
        logger.info("QueryUnderstandingChain initialized with LangChain4j OpenAI chat model");
    }

    /**
     * Analyzes a raw user query and returns a structured ParsedQuery.
     * Falls back to the original query if LLM processing fails.
     */
    public ParsedQuery parse(String rawQuery) {
        logger.debug("Parsing query: '{}'", rawQuery);

        try {
            Response<AiMessage> response = chatModel.generate(
                    SystemMessage.from(SYSTEM_PROMPT),
                    UserMessage.from(rawQuery));

            String output = response.content().text();
            return parseResponse(rawQuery, output);

        } catch (Exception e) {
            logger.warn("Query understanding failed, using raw query: {}", e.getMessage());
            return ParsedQuery.builder()
                    .originalQuery(rawQuery)
                    .reformulatedQuery(rawQuery)
                    .intent("general_search")
                    .build();
        }
    }

    /**
     * Parses the structured LLM response into a ParsedQuery object.
     */
    private ParsedQuery parseResponse(String originalQuery, String llmOutput) {
        String reformulated = originalQuery;
        String intent = "general_search";
        String source = null;
        List<String> subQueries = new ArrayList<>();

        for (String line : llmOutput.split("\\n")) {
            line = line.trim();
            if (line.startsWith("REFORMULATED:")) {
                reformulated = line.substring("REFORMULATED:".length()).trim();
            } else if (line.startsWith("INTENT:")) {
                intent = line.substring("INTENT:".length()).trim().toLowerCase();
            } else if (line.startsWith("SOURCE:")) {
                String src = line.substring("SOURCE:".length()).trim().toLowerCase();
                if (!"all".equals(src) && !src.isEmpty()) {
                    source = src;
                }
            } else if (line.startsWith("SUB_QUERIES:")) {
                String sq = line.substring("SUB_QUERIES:".length()).trim();
                if (!"none".equalsIgnoreCase(sq) && !sq.isEmpty()) {
                    for (String part : sq.split(",")) {
                        String trimmed = part.trim();
                        if (!trimmed.isEmpty()) {
                            subQueries.add(trimmed);
                        }
                    }
                }
            }
        }

        ParsedQuery parsed = ParsedQuery.builder()
                .originalQuery(originalQuery)
                .reformulatedQuery(reformulated)
                .intent(intent)
                .sourceFilter(source)
                .subQueries(subQueries)
                .build();

        logger.info("Parsed query: {}", parsed);
        return parsed;
    }
}
