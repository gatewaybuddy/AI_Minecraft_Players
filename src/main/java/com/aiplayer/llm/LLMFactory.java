package com.aiplayer.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LLM Factory - Creates and configures LLM providers.
 *
 * Handles provider selection based on configuration:
 * - OpenAI (GPT-4, GPT-3.5)
 * - Anthropic (Claude 3.5 Sonnet, Claude 3 Haiku)
 * - Local (Ollama - Mistral, LLaMA, etc.)
 *
 * Automatically wraps providers with caching to reduce API costs.
 */
public class LLMFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(LLMFactory.class);

    /**
     * Create LLM provider from config.
     *
     * @param providerType Provider type ("openai", "claude", "local")
     * @param apiKey API key (required for OpenAI and Claude)
     * @param modelName Model name
     * @param baseUrl Base URL (for local providers)
     * @param enableCache Enable response caching
     * @return Configured LLM provider (or null if creation fails)
     */
    public static LLMProvider create(
        String providerType,
        String apiKey,
        String modelName,
        String baseUrl,
        boolean enableCache
    ) {
        if (providerType == null || providerType.trim().isEmpty()) {
            LOGGER.error("Provider type is required");
            return null;
        }

        LLMProvider provider = null;

        try {
            switch (providerType.toLowerCase()) {
                case "openai":
                    provider = createOpenAI(apiKey, modelName);
                    break;

                case "claude":
                case "anthropic":
                    provider = createClaude(apiKey, modelName);
                    break;

                case "local":
                case "ollama":
                    provider = createLocal(baseUrl, modelName);
                    break;

                default:
                    LOGGER.error("Unknown provider type: {}", providerType);
                    return null;
            }

            if (provider == null) {
                return null;
            }

            // Check availability
            if (!provider.isAvailable()) {
                LOGGER.warn("Provider {} is not available (check API key, network, or model)", providerType);
                return null;
            }

            LOGGER.info("Created LLM provider: {} (model: {})", provider.getProviderName(), provider.getModelName());

            // Wrap with cache if enabled
            if (enableCache) {
                LOGGER.info("Enabled response caching for {}", provider.getProviderName());
                return new LLMCache(provider);
            }

            return provider;

        } catch (Exception e) {
            LOGGER.error("Failed to create LLM provider: {}", providerType, e);
            return null;
        }
    }

    /**
     * Create OpenAI provider.
     */
    private static LLMProvider createOpenAI(String apiKey, String modelName) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            LOGGER.error("OpenAI API key is required");
            return null;
        }

        return new OpenAIProvider(apiKey, modelName);
    }

    /**
     * Create Claude provider.
     */
    private static LLMProvider createClaude(String apiKey, String modelName) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            LOGGER.error("Claude API key is required");
            return null;
        }

        return new ClaudeProvider(apiKey, modelName);
    }

    /**
     * Create local LLM provider (Ollama).
     */
    private static LLMProvider createLocal(String baseUrl, String modelName) {
        if (modelName == null || modelName.trim().isEmpty()) {
            LOGGER.error("Model name is required for local LLM");
            return null;
        }

        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            return new LocalLLMProvider(modelName);
        }

        return new LocalLLMProvider(baseUrl, modelName);
    }

    /**
     * Cached LLM provider wrapper.
     */
    private static class CachedLLMProvider extends LLMCache {

        public CachedLLMProvider(LLMProvider provider) {
            super(provider);
        }
    }
}
