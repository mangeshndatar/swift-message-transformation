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

    public boolean isFileAlreadySuccessful(String fileName, long fileLastModified) throws SQLException {

        Date date = new Date(fileLastModified);
        Timestamp fileTimestamp = new Timestamp(fileLastModified);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        String formattedDate = dateFormat.format(date);

        String sql = "SELECT status,last_updated FROM file_audit WHERE file_name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, fileName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
            		System.out.println("FROM FILE : ✅✅"+formattedDate);
                Timestamp dbTimestamp = rs.getTimestamp("last_updated");
                System.out.println("FROM DB : ✅✅"+dbTimestamp);
            		if (fileTimestamp.after(dbTimestamp)) {
            			System.out.println("✅ ✅ ✅ ✅ ✅  found updated file content");
            			updateFileTimestamp(fileName);
            			return false;
            		}
                return "success".equalsIgnoreCase(rs.getString("status"));
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

    public void insertOrUpdateAudit(String fileName, String status, int retryCount,String reason) throws SQLException {
        String sql = """
            INSERT INTO file_audit (file_name, status, retry_count,reason)
            VALUES (?, ?, ?,?)
            ON DUPLICATE KEY UPDATE status = VALUES(status), retry_count = VALUES(retry_count)
            """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, fileName);
            stmt.setString(2, status);
            stmt.setInt(3, retryCount);
            stmt.setString(4, reason);
            stmt.executeUpdate();
        }
    }
    
    public void updateFileTimestamp(String fileName) throws SQLException {
        String sql = """
            UPDATE file_audit
            SET last_updated = CURRENT_TIMESTAMP
            WHERE file_name = ?
            """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, fileName);
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                System.out.println("✅ Updated timestamp for file: " + fileName);
            } else {
                System.out.println("⚠️ No file found with the name: " + fileName);
            }
        }
    }

}
