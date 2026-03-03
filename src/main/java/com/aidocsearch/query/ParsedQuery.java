package com.aidocsearch.query;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable model representing a parsed and enriched user query.
 * 
 * Produced by {@link QueryUnderstandingChain} after analyzing the user's raw
 * input.
 * Contains the reformulated query for better retrieval, detected intent,
 * optional source filter, and sub-queries for complex multi-part questions.
 */
public class ParsedQuery {
    private final String originalQuery;
    private final String reformulatedQuery;
    private final String intent;
    private final String sourceFilter; // "notion", "google_drive", or null for all
    private final List<String> subQueries;

    private ParsedQuery(Builder builder) {
        this.originalQuery = Objects.requireNonNull(builder.originalQuery);
        this.reformulatedQuery = builder.reformulatedQuery != null ? builder.reformulatedQuery : builder.originalQuery;
        this.intent = builder.intent != null ? builder.intent : "search";
        this.sourceFilter = builder.sourceFilter;
        this.subQueries = builder.subQueries != null ? List.copyOf(builder.subQueries) : Collections.emptyList();
    }

    public String getOriginalQuery() {
        return originalQuery;
    }

    public String getReformulatedQuery() {
        return reformulatedQuery;
    }

    public String getIntent() {
        return intent;
    }

    public String getSourceFilter() {
        return sourceFilter;
    }

    public List<String> getSubQueries() {
        return subQueries;
    }

    /** Whether a specific data source filter was detected. */
    public boolean hasSourceFilter() {
        return sourceFilter != null && !sourceFilter.isEmpty();
    }

    /** Whether the query was decomposed into sub-queries. */
    public boolean hasSubQueries() {
        return !subQueries.isEmpty();
    }

    @Override
    public String toString() {
        return "ParsedQuery{" +
                "original='" + originalQuery + '\'' +
                ", reformulated='" + reformulatedQuery + '\'' +
                ", intent='" + intent + '\'' +
                ", sourceFilter='" + sourceFilter + '\'' +
                ", subQueries=" + subQueries +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String originalQuery;
        private String reformulatedQuery;
        private String intent;
        private String sourceFilter;
        private List<String> subQueries;

        public Builder originalQuery(String q) {
            this.originalQuery = q;
            return this;
        }

        public Builder reformulatedQuery(String q) {
            this.reformulatedQuery = q;
            return this;
        }

        public Builder intent(String i) {
            this.intent = i;
            return this;
        }

        public Builder sourceFilter(String s) {
            this.sourceFilter = s;
            return this;
        }

        public Builder subQueries(List<String> sq) {
            this.subQueries = sq;
            return this;
        }

        public ParsedQuery build() {
            return new ParsedQuery(this);
        }
    }
}
