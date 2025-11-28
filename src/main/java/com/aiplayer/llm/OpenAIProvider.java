package com.aiplayer.llm;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * OpenAI Provider - GPT-4 and GPT-3.5 integration.
 *
 * Supports:
 * - GPT-4 (gpt-4, gpt-4-turbo)
 * - GPT-3.5 (gpt-3.5-turbo)
 *
 * API Docs: https://platform.openai.com/docs/api-reference/chat
 */
public class OpenAIProvider implements LLMProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAIProvider.class);
    private static final String API_BASE_URL = "https://api.openai.com/v1/chat/completions";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final String apiKey;
    private final String modelName;
    private final Gson gson;

    public OpenAIProvider(String apiKey, String modelName) {
        this.apiKey = apiKey;
        this.modelName = modelName != null ? modelName : "gpt-4-turbo";
        this.gson = new Gson();
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    }

    @Override
    public CompletableFuture<String> complete(String prompt, LLMOptions options) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject requestBody = buildRequestBody(prompt, options);
                Request request = new Request.Builder()
                    .url(API_BASE_URL)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody.toString(), JSON))
                    .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "No error details";
                        LOGGER.error("OpenAI API error: {} - {}", response.code(), errorBody);
                        throw new RuntimeException("OpenAI API error: " + response.code());
                    }

                    String responseBody = response.body().string();
                    JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

                    return jsonResponse
                        .getAsJsonArray("choices")
                        .get(0)
                        .getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content")
                        .getAsString()
                        .trim();
                }
            } catch (IOException e) {
                LOGGER.error("Failed to complete OpenAI request", e);
                throw new RuntimeException("OpenAI request failed", e);
            }
        });
    }

    @Override
    public CompletableFuture<List<String>> completeBatch(List<String> prompts, LLMOptions options) {
        // Execute prompts in parallel
        List<CompletableFuture<String>> futures = new ArrayList<>();
        for (String prompt : prompts) {
            futures.add(complete(prompt, options));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                List<String> results = new ArrayList<>();
                for (CompletableFuture<String> future : futures) {
                    results.add(future.join());
                }
                return results;
            });
    }

    @Override
    public boolean isAvailable() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return false;
        }

        // Quick test request to verify API key
        try {
            JsonObject testBody = new JsonObject();
            testBody.addProperty("model", modelName);
            testBody.addProperty("max_tokens", 5);

            JsonArray messages = new JsonArray();
            JsonObject message = new JsonObject();
            message.addProperty("role", "user");
            message.addProperty("content", "test");
            messages.add(message);
            testBody.add("messages", messages);

            Request request = new Request.Builder()
                .url(API_BASE_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(testBody.toString(), JSON))
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            LOGGER.warn("OpenAI availability check failed", e);
            return false;
        }
    }

    @Override
    public String getModelName() {
        return modelName;
    }

    @Override
    public String getProviderName() {
        return "OpenAI";
    }

    /**
     * Build OpenAI chat completion request body.
     */
    private JsonObject buildRequestBody(String prompt, LLMOptions options) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", modelName);

        // Build messages array
        JsonArray messages = new JsonArray();

        // System message (if provided)
        if (options.getSystemPrompt() != null && !options.getSystemPrompt().isEmpty()) {
            JsonObject systemMessage = new JsonObject();
            systemMessage.addProperty("role", "system");
            systemMessage.addProperty("content", options.getSystemPrompt());
            messages.add(systemMessage);
        }

        // User message
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", prompt);
        messages.add(userMessage);

        requestBody.add("messages", messages);

        // Generation parameters
        requestBody.addProperty("temperature", options.getTemperature());
        requestBody.addProperty("max_tokens", options.getMaxTokens());
        requestBody.addProperty("top_p", options.getTopP());

        // Stop sequences
        if (!options.getStopSequences().isEmpty()) {
            JsonArray stopArray = new JsonArray();
            for (String stop : options.getStopSequences()) {
                stopArray.add(stop);
            }
            requestBody.add("stop", stopArray);
        }

        return requestBody;
    }

    /**
     * Create OpenAI provider from config.
     */
    public static OpenAIProvider fromConfig(String apiKey, String model) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("OpenAI API key is required");
        }
        return new OpenAIProvider(apiKey, model);
    }
}
