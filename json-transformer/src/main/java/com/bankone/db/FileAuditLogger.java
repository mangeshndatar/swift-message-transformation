package com.bankone.db;

import java.sql.*;

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

    public boolean isFileAlreadySuccessful(String fileName) throws SQLException {
        String sql = "SELECT status FROM file_audit WHERE file_name = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, fileName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
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
}
