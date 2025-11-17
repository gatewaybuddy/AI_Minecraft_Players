package com.aiplayer.llm;

import java.util.ArrayList;
import java.util.List;

/**
 * LLM generation options.
 *
 * Controls how the LLM generates text:
 * - temperature: randomness (0.0 = deterministic, 1.0+ = creative)
 * - maxTokens: maximum response length
 * - topP: nucleus sampling parameter
 * - stopSequences: strings that stop generation
 * - systemPrompt: system message (role/context)
 */
public class LLMOptions {

    private double temperature = 0.7;
    private int maxTokens = 1000;
    private double topP = 1.0;
    private List<String> stopSequences = new ArrayList<>();
    private String systemPrompt = null;

    public LLMOptions() {
    }

    // Builder pattern

    public LLMOptions temperature(double temperature) {
        this.temperature = temperature;
        return this;
    }

    public LLMOptions maxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
        return this;
    }

    public LLMOptions topP(double topP) {
        this.topP = topP;
        return this;
    }

    public LLMOptions stopSequences(List<String> stopSequences) {
        this.stopSequences = new ArrayList<>(stopSequences);
        return this;
    }

    public LLMOptions systemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
        return this;
    }

    // Getters

    public double getTemperature() {
        return temperature;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public double getTopP() {
        return topP;
    }

    public List<String> getStopSequences() {
        return new ArrayList<>(stopSequences);
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    // Presets

    public static LLMOptions planning() {
        return new LLMOptions()
            .temperature(0.7)
            .maxTokens(1500);
    }

    public static LLMOptions chat() {
        return new LLMOptions()
            .temperature(0.8)
            .maxTokens(500);
    }

    public static LLMOptions deterministic() {
        return new LLMOptions()
            .temperature(0.0)
            .maxTokens(1000);
    }
}
