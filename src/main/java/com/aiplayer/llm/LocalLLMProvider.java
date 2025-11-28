package com.aiplayer.llm;

import com.google.gson.Gson;
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
 * Local LLM Provider - Ollama integration for local models.
 *
 * Supports any model available in Ollama:
 * - Mistral 7B (mistral)
 * - LLaMA 2 (llama2)
 * - CodeLlama (codellama)
 * - Phi-2 (phi)
 * - And many more...
 *
 * Ollama must be running locally on http://localhost:11434
 *
 * Docs: https://github.com/ollama/ollama/blob/main/docs/api.md
 */
public class LocalLLMProvider implements LLMProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalLLMProvider.class);
    private static final String DEFAULT_BASE_URL = "http://localhost:11434";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final String baseUrl;
    private final String modelName;
    private final Gson gson;

    public LocalLLMProvider(String modelName) {
        this(DEFAULT_BASE_URL, modelName);
    }

    public LocalLLMProvider(String baseUrl, String modelName) {
        this.baseUrl = baseUrl;
        this.modelName = modelName != null ? modelName : "mistral";
        this.gson = new Gson();
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS) // Local models can be slow
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    }

    @Override
    public CompletableFuture<String> complete(String prompt, LLMOptions options) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject requestBody = buildRequestBody(prompt, options);
                String apiUrl = baseUrl + "/api/generate";

                Request request = new Request.Builder()
                    .url(apiUrl)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody.toString(), JSON))
                    .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "No error details";
                        LOGGER.error("Ollama API error: {} - {}", response.code(), errorBody);
                        throw new RuntimeException("Ollama API error: " + response.code());
                    }

                    // Ollama streams JSON objects line by line
                    // We need to collect all response chunks
                    StringBuilder fullResponse = new StringBuilder();
                    String responseBody = response.body().string();
                    String[] lines = responseBody.split("\n");

                    for (String line : lines) {
                        if (line.trim().isEmpty()) continue;

                        JsonObject chunk = gson.fromJson(line, JsonObject.class);
                        if (chunk.has("response")) {
                            fullResponse.append(chunk.get("response").getAsString());
                        }

                        // Check if done
                        if (chunk.has("done") && chunk.get("done").getAsBoolean()) {
                            break;
                        }
                    }

                    return fullResponse.toString().trim();
                }
            } catch (IOException e) {
                LOGGER.error("Failed to complete Ollama request", e);
                throw new RuntimeException("Ollama request failed", e);
            }
        });
    }

    @Override
    public CompletableFuture<List<String>> completeBatch(List<String> prompts, LLMOptions options) {
        // Execute prompts sequentially to avoid overwhelming local GPU
        // (Ollama doesn't support true batch processing)
        return CompletableFuture.supplyAsync(() -> {
            List<String> results = new ArrayList<>();
            for (String prompt : prompts) {
                try {
                    String result = complete(prompt, options).get();
                    results.add(result);
                } catch (Exception e) {
                    LOGGER.error("Failed to complete batch item", e);
                    results.add(""); // Add empty result on failure
                }
            }
            return results;
        });
    }

    @Override
    public boolean isAvailable() {
        try {
            // Check if Ollama is running by hitting the tags endpoint
            String apiUrl = baseUrl + "/api/tags";
            Request request = new Request.Builder()
                .url(apiUrl)
                .get()
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return false;
                }

                // Check if model is available
                String responseBody = response.body().string();
                JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

                if (!jsonResponse.has("models")) {
                    return false;
                }

                // Check if our model is in the list
                return jsonResponse.getAsJsonArray("models")
                    .toString()
                    .contains(modelName);
            }
        } catch (Exception e) {
            LOGGER.warn("Ollama availability check failed", e);
            return false;
        }
    }

    @Override
    public String getModelName() {
        return modelName;
    }

    @Override
    public String getProviderName() {
        return "Ollama (Local)";
    }

    /**
     * Build Ollama generate request body.
     */
    private JsonObject buildRequestBody(String prompt, LLMOptions options) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", modelName);
        requestBody.addProperty("stream", true); // Ollama uses streaming by default

        // Build full prompt with system message
        String fullPrompt = prompt;
        if (options.getSystemPrompt() != null && !options.getSystemPrompt().isEmpty()) {
            fullPrompt = options.getSystemPrompt() + "\n\n" + prompt;
        }
        requestBody.addProperty("prompt", fullPrompt);

        // Generation options
        JsonObject optionsObj = new JsonObject();
        optionsObj.addProperty("temperature", options.getTemperature());
        optionsObj.addProperty("top_p", options.getTopP());
        optionsObj.addProperty("num_predict", options.getMaxTokens());

        // Stop sequences
        if (!options.getStopSequences().isEmpty()) {
            // Ollama expects stop as an array in options
            StringBuilder stopBuilder = new StringBuilder();
            for (int i = 0; i < options.getStopSequences().size(); i++) {
                if (i > 0) stopBuilder.append(",");
                stopBuilder.append(options.getStopSequences().get(i));
            }
            optionsObj.addProperty("stop", stopBuilder.toString());
        }

        requestBody.add("options", optionsObj);

        return requestBody;
    }

    /**
     * Create local LLM provider from config.
     */
    public static LocalLLMProvider fromConfig(String baseUrl, String model) {
        if (model == null || model.trim().isEmpty()) {
            throw new IllegalArgumentException("Model name is required for local LLM");
        }

        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            return new LocalLLMProvider(model);
        }

        return new LocalLLMProvider(baseUrl, model);
    }

    /**
     * Pull a model from Ollama (if not already available).
     */
    public CompletableFuture<Boolean> pullModel() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject requestBody = new JsonObject();
                requestBody.addProperty("name", modelName);

                String apiUrl = baseUrl + "/api/pull";
                Request request = new Request.Builder()
                    .url(apiUrl)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody.toString(), JSON))
                    .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    return response.isSuccessful();
                }
            } catch (Exception e) {
                LOGGER.error("Failed to pull model {}", modelName, e);
                return false;
            }
        });
    }
}
