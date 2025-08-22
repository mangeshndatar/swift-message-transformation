package com.bankone.ftp;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class FtpFetcher {

    private final Path messagesDir;

    public FtpFetcher(String messagesPath) {
        this.messagesDir = Paths.get(messagesPath);
    }

    public List<File> fetchMessageFiles() throws IOException {
        List<File> messageFiles = new ArrayList<>();

        if (!Files.exists(messagesDir)) {
            throw new IOException("❌ Messages directory not found: " + messagesDir.toAbsolutePath());
        }

        Files.list(messagesDir)
                .filter(Files::isRegularFile)
                .forEach(path -> messageFiles.add(path.toFile()));

        return messageFiles;
    }
}
