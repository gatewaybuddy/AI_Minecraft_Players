package com.aiplayer.llm;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * LLM Provider interface - Abstract interface for language model providers.
 *
 * Supports multiple backends:
 * - OpenAI (GPT-4, GPT-3.5)
 * - Anthropic (Claude 3.5 Sonnet, Claude 3 Haiku)
 * - Local models (via Ollama - Mistral, LLaMA, etc.)
 *
 * All methods return CompletableFuture for async execution.
 */
public interface LLMProvider {

    /**
     * Complete a prompt (single-turn).
     *
     * @param prompt The input prompt
     * @param options Generation options
     * @return Future containing the completion
     */
    CompletableFuture<String> complete(String prompt, LLMOptions options);

    /**
     * Complete multiple prompts in batch.
     *
     * @param prompts List of prompts
     * @param options Generation options
     * @return Future containing list of completions
     */
    CompletableFuture<List<String>> completeBatch(List<String> prompts, LLMOptions options);

    /**
     * Check if provider is available and configured.
     */
    boolean isAvailable();

    /**
     * Get model name/identifier.
     */
    String getModelName();

    /**
     * Get provider name.
     */
    String getProviderName();
}
