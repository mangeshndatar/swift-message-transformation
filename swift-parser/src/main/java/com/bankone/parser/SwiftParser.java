package com.bankone.parser;

import com.prowidesoftware.swift.model.SwiftMessage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class SwiftParser {

	public String parse(File swiftFile, String processedDirPath) {
		try {
			String rawMessage = new String(Files.readAllBytes(swiftFile.toPath()));
			SwiftMessage swiftMessage = SwiftMessage.parse(rawMessage);

			if (swiftMessage.getBlock1() == null || swiftMessage.getBlock2() == null
					|| swiftMessage.getType() == null) {
				throw new IllegalArgumentException("Invalid SWIFT message: missing mandatory blocks or message type");
			}

			String type = swiftMessage.getType();
			JSONObject rawJson = new JSONObject(swiftMessage.toJson());
			JSONObject finalJson = new JSONObject();

			finalJson.put("messageType", "MT" + type);
			finalJson.put("version", 2);
			finalJson.put("timestamp", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT));
			finalJson.put("rawJson", rawJson); // optional for prod

			JSONObject data = rawJson.optJSONObject("data");
			if (data != null) {
				JSONObject block4 = data.optJSONObject("block4");
				if (block4 != null) {
					JSONArray tags = block4.optJSONArray("tags");
					if (tags != null) {
						for (int i = 0; i < tags.length(); i++) {
							JSONObject tag = tags.getJSONObject(i);
							String name = tag.optString("name");
							String value = tag.optString("value");

							switch (name) {
							case "20":
								finalJson.put("transactionReferenceNumber", value.trim());
								break;

							case "23B":
								finalJson.put("bankOperationCode", value.trim());
								break;

							case "32A":
								parse32A(value, finalJson);
								break;

							case "50K":
								finalJson.put("orderingCustomer", parseCustomer(value));
								break;

							case "59":
								finalJson.put("beneficiaryCustomer", parseCustomer(value));
								break;

							case "70":
								finalJson.put("remittanceInformation", value.trim());
								break;

							case "71A":
								finalJson.put("chargesDetails", value.trim());
								break;

							case "60F":
								finalJson.put("openingBalance", parseBalance(value));
								break;

							case "62F":
								finalJson.put("closingBalance", parseBalance(value));
								break;
							}
						}
					}
				}
			}

			// Copy to processed folder
			Path processedDir = Paths.get(processedDirPath);
			if (!Files.exists(processedDir)) {
				Files.createDirectories(processedDir);
			}
			Path targetPath = processedDir.resolve(swiftFile.getName());
			Files.copy(swiftFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

			return finalJson.toString(2);

		} catch (Exception e) {
			 throw new RuntimeException("Failed to parse file: " + swiftFile.getName(),
			 e);
		}

	}

	private void parse32A(String value, JSONObject finalJson) {
		try {
			if (value.length() < 9) {
				throw new IllegalArgumentException("Invalid 32A tag format: too short");
			}
			finalJson.put("valueDate", value.substring(0, 6));
			finalJson.put("currency", value.substring(6, 9));

			String amountPart = value.length() > 9 ? value.substring(9).replace(",", ".") : "";

			if (!amountPart.isEmpty()) {
				// Optional: validate amount as numeric
				try {
					Double.parseDouble(amountPart);
					finalJson.put("amount", amountPart);
				} catch (NumberFormatException ex) {
					finalJson.put("amount", JSONObject.NULL);
					finalJson.put("amountParseError", "Invalid numeric format: " + amountPart);
				}
			} else {
				finalJson.put("amount", JSONObject.NULL);
				finalJson.put("amountParseError", "Missing amount in 32A tag");
			}
		} catch (Exception e) {
			finalJson.put("32A_parse_error", "Failed to parse 32A: " + e.getMessage());
		}
	}

	private JSONObject parseBalance(String value) {
		JSONObject balance = new JSONObject();
		try {
			if (value.length() >= 15) {
				balance.put("sign", String.valueOf(value.charAt(0)));
				balance.put("date", value.substring(1, 7));
				balance.put("currency", value.substring(7, 10));
				balance.put("amount", value.substring(10).replace(",", "."));
			} else {
				balance.put("raw", value);
			}
		} catch (Exception e) {
			balance.put("error", "Invalid balance field");
		}
		return balance;
	}

	private JSONObject parseCustomer(String value) {
		JSONObject customer = new JSONObject();
		String[] lines = value.split("\n");
		if (lines.length > 0) {
			if (lines[0].startsWith("/")) {
				customer.put("accountNumber", lines[0].replace("/", "").trim());
				if (lines.length > 1) {
					customer.put("name", lines[1].trim());
				}
			} else {
				customer.put("name", lines[0].trim());
			}
		}
		return customer;
	}

	// getTransaction by id	
	public static String getTransactionId(File file) throws IOException {
		String transactionId = null;
        String rawMessage = new String(Files.readAllBytes(file.toPath()));
        SwiftMessage swiftMessage = SwiftMessage.parse(rawMessage);
    				JSONObject rawJson = new JSONObject(swiftMessage.toJson());
    				JSONObject data = rawJson.optJSONObject("data");
    				if (data != null) {
    					JSONObject block4 = data.optJSONObject("block4");
    					if (block4 != null) {
    						JSONArray tags = block4.optJSONArray("tags");
    						if (tags != null) {
    							for (int i = 0; i < tags.length(); i++) {
    								JSONObject tag = tags.getJSONObject(i);
    								String name = tag.optString("name");
    								String value = tag.optString("value");
    								if(name.equalsIgnoreCase("20")) {
    									transactionId = value;
    								}
    							}
    						}
    					}
    				}
    				return transactionId;
	}
}
