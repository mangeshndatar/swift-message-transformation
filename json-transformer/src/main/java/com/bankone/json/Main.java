package com.bankone.json;

import com.bankone.parser.SwiftParser;
import com.bankone.ftp.FtpFetcher;
import com.bankone.db.FileAuditLogger;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

public class Main {

    private static final int MAX_RETRY = 1;
    private static final long RETRY_DELAY_MS = 2_000; // 5 seconds

    public static void main(String[] args) throws Exception {

        String inputFolder = "sample-messages";
        String processedFolder = "processed-messages";
        String failedFolder = "failed-messages";

        FtpFetcher fetcher = new FtpFetcher(inputFolder);
        SwiftParser parser = new SwiftParser();
        ObjectMapper mapper = new ObjectMapper();

        String jdbcURL = "jdbc:mysql://localhost:3306/demodb";
        String dbUser = "root";
        String dbPassword = "root";

        try (Connection connection = DriverManager.getConnection(jdbcURL, dbUser, dbPassword)) {

            FileAuditLogger auditLogger = new FileAuditLogger(connection);
            auditLogger.createAuditTableIfNotExists();

            Path processedPath = Paths.get(processedFolder);
            Path failedPath = Paths.get(failedFolder);
            if (!Files.exists(processedPath)) Files.createDirectories(processedPath);
            if (!Files.exists(failedPath)) Files.createDirectories(failedPath);

            List<File> messageFiles = fetcher.fetchMessageFiles();

            for (File file : messageFiles) {
            		String exception ="Successfully parsed & moved";
                String fileName = file.getName();
               
                if (auditLogger.isFileAlreadySuccessful(fileName,file.lastModified())) {
                    System.out.println("✅ Skipping already successfully processed file: " + fileName);
                    copyFile(file.toPath(), processedPath, fileName);  // Copy skipped successful file
                    continue;
                }

                boolean shouldRetry = auditLogger.isFileAlreadyFailed(fileName);
                int retryCount = 0;
                boolean success = false;

                if (!shouldRetry) {
                    System.out.println("📥 New file detected: " + fileName);
                } else {
                    System.out.println("🔁 Retrying failed file: " + fileName);
                }

                while (retryCount < MAX_RETRY && !success) {
                    retryCount++;
                    try {
                        String parsed = parser.parse(file, processedFolder);

                        // Simulate failure if message contains "FAIL"
                        if (parsed.contains("FAIL")) {
                            throw new RuntimeException("Simulated failure on file content");
                        }

                        System.out.println("\n✅ Transformed to JSON:\n" + parsed);
                        success = true;
                        auditLogger.insertOrUpdateAudit(fileName, "success", retryCount,exception);
                        copyFile(file.toPath(), processedPath, fileName);  // Copy success

                    } catch (Exception e) {
                        System.out.println("⚠️ Error processing file " + fileName + ", attempt " + retryCount + ": " + e.getMessage());
                        //e.printStackTrace(System.out);
                        System.out.println(e.getMessage());
                        exception = e.getMessage();
                        if (retryCount < MAX_RETRY) {
                            System.out.println("⏳ Waiting 5 seconds before retrying...");
                            try {
                                Thread.sleep(RETRY_DELAY_MS);
                            } catch (InterruptedException ie) {
                                System.out.println("⚠️ Retry sleep interrupted: " + ie.getMessage());
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                }

                if (!success) {
                		System.out.println();
                		System.out.println("❌❌❌❌❌❌"+exception+"❌❌❌❌❌");
                    System.out.println("❌ Failed after 3 retries: " + fileName);
                    auditLogger.insertOrUpdateAudit(fileName, "failed", retryCount,exception);
                    copyFile(file.toPath(), failedPath, fileName);  // Copy failed
                }
            }
        }
    }

    private static void copyFile(Path source, Path targetDir, String fileName) {
        try {
            if (!Files.exists(source)) {
                System.out.println("⚠️ Skipping copy: Source file does not exist: " + source);
                return;
            }

            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            Path target = targetDir.resolve(fileName);
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("📁 Copied file to: " + target);
        } catch (Exception e) {
            System.out.println("❌ Failed to copy file '" + fileName + "': " + e.getMessage());
            e.printStackTrace(System.out);
        }
    }
}