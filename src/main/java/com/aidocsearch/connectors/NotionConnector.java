package com.aidocsearch.connectors;

import com.aidocsearch.config.SearchConfig;
import com.aidocsearch.models.Document;
import com.aidocsearch.models.DocumentMetadata;
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
import java.util.List;
import java.util.Objects;

/**
 * Connector for the Notion API that fetches pages and extracts their content.
 * 
 * Uses Notion's REST API directly via OkHttp to:
 * 1. Search for all accessible pages in the workspace
 * 2. Extract page titles from properties
 * 3. Fetch block children to build full page text content
 * 4. Handle paragraphs, headings, lists, code blocks, and quotes
 */
public class NotionConnector {
    private static final Logger logger = LoggerFactory.getLogger(NotionConnector.class);
    private static final String NOTION_API_BASE = "https://api.notion.com/v1";
    private static final String NOTION_VERSION = "2022-06-28";
    private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final Gson gson;
    private final String apiKey;
    private final String workspaceId;

    public NotionConnector(SearchConfig config) {
        Objects.requireNonNull(config, "SearchConfig cannot be null");
        this.apiKey = config.getNotionApiKey();
        this.workspaceId = config.getNotionWorkspaceId();

        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("Notion API key is required");
        }

        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(java.time.Duration.ofSeconds(30))
                .readTimeout(java.time.Duration.ofSeconds(60))
                .build();
        this.gson = new Gson();

