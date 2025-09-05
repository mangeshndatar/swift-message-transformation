package com.bankone.db;

import java.sql.*;
import java.text.SimpleDateFormat;

public class FileAuditLogger {

    private final Connection connection;

    public FileAuditLogger(Connection connection) {
        this.connection = connection;
    }

    public void createAuditTableIfNotExists() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS file_audit (
        			id BIGINT AUTO_INCREMENT PRIMARY KEY,
        			message_id VARCHAR(100),
                file_name VARCHAR(255),
                status ENUM('success', 'failed') NOT NULL,
                retry_count INT DEFAULT 0,
                last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                reason VARCHAR(100)               
            )
            """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    public boolean isFileAlreadySuccessful(String transactionId) throws SQLException {

        String sql = "SELECT message_id FROM file_audit WHERE message_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, transactionId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return transactionId.equalsIgnoreCase(rs.getString("message_id"));
            }
        }
        return false;
    }

    public boolean isFileAlreadyFailed(String fileName) throws SQLException {
        String sql = "SELECT status FROM file_audit WHERE file_name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, fileName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return "failed".equalsIgnoreCase(rs.getString("status"));
            }
        }
        return false;
    }

    public void insertOrUpdateAudit(String fileName, String status, int retryCount,String reason,String transactionId) throws SQLException {
    System.out.println("########## Transactionid ######"+transactionId);   
    	String sql = """
            INSERT INTO file_audit (file_name, status, retry_count,reason,message_id)
            VALUES (?, ?, ?,?,?)
            ON DUPLICATE KEY UPDATE status = VALUES(status), retry_count = VALUES(retry_count)
            """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, fileName);
            stmt.setString(2, status);
            stmt.setInt(3, retryCount);
            stmt.setString(4, reason);
            stmt.setString(5, transactionId);
            stmt.executeUpdate();
        }
    }
    
    public void updateFileTimestamp(String fileName, Timestamp fileTimestamp) throws SQLException {
        String sql = """
            UPDATE file_audit
            SET last_updated = ?
            WHERE file_name = ?
            """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {

        		stmt.setTimestamp(1, fileTimestamp);
            stmt.setString(2, fileName);
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                System.out.println("✅ Updated timestamp for file: " + fileName);
            } else {
                System.out.println("⚠️ No file found with the name: " + fileName);
            }
        }
    }

    public boolean isFileModifiedAfterDb(Timestamp fileTimestamp, Timestamp dbTimestamp) {
        long fileSeconds = fileTimestamp.getTime() / 1000;
        long dbSeconds = dbTimestamp.getTime() / 1000;

        return fileSeconds > dbSeconds;
    }

}
