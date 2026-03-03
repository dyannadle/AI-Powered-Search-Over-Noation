package com.aidocsearch.connectors;

import com.aidocsearch.config.SearchConfig;
import com.aidocsearch.models.Document;
import com.aidocsearch.models.DocumentMetadata;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Connector for Google Drive API to fetch and extract content from documents.
 *
 * Supports:
 * - Service account authentication via JSON credentials file
 * - Paginated file listing with MIME type filtering
 * - Content export for Google Docs (as plain text)
 * - Content download for plain text and markdown files
 */
public class GoogleDriveConnector {
    private static final Logger logger = LoggerFactory.getLogger(GoogleDriveConnector.class);

    private static final Set<String> EXPORTABLE_MIME_TYPES = Set.of(
            "application/vnd.google-apps.document" // Google Docs
    );

    private static final Set<String> DOWNLOADABLE_MIME_TYPES = Set.of(
            "text/plain",
            "text/markdown",
            "text/csv",
            "application/json");

    private final Drive driveService;

    public GoogleDriveConnector(SearchConfig config) throws Exception {
        Objects.requireNonNull(config, "SearchConfig cannot be null");
        String credentialsPath = config.getGoogleCredentialsPath();

        if (credentialsPath == null || credentialsPath.isEmpty()) {
            throw new IllegalArgumentException("Google credentials path is required");
        }

        // Load service account credentials
        GoogleCredentials credentials;
        try (InputStream is = new FileInputStream(credentialsPath)) {
            credentials = GoogleCredentials.fromStream(is)
                    .createScoped(Collections.singletonList(DriveScopes.DRIVE_READONLY));
        }

        this.driveService = new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("AI Document Search")
                .build();

        logger.info("GoogleDriveConnector initialized with service account credentials");
    }

    /**
     * Fetches all accessible documents from Google Drive with content extraction.
     * Paginates through all files and filters by supported MIME types.
     */
    public List<Document> fetchDocuments() {
        logger.info("Fetching documents from Google Drive");
        List<Document> documents = new ArrayList<>();

        try {
            String pageToken = null;

            do {
                FileList result = driveService.files().list()
                        .setPageSize(100)
                        .setPageToken(pageToken)
                        .setFields(
                                "nextPageToken, files(id, name, mimeType, webViewLink, createdTime, modifiedTime, owners)")
                        .setQ("trashed = false")
                        .execute();

                List<File> files = result.getFiles();
                if (files != null) {
                    for (File file : files) {
                        try {
                            String mimeType = file.getMimeType();
                            if (isSupported(mimeType)) {
                                String content = extractContent(file);
                                if (content != null && !content.trim().isEmpty()) {
                                    documents.add(mapFileToDocument(file, content));
                                }
                            }
                        } catch (Exception e) {
                            logger.warn("Failed to process file '{}': {}", file.getName(), e.getMessage());
                        }
                    }
                }

                pageToken = result.getNextPageToken();
            } while (pageToken != null);

            logger.info("Successfully fetched {} documents from Google Drive", documents.size());
        } catch (Exception e) {
            logger.error("Failed to fetch documents from Google Drive: {}", e.getMessage(), e);
        }

        return documents;
    }

    /**
     * Extracts the text content from a Google Drive file.
     * Google Docs are exported as plain text; other text files are downloaded
     * directly.
     */
    private String extractContent(File file) throws IOException {
        String mimeType = file.getMimeType();

        if (EXPORTABLE_MIME_TYPES.contains(mimeType)) {
            // Export Google Docs as plain text
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            driveService.files().export(file.getId(), "text/plain")
                    .executeMediaAndDownloadTo(outputStream);
            return outputStream.toString(StandardCharsets.UTF_8);
        }

        if (DOWNLOADABLE_MIME_TYPES.contains(mimeType)) {
            // Download text-based files directly
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            driveService.files().get(file.getId())
                    .executeMediaAndDownloadTo(outputStream);
            return outputStream.toString(StandardCharsets.UTF_8);
        }

        return null;
    }

    private boolean isSupported(String mimeType) {
        return EXPORTABLE_MIME_TYPES.contains(mimeType) || DOWNLOADABLE_MIME_TYPES.contains(mimeType);
    }

    private Document mapFileToDocument(File file, String content) {
        String author = file.getOwners() != null && !file.getOwners().isEmpty()
                ? file.getOwners().get(0).getDisplayName()
                : "Unknown";

        DocumentMetadata metadata = DocumentMetadata.builder()
                .author(author)
                .createdAt(Instant.ofEpochMilli(file.getCreatedTime().getValue()))
                .modifiedAt(Instant.ofEpochMilli(file.getModifiedTime().getValue()))
                .url(file.getWebViewLink())
                .permissions(List.of("read"))
                .build();

        return Document.builder()
                .id(file.getId())
                .source("google_drive")
                .title(file.getName())
                .content(content)
                .metadata(metadata)
                .build();
    }
}
