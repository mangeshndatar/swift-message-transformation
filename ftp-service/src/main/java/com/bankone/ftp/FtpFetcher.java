package com.bankone.ftp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FtpFetcher {

    private final Path messagesDir;
    private final List<String> failedFiles = new ArrayList<>();

    // DB connection details
    private static final String DB_URL = "jdbc:mysql://localhost:3306/demodb";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "Ashish@121";

    public FtpFetcher(String messagesPath) {
        this.messagesDir = Paths.get(messagesPath);
    }

    public List<String> fetchMessages() throws IOException {
        List<String> messages = new ArrayList<>();

        if (!Files.exists(messagesDir)) {
            throw new IOException("Messages directory not found: " + messagesDir.toAbsolutePath());
        }

        Files.list(messagesDir)
                .filter(Files::isRegularFile)
                .forEach(file -> {
                    int attempts = 0;
                    boolean success = false;

                    while (attempts < 3 && !success) {
                        attempts++;
                        try {
                            String content = Files.readString(file, StandardCharsets.UTF_8);
                            System.out.println("\n=== Fetching SWIFT message: " + file.getFileName() + " (Attempt " + attempts + ") ===");
                            System.out.println(content);
                            messages.add(content);
                            success = true;
                        } catch (IOException e) {
                            System.err.println("Error reading file " + file.getFileName() + " (Attempt " + attempts + "): " + e.getMessage());
                        }
                    }

                    if (!success) {
                        failedFiles.add(file.getFileName().toString());
                        saveFailureToDB(file.getFileName().toString(), attempts);
                    }
                });

        if (!failedFiles.isEmpty()) {
            System.err.println("\n⚠ Failed files after 3 attempts: " + failedFiles);
        }

        return messages;
    }

    private void saveFailureToDB(String fileName, int retryCount) {
        String sql = "INSERT INTO failed_files (file_name, retry_count, failure_time) VALUES (?, ?, NOW())";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, fileName);
            stmt.setInt(2, retryCount);
            stmt.executeUpdate();
            System.out.println("Saved failure record for file: " + fileName);
        } catch (SQLException e) {
            System.err.println("Error saving failure record to DB: " + e.getMessage());
        }
    }

    public List<String> getFailedFiles() {
        return failedFiles;
    }
}