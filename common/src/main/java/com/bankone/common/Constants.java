package com.bankone.common;

/**
 * Application-wide constant values.
 * Keeping all constants in one place makes it easy to update
 * and avoid magic strings/numbers in the code.
 */
public class Constants {

    // ==== Folders ====
    public static final String SAMPLE_FOLDER = "sample-messages";
    public static final String ARCHIVE_FOLDER = "archive";
    public static final String ERROR_FOLDER = "error";

    // ==== Retry Config ====
    public static final int MAX_RETRIES = 3;

    // ==== Logging Messages ====
    public static final String LOG_PROCESSING_FILE = "Processing file: {}";
    public static final String LOG_SEND_ATTEMPT = "Attempt {}/{} to send JSON to AML";
    public static final String LOG_SEND_FAIL = "Send attempt {} failed for file {}";
    public static final String LOG_SEND_SUCCESS = "Successfully sent file {} to AML";
    public static final String LOG_ARCHIVE_SUCCESS = "File archived successfully: {}";
    public static final String LOG_ARCHIVE_FAIL = "Archiving failed for file {}";
    public static final String LOG_ERROR_MOVING = "Failed to move file {} to error folder.";
    public static final String LOG_ALL_ATTEMPTS_FAILED = "All {} attempts failed for file {}. Moving to error folder.";

    // ==== Misc ====
    public static final int JSON_PREVIEW_LENGTH = 200;

    private Constants() {
        // Prevent instantiation
    }
}
