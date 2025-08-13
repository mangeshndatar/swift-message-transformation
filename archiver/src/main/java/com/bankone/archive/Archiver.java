package com.bankone.archive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class Archiver {

    private static final Logger logger = LoggerFactory.getLogger(Archiver.class);

    /**
     * Archives (moves) a file from source to target location.
     *
     * @param source Path of the file to archive
     * @param target Destination path where the file should be moved
     * @return true if archived successfully, false otherwise
     */
    public boolean archive(Path source, Path target) {
        if (source == null || target == null) {
            logger.error("Source or target path is null.");
            return false;
        }

        if (!Files.exists(source)) {
            logger.error("Source file does not exist: {}", source);
            return false;
        }

        try {
            // Ensure target directory exists
            Path parentDir = target.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }

            // Move file (replace if exists)
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);

            logger.info("Archived {} -> {}", source, target);
            return true;
        } catch (IOException e) {
            logger.error("Failed to archive file from {} to {}", source, target, e);
            return false;
        }
    }
}
