package com.pipelinepilot.settings

/**
 * Known AI providers. All speak the OpenAI Chat Completions wire format, so only
 * the base URL (and whether a key is needed) differs. Selecting one in settings
 * auto-fills the base URL + a suggested model — both remain editable.
 */
enum class AiProvider(
    val id: String,
    val displayName: String,
    val defaultBaseUrl: String,
    val defaultModel: String,
    val keyRequired: Boolean,
) {
    OPENAI("openai", "OpenAI", "https://api.openai.com/v1", "gpt-4o-mini", true),
    OPENROUTER("openrouter", "OpenRouter", "https://openrouter.ai/api/v1", "openai/gpt-4o-mini", true),
    OLLAMA("ollama", "Local — Ollama", "http://localhost:11434/v1", "llama3.1", false),
    LMSTUDIO("lmstudio", "Local — LM Studio", "http://localhost:1234/v1", "local-model", false),
    // Any OpenAI-compatible gateway: LiteLLM, vLLM, LocalAI, a corporate proxy, etc.
    // (opencode itself is an agent, not an OpenAI /chat/completions server — point at
    //  its underlying provider or a LiteLLM proxy here.)
    CUSTOM("custom", "Custom / OpenAI-compatible gateway", "", "", false);

    companion object {
        fun fromId(id: String?): AiProvider = entries.firstOrNull { it.id == id } ?: OPENAI
        fun fromDisplayName(name: String?): AiProvider =
            entries.firstOrNull { it.displayName == name } ?: OPENAI
    }
}
