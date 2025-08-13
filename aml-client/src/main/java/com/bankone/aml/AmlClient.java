package com.bankone.aml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AmlClient {

    private static final Logger logger = LoggerFactory.getLogger(AmlClient.class);

    public boolean send(String json) {
        if (json == null || json.isEmpty()) {
            logger.error("[AML] JSON input is null or empty. Cannot send to AML system.");
            return false;
        }

        // Mask sensitive data if needed
        String preview = json.length() > 200 ? json.substring(0, 200) + "..." : json;

        try {
            // Simulate sending to AML system
            logger.info("[AML] Sending to AML system: {}", preview);

            // TODO: Implement real sending logic here (HTTP API, JMS, etc.)

            return true; // Return true only if sending was successful
        } catch (Exception e) {
            logger.error("[AML] Failed to send to AML system", e);
            return false;
        }
    }
}