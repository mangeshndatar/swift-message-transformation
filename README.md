SWIFT JSON Transformer - Boilerplate (Regenerated)

This repository is a modular Java boilerplate that demonstrates a SWIFT-to-JSON transformation flow.
It contains PoC stubs to illustrate how the components interact. Replace stubs with production code as required.

To run the JSON transformer PoC:
1. Build:
   mvn clean install
2. Run the json-transformer module:
   mvn -pl json-transformer exec:java -Dexec.mainClass="com.bankone.json.Main" -Dsample.folder=../sample-messages

Sample messages are under folder sample-messages
