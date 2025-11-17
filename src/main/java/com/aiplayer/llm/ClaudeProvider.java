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
 * Anthropic Claude Provider - Claude 3.5 Sonnet and Claude 3 Haiku integration.
 *
 * Supports:
 * - Claude 3.5 Sonnet (claude-3-5-sonnet-20240620) - Most capable
 * - Claude 3 Haiku (claude-3-haiku-20240307) - Fastest, cheapest
 * - Claude 3 Opus (claude-3-opus-20240229) - Previous flagship
 *
 * API Docs: https://docs.anthropic.com/claude/reference/messages_post
 */
public class ClaudeProvider implements LLMProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClaudeProvider.class);
    private static final String API_BASE_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final String apiKey;
    private final String modelName;
    private final Gson gson;

    public ClaudeProvider(String apiKey, String modelName) {
        this.apiKey = apiKey;
        this.modelName = modelName != null ? modelName : "claude-3-5-sonnet-20240620";
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
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", ANTHROPIC_VERSION)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody.toString(), JSON))
                    .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "No error details";
                        LOGGER.error("Claude API error: {} - {}", response.code(), errorBody);
                        throw new RuntimeException("Claude API error: " + response.code());
                    }

                    String responseBody = response.body().string();
                    JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

                    return jsonResponse
                        .getAsJsonArray("content")
                        .get(0)
                        .getAsJsonObject()
                        .get("text")
                        .getAsString()
                        .trim();
                }
            } catch (IOException e) {
                LOGGER.error("Failed to complete Claude request", e);
                throw new RuntimeException("Claude request failed", e);
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
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", ANTHROPIC_VERSION)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(testBody.toString(), JSON))
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            LOGGER.warn("Claude availability check failed", e);
            return false;
        }
    }

    @Override
    public String getModelName() {
        return modelName;
    }

    @Override
    public String getProviderName() {
        return "Anthropic Claude";
    }

    /**
     * Build Claude messages request body.
     */
    private JsonObject buildRequestBody(String prompt, LLMOptions options) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", modelName);
        requestBody.addProperty("max_tokens", options.getMaxTokens());

        // System prompt (if provided)
        if (options.getSystemPrompt() != null && !options.getSystemPrompt().isEmpty()) {
            requestBody.addProperty("system", options.getSystemPrompt());
        }

        // Build messages array (user message)
        JsonArray messages = new JsonArray();
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", prompt);
        messages.add(userMessage);
        requestBody.add("messages", messages);

        // Generation parameters
        requestBody.addProperty("temperature", options.getTemperature());
        requestBody.addProperty("top_p", options.getTopP());

        // Stop sequences
        if (!options.getStopSequences().isEmpty()) {
            JsonArray stopArray = new JsonArray();
            for (String stop : options.getStopSequences()) {
                stopArray.add(stop);
            }
            requestBody.add("stop_sequences", stopArray);
        }

        return requestBody;
    }

    /**
     * Create Claude provider from config.
     */
    public static ClaudeProvider fromConfig(String apiKey, String model) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Claude API key is required");
        }
        return new ClaudeProvider(apiKey, model);
    }
}
