package com.aidocsearch.processing;

import com.aidocsearch.config.SearchConfig;
import com.aidocsearch.models.Document;
import com.aidocsearch.models.DocumentChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Processes documents into searchable chunks using sentence-aware splitting.
 * 
 * Unlike naive fixed-size splitting, this processor:
 * 1. Normalizes whitespace and strips control characters
 * 2. Splits text at sentence boundaries (., !, ?, or newlines)
 * 3. Groups sentences into chunks that don't exceed the configured size
 * 4. Applies configurable overlap between chunks to preserve context
 * 5. Enriches chunk metadata with position information
 */
public class DocumentProcessor {
    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessor.class);

    /** Regex to split at sentence boundaries while preserving terminators */
    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile(
            "(?<=[.!?])\\s+|(?<=\\n)\\s*");

    private final int chunkSize;
    private final int chunkOverlap;

    public DocumentProcessor(SearchConfig config) {
        this.chunkSize = config.getChunkSize();
        this.chunkOverlap = config.getChunkOverlap();
        logger.debug("DocumentProcessor initialized: chunkSize={}, chunkOverlap={}", chunkSize, chunkOverlap);
    }

    /**
     * Processes a document into chunks using sentence-aware splitting with overlap.
     *
     * @param document the document to process
     * @return list of document chunks
     */
    public List<DocumentChunk> process(Document document) {
        String content = document.getContent();
        List<DocumentChunk> chunks = new ArrayList<>();

        if (content == null || content.trim().isEmpty()) {
            return chunks;
        }

        // Normalize: collapse whitespace, strip control chars
        content = normalizeText(content);

        // Split into sentences
        List<String> sentences = splitIntoSentences(content);

        // Group sentences into chunks with overlap
        List<String> chunkTexts = groupSentencesIntoChunks(sentences);

        // Build DocumentChunk objects
        int totalChunks = chunkTexts.size();
        for (int i = 0; i < totalChunks; i++) {
            DocumentChunk chunk = DocumentChunk.builder()
                    .id(UUID.randomUUID().toString())
                    .documentId(document.getId())
                    .content(chunkTexts.get(i).trim())
                    .metadata(document.getMetadata())
                    .chunkIndex(i)
                    .build();
            chunks.add(chunk);
        }

        logger.debug("Processed document '{}' ({} chars) into {} chunks",
                document.getTitle(), content.length(), chunks.size());
        return chunks;
    }

    /**
     * Normalizes text by collapsing whitespace and removing control characters.
     */
    private String normalizeText(String text) {
        // Remove control characters except newlines and tabs
        text = text.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
        // Collapse multiple spaces into one
        text = text.replaceAll(" {2,}", " ");
        // Collapse multiple newlines into double newline (paragraph breaks)
        text = text.replaceAll("\\n{3,}", "\n\n");
        return text.trim();
    }

    /**
     * Splits text into sentences using regex boundary detection.
     * Falls back to character-based splitting for very long sentences.
     */
    private List<String> splitIntoSentences(String text) {
        List<String> sentences = new ArrayList<>();
        Matcher matcher = SENTENCE_BOUNDARY.matcher(text);

        int lastEnd = 0;
        while (matcher.find()) {
            String sentence = text.substring(lastEnd, matcher.start()).trim();
            if (!sentence.isEmpty()) {
                // If a single sentence exceeds chunk size, split it at word boundaries
                if (sentence.length() > chunkSize) {
                    sentences.addAll(splitLongSentence(sentence));
                } else {
                    sentences.add(sentence);
                }
            }
            lastEnd = matcher.end();
        }

        // Don't forget the last segment
        if (lastEnd < text.length()) {
            String remaining = text.substring(lastEnd).trim();
            if (!remaining.isEmpty()) {
                if (remaining.length() > chunkSize) {
                    sentences.addAll(splitLongSentence(remaining));
                } else {
                    sentences.add(remaining);
                }
            }
        }

        // If no sentence boundaries found, treat the whole text as one
        if (sentences.isEmpty() && !text.trim().isEmpty()) {
            if (text.length() > chunkSize) {
                sentences.addAll(splitLongSentence(text));
            } else {
                sentences.add(text.trim());
            }
        }

        return sentences;
    }

    /**
     * Splits a sentence that exceeds chunkSize at word boundaries.
     */
    private List<String> splitLongSentence(String sentence) {
        List<String> parts = new ArrayList<>();
        String[] words = sentence.split("\\s+");
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            if (current.length() + word.length() + 1 > chunkSize && current.length() > 0) {
                parts.add(current.toString().trim());
                current = new StringBuilder();
            }
            if (current.length() > 0) {
                current.append(" ");
            }
            current.append(word);
        }

        if (current.length() > 0) {
            parts.add(current.toString().trim());
        }

        return parts;
    }

    /**
     * Groups sentences into chunks respecting chunkSize with overlap.
     * Overlap is achieved by re-including trailing sentences from the previous
     * chunk.
     */
    private List<String> groupSentencesIntoChunks(List<String> sentences) {
        List<String> chunks = new ArrayList<>();

        if (sentences.isEmpty()) {
            return chunks;
        }

        int i = 0;
        while (i < sentences.size()) {
            StringBuilder chunk = new StringBuilder();
            int startIndex = i;

            // Fill the chunk up to chunkSize
            while (i < sentences.size()) {
                String sentence = sentences.get(i);
                if (chunk.length() + sentence.length() + 1 > chunkSize && chunk.length() > 0) {
                    break;
                }
                if (chunk.length() > 0) {
                    chunk.append(" ");
                }
                chunk.append(sentence);
                i++;
            }

            chunks.add(chunk.toString());

            // If we've consumed all sentences, stop
            if (i >= sentences.size()) {
                break;
            }

            // Calculate overlap: step back enough sentences to cover chunkOverlap
            // characters
            if (chunkOverlap > 0) {
                int overlapChars = 0;
                int backtrack = i - 1;
                while (backtrack > startIndex && overlapChars < chunkOverlap) {
                    overlapChars += sentences.get(backtrack).length();
                    backtrack--;
                }
                i = backtrack + 1;
            }
        }

        return chunks;
    }
}
