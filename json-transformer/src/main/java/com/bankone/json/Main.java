package com.bankone.json;

import com.bankone.parser.SwiftParser;
import com.bankone.ftp.FtpFetcher;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.*;
import java.util.*;

public class Main {

    private static final int MAX_RETRY = 3;

    public static void main(String[] args) throws Exception {

        String sampleFolder = "sample-messages";
        FtpFetcher fetcher = new FtpFetcher(sampleFolder);
        SwiftParser parser = new SwiftParser();
        ObjectMapper mapper = new ObjectMapper();

        // DB Connection
        String jdbcURL = "jdbc:mysql://localhost:3306/demodb"; // Change DB name
        String dbUser = "root"; // Change username
        String dbPassword = "Ashish@121"; // Change password

        Connection connection = DriverManager.getConnection(jdbcURL, dbUser, dbPassword);

        // Ensure failure table exists
        createFailureTableIfNotExists(connection);

        List<String> messages = fetcher.fetchMessages();

        for (String raw : messages) {
            boolean success = false;
            int retryCount = 0;

            while (retryCount < MAX_RETRY && !success) {
                try {
                    String parsed = parser.parse(raw);
                    String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsed);
                    System.out.println("\nTransformed to JSON\n" + json);
                    success = true;
                } catch (Exception e) {
                    retryCount++;
                    System.err.println("Error processing message, retry " + retryCount + ": " + e.getMessage());
                }
            }

            if (!success) {
                saveFailedRecord(connection, raw, retryCount);
            }
        }

        connection.close();
    }

    private static void createFailureTableIfNotExists(Connection conn) throws SQLException {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS failed_records (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "message TEXT, " +
                "retry_count INT, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
        }
    }

    private static void saveFailedRecord(Connection conn, String message, int retryCount) throws SQLException {
        String insertSQL = "INSERT INTO failed_records (message, retry_count) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            pstmt.setString(1, message);
            pstmt.setInt(2, retryCount);
            pstmt.executeUpdate();
            System.out.println("❌ Saved failed record to DB with retry count: " + retryCount);
        }
    }
}
