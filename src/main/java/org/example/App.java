package org.example;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.InputStreamReader;

public class App extends Application {

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent?key=AIzaSyCaEwqkwm9wbJrE-yxHn6OjNIL6_X7eEHk"; // Replace with your API key
    private TextArea responseArea;
    private TextField inputField;
    private Button sendButton;
    private Button saveButton;

    @Override
    public void start(Stage primaryStage) {
        inputField = new TextField();
        responseArea = new TextArea();
        responseArea.setEditable(false);
        responseArea.setWrapText(true);

        sendButton = new Button("Send");
        saveButton = new Button("Save Chat");

        VBox layout = new VBox(10, responseArea, inputField, sendButton, saveButton);
        layout.setPadding(new Insets(15));
        layout.setStyle("-fx-background-color: #f0f4f8;");

        // Handle Enter key event to send messages
        inputField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                sendButton.fire();
            }
        });

        // Send button action
        sendButton.setOnAction(e -> sendMessage());

        // Save button action
        saveButton.setOnAction(e -> saveChat());

        // Set up scene and stage
        Scene scene = new Scene(layout, 400, 350);
        primaryStage.setTitle("Saigpt Chatbot");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void sendMessage() {
        String userInput = inputField.getText().trim();
        if (!userInput.isEmpty()) {
            responseArea.appendText("You: " + userInput + "\n");
            inputField.clear();

            // Show loading indicator while waiting for response
            responseArea.appendText("Sai: Thinking...\n");

            // Fetch response from Gemini API asynchronously
            new Thread(() -> {
                String response = fetchGeminiResponse(userInput);

                // Add the response to the UI with a separator line
                responseArea.appendText("SaiGPT: " + response + "\n");
                responseArea.appendText("====================================\n");
                responseArea.setScrollTop(Double.MAX_VALUE); // Auto-scroll to the bottom
            }).start();
        }
    }

    private String fetchGeminiResponse(String input) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(GEMINI_API_URL);
            post.setHeader("Content-Type", "application/json");

            // Prepare the JSON payload
            JSONObject json = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject part = new JSONObject();
            part.put("text", input);
            JSONArray parts = new JSONArray();
            parts.put(part);
            JSONObject contentItem = new JSONObject();
            contentItem.put("parts", parts);
            contents.put(contentItem);
            json.put("contents", contents);

            StringEntity entity = new StringEntity(json.toString());
            post.setEntity(entity);

            try (CloseableHttpResponse response = httpClient.execute(post)) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                // Parse and return the response text
                return parseResponse(result.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    private String parseResponse(String jsonResponse) {
        try {
            JSONObject json = new JSONObject(jsonResponse);
            if (json.has("candidates")) {
                JSONArray candidates = json.getJSONArray("candidates");
                if (candidates.length() > 0) {
                    JSONObject firstCandidate = candidates.getJSONObject(0);
                    JSONObject content = firstCandidate.getJSONObject("content");
                    JSONArray parts = content.getJSONArray("parts");
                    if (parts.length() > 0) {
                        return parts.getJSONObject(0).getString("text");
                    }
                }
            }
            return "No valid response received.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error parsing response: " + e.getMessage();
        }
    }

    private void saveChat() {
        try (FileWriter writer = new FileWriter("ChatHistory.txt", true)) {
            writer.write(responseArea.getText() + "\n");
            writer.write("====================================\n");
            responseArea.appendText("Chat saved to ChatHistory.txt\n");
        } catch (Exception e) {
            e.printStackTrace();
            responseArea.appendText("Error saving chat: " + e.getMessage() + "\n");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