        logger.info("NotionConnector initialized for workspace: {}", workspaceId);
    }

    /**
     * Fetches all accessible pages from Notion and extracts their content.
     */
    public List<Document> fetchDocuments() {
        logger.info("Fetching documents from Notion workspace: {}", workspaceId);
        List<Document> documents = new ArrayList<>();

        try {
            String startCursor = null;
            boolean hasMore = true;

            while (hasMore) {
                JsonObject requestBody = new JsonObject();
                requestBody.add("filter", createPageFilter());
                requestBody.addProperty("page_size", 100);
                if (startCursor != null) {
                    requestBody.addProperty("start_cursor", startCursor);
                }

                String responseJson = notionPost("/search", requestBody);
                if (responseJson == null)
                    break;

                JsonObject response = gson.fromJson(responseJson, JsonObject.class);
                JsonArray results = response.getAsJsonArray("results");

                if (results != null) {
                    for (JsonElement element : results) {
                        try {
                            JsonObject page = element.getAsJsonObject();
                            if ("page".equals(getStr(page, "object"))) {
                                Document doc = mapPageToDocument(page);
                                if (doc != null) {
                                    documents.add(doc);
                                }
                            }
                        } catch (Exception e) {
                            logger.warn("Failed to process page: {}", e.getMessage());
                        }
                    }
                }

                hasMore = response.has("has_more") && response.get("has_more").getAsBoolean();
                startCursor = response.has("next_cursor") && !response.get("next_cursor").isJsonNull()
                        ? response.get("next_cursor").getAsString()
                        : null;
            }

            logger.info("Successfully fetched {} documents from Notion", documents.size());
        } catch (Exception e) {
            logger.error("Failed to fetch documents from Notion: {}", e.getMessage(), e);
        }

        return documents;
    }

    private JsonObject createPageFilter() {
        JsonObject filter = new JsonObject();
        filter.addProperty("value", "page");
        filter.addProperty("property", "object");
        return filter;
    }

    /**
     * Converts a Notion page JSON to our Document model.
     */
    private Document mapPageToDocument(JsonObject page) {
        String pageId = getStr(page, "id");
        String title = extractTitle(page);
        String content = fetchPageContent(pageId);

        if (content == null || content.trim().isEmpty()) {
            logger.debug("Skipping empty page: {}", title);
            return null;
        }

        String createdTime = getStr(page, "created_time");
        String lastEditedTime = getStr(page, "last_edited_time");
        String url = getStr(page, "url");

        DocumentMetadata metadata = DocumentMetadata.builder()
                .author("Notion User")
                .createdAt(createdTime != null ? Instant.parse(createdTime) : Instant.now())
                .modifiedAt(lastEditedTime != null ? Instant.parse(lastEditedTime) : Instant.now())
                .url(url != null ? url : "")
                .permissions(List.of("read"))
                .build();

        return Document.builder()
                .id(pageId)
                .source("notion")
                .title(title)
                .content(content)
                .metadata(metadata)
                .build();
    }

    /**
     * Extracts the page title from the properties object.
     */
    private String extractTitle(JsonObject page) {
        JsonObject properties = page.getAsJsonObject("properties");
        if (properties == null)
            return "Untitled";

        for (String key : properties.keySet()) {
            JsonObject prop = properties.getAsJsonObject(key);
            if (prop != null && "title".equals(getStr(prop, "type"))) {
                JsonArray titleArray = prop.getAsJsonArray("title");
                if (titleArray != null && !titleArray.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (JsonElement el : titleArray) {
                        JsonObject rt = el.getAsJsonObject();
                        String plainText = getStr(rt, "plain_text");
                        if (plainText != null)
                            sb.append(plainText);
                    }
                    String title = sb.toString().trim();
                    if (!title.isEmpty())
                        return title;
                }
            }
        }
        return "Untitled";
    }

    /**
     * Fetches full page content by retrieving block children recursively.
     */
    private String fetchPageContent(String pageId) {
        StringBuilder content = new StringBuilder();
        fetchBlockChildren(pageId, content, 0);
        return content.toString().trim();
    }

    /**
     * Recursively fetches block children and appends their text.
     */
    private void fetchBlockChildren(String blockId, StringBuilder content, int depth) {
        try {
            String startCursor = null;
            boolean hasMore = true;

            while (hasMore) {
                String url = NOTION_API_BASE + "/blocks/" + blockId + "/children?page_size=100";
                if (startCursor != null) {
                    url += "&start_cursor=" + startCursor;
                }

                String responseJson = notionGet(url);
                if (responseJson == null)
                    break;

                JsonObject response = gson.fromJson(responseJson, JsonObject.class);
                JsonArray results = response.getAsJsonArray("results");

                if (results != null) {
                    for (JsonElement element : results) {
                        JsonObject block = element.getAsJsonObject();
                        String type = getStr(block, "type");
                        String indent = "  ".repeat(depth);

                        if (type != null) {
                            appendBlockContent(block, type, content, indent);
                        }

                        // Recurse into children
                        if (block.has("has_children") && block.get("has_children").getAsBoolean()) {
                            fetchBlockChildren(getStr(block, "id"), content, depth + 1);
                        }
                    }
                }

                hasMore = response.has("has_more") && response.get("has_more").getAsBoolean();
                startCursor = response.has("next_cursor") && !response.get("next_cursor").isJsonNull()
                        ? response.get("next_cursor").getAsString()
                        : null;
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch blocks for {}: {}", blockId, e.getMessage());
        }
    }

    /**
     * Appends text content from a block based on its type.
     */
    private void appendBlockContent(JsonObject block, String type, StringBuilder content, String indent) {
        switch (type) {
            case "paragraph":
                appendRichText(content, block.getAsJsonObject("paragraph"), indent);
                break;
            case "heading_1":
                content.append("\n").append(indent).append("# ");
                appendRichText(content, block.getAsJsonObject("heading_1"), "");
                break;
            case "heading_2":
                content.append("\n").append(indent).append("## ");
                appendRichText(content, block.getAsJsonObject("heading_2"), "");
                break;
            case "heading_3":
                content.append("\n").append(indent).append("### ");
                appendRichText(content, block.getAsJsonObject("heading_3"), "");
                break;
            case "bulleted_list_item":
                content.append(indent).append("• ");
                appendRichText(content, block.getAsJsonObject("bulleted_list_item"), "");
                break;
            case "numbered_list_item":
                content.append(indent).append("1. ");
                appendRichText(content, block.getAsJsonObject("numbered_list_item"), "");
                break;
            case "to_do":
                JsonObject todo = block.getAsJsonObject("to_do");
                boolean checked = todo != null && todo.has("checked") && todo.get("checked").getAsBoolean();
                content.append(indent).append(checked ? "[x] " : "[ ] ");
                appendRichText(content, todo, "");
                break;
            case "code":
                content.append(indent).append("```\n");
                appendRichText(content, block.getAsJsonObject("code"), indent);
                content.append(indent).append("```\n");
                break;
            case "quote":
                content.append(indent).append("> ");
                appendRichText(content, block.getAsJsonObject("quote"), "");
                break;
            case "callout":
                content.append(indent).append("💡 ");
                appendRichText(content, block.getAsJsonObject("callout"), "");
                break;
            case "divider":
                content.append(indent).append("---\n");
                break;
            case "toggle":
                appendRichText(content, block.getAsJsonObject("toggle"), indent);
                break;
            default:
                break;
        }
    }

    /**
     * Extracts rich_text array from a block type object and appends plain text.
     */
    private void appendRichText(StringBuilder content, JsonObject blockTypeObj, String indent) {
        if (blockTypeObj == null) {
            content.append("\n");
            return;
        }

        JsonArray richText = blockTypeObj.getAsJsonArray("rich_text");
        if (richText == null || richText.isEmpty()) {
            content.append("\n");
            return;
        }

        content.append(indent);
        for (JsonElement el : richText) {
            JsonObject rt = el.getAsJsonObject();
            String plainText = getStr(rt, "plain_text");
            if (plainText != null) {
                content.append(plainText);
            }
        }
        content.append("\n");
    }

    // ==================== HTTP Helpers ====================

    private String notionPost(String path, JsonObject body) {
        RequestBody rb = RequestBody.create(gson.toJson(body), JSON_MEDIA);
        Request request = new Request.Builder()
                .url(NOTION_API_BASE + path)
                .header("Authorization", "Bearer " + apiKey)
                .header("Notion-Version", NOTION_VERSION)
                .header("Content-Type", "application/json")
                .post(rb)
                .build();
        return executeRequest(request);
    }

    private String notionGet(String url) {
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + apiKey)
                .header("Notion-Version", NOTION_VERSION)
                .get()
                .build();
        return executeRequest(request);
    }

    private String executeRequest(Request request) {
        try (Response response = httpClient.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : null;
            if (!response.isSuccessful()) {
                logger.error("Notion API error ({}): {}", response.code(), body);
                return null;
            }
            return body;
        } catch (IOException e) {
            logger.error("Notion API request failed: {}", e.getMessage());
            return null;
        }
    }

    private String getStr(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : null;
    }
}
