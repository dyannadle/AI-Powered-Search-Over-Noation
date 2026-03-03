package com.aidocsearch.query;

import com.aidocsearch.config.SearchConfig;
import com.aidocsearch.models.SearchResult;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * LangChain4j-powered RAG (Retrieval-Augmented Generation) answer synthesis
 * chain.
 * 
 * Takes retrieved document chunks and the original query, then uses an LLM to
 * synthesize a coherent, sourced answer. This is the "generation" step of RAG.
 * 
 * Features:
 * - Contextual answer generation from retrieved chunks
 * - Source attribution with document references
 * - Graceful handling when no relevant context is found
 * - Configurable model parameters for answer quality tuning
 */
public class AnswerSynthesisChain {
    private static final Logger logger = LoggerFactory.getLogger(AnswerSynthesisChain.class);

    private static final String SYSTEM_PROMPT = """
            You are a helpful AI assistant that answers questions based on provided document context.

            Rules:
            1. Answer ONLY based on the provided context. Do not use external knowledge.
            2. If the context doesn't contain enough information, say "I couldn't find enough information to answer this fully."
            3. Cite your sources by referencing the document chunk numbers (e.g., [Source 1], [Source 2]).
            4. Be concise but thorough.
            5. If multiple sources agree, synthesize them into a single cohesive answer.
            6. If sources conflict, mention the discrepancy.

            Format your response as:
            ANSWER: <your synthesized answer with source citations>
            CONFIDENCE: <high, medium, or low>
            """;

    private final ChatLanguageModel chatModel;

    public AnswerSynthesisChain(SearchConfig config) {
        this.chatModel = OpenAiChatModel.builder()
                .apiKey(config.getOpenaiApiKey())
                .modelName("gpt-3.5-turbo")
                .temperature(0.2)
                .maxTokens(800)
                .build();
        logger.info("AnswerSynthesisChain initialized with LangChain4j OpenAI chat model");
    }

    /**
     * Synthesizes a natural language answer from retrieved search results.
     *
     * @param query   the original user query
     * @param results the retrieved search results with document chunks
     * @return a synthesized answer string with source attributions
     */
    public String synthesize(String query, List<SearchResult> results) {
        if (results == null || results.isEmpty()) {
            return "No relevant documents were found for your query.";
        }

        logger.info("Synthesizing answer from {} results for query: '{}'", results.size(), query);

        String context = buildContext(results);

        try {
            String userPrompt = String.format(
                    "Question: %s\n\nDocument Context:\n%s",
                    query, context);

            Response<AiMessage> response = chatModel.generate(
                    SystemMessage.from(SYSTEM_PROMPT),
                    UserMessage.from(userPrompt));

            String output = response.content().text();
            return formatAnswer(output, results);

        } catch (Exception e) {
            logger.error("Answer synthesis failed: {}", e.getMessage(), e);
            return buildFallbackAnswer(results);
        }
    }

    /**
     * Builds the context string from search results for the LLM prompt.
     */
    private String buildContext(List<SearchResult> results) {
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            context.append(String.format(
                    "[Source %d] (Document: %s, Relevance: %.2f)\n%s\n\n",
                    i + 1,
                    result.getSourceDocument(),
                    result.getRelevanceScore(),
                    result.getChunk().getContent()));
        }
        return context.toString();
    }

    /**
     * Formats the LLM answer, appending source references.
     */
    private String formatAnswer(String llmOutput, List<SearchResult> results) {
        StringBuilder formatted = new StringBuilder();

        // Extract answer and confidence from structured output
        String answer = llmOutput;
        String confidence = "medium";

        for (String line : llmOutput.split("\\n")) {
            if (line.trim().startsWith("ANSWER:")) {
                answer = line.substring("ANSWER:".length()).trim();
            } else if (line.trim().startsWith("CONFIDENCE:")) {
                confidence = line.substring("CONFIDENCE:".length()).trim().toLowerCase();
            }
        }

        formatted.append(answer).append("\n\n");
        formatted.append("📊 Confidence: ").append(confidence).append("\n");
        formatted.append("📚 Sources:\n");

        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            String url = result.getChunk().getMetadata() != null
                    ? result.getChunk().getMetadata().getUrl()
                    : "";
            formatted.append(String.format("  [%d] %s (score: %.2f)%s\n",
                    i + 1,
                    result.getSourceDocument(),
                    result.getRelevanceScore(),
                    url != null && !url.isEmpty() ? " — " + url : ""));
        }

        return formatted.toString();
    }

    /**
     * Builds a fallback answer from raw search results when LLM synthesis fails.
     */
    private String buildFallbackAnswer(List<SearchResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("Here are the most relevant excerpts I found:\n\n");
        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            String content = result.getChunk().getContent();
            String snippet = content.length() > 200 ? content.substring(0, 200) + "..." : content;
            sb.append(String.format("[%d] %s (score: %.2f)\n%s\n\n",
                    i + 1, result.getSourceDocument(), result.getRelevanceScore(), snippet));
        }
        return sb.toString();
    }
}
