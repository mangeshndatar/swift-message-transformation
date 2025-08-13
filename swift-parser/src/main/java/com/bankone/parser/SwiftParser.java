package com.bankone.parser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.prowidesoftware.swift.model.SwiftMessage;
import com.prowidesoftware.swift.model.Tag;
import com.prowidesoftware.swift.model.SwiftBlock4;
import org.json.JSONArray;
import org.json.JSONObject;

public class SwiftParser {

    public String parse(String rawMessage) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JSONObject result = new JSONObject();

        try {
            // Parse the raw message
            SwiftMessage swiftMessage = SwiftMessage.parse(rawMessage);

            // Get message type
            String type = swiftMessage.getType();
            result.put("messageType", "MT" + type);

            // Extract headers
            JSONObject headers = new JSONObject();
            if (swiftMessage.getBlock1() != null) {
                headers.put("block1", swiftMessage.getBlock1().toJson());
            }
            if (swiftMessage.getBlock2() != null) {
                headers.put("block2", swiftMessage.getBlock2().toJson());
            }
            if (swiftMessage.getBlock3() != null) {
                headers.put("block3", swiftMessage.getBlock3().toJson());
            }
            if (swiftMessage.getBlock5() != null) {
                headers.put("block5", swiftMessage.getBlock5().toJson());
            }
            result.put("headers", headers);

            // Extract fields from Block 4 using Tag objects
            SwiftBlock4 block4 = swiftMessage.getBlock4();
            JSONArray formattedFields = new JSONArray();

            if (block4 != null && block4.getTags() != null) {
                for (Tag tag : block4.getTags()) {
                    JSONObject fieldObj = new JSONObject();
                    fieldObj.put("tag", tag.getName());
                    fieldObj.put("value", tag.getValue());
                    formattedFields.put(fieldObj);
                }
            }

            result.put("fields", formattedFields);

            return gson.toJson(result);

        } catch (Exception e) {
            return new JSONObject().put("error", e.getMessage()).toString(2);
        }
    }
}
