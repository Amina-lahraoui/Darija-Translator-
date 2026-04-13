package ma.translator.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

@ApplicationScoped
public class GroqTranslationService {

    private static final String CHAT_COMPLETIONS_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String DEFAULT_MODEL = "llama-3.3-70b-versatile";
    private static final long DEFAULT_MIN_INTERVAL_MS = 2_500;
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    private final Object groqRateLock = new Object();
    private long lastGroqRequestEndMs;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();

    public record TranslationOutcome(String translatedText, String modelUsed) {
    }

    public TranslationOutcome translateToDarija(String sourceText) {
        if (sourceText == null || sourceText.isBlank()) {
            throw new IllegalArgumentException("text must not be empty");
        }
        String apiKey = resolveGroqApiKey();
        String model = resolveGroqModel();

        String prompt = """
                You are a professional translator. Translate the following text into Moroccan Arabic (Darija)
                using Arabic script where appropriate. Preserve meaning, tone, and any names.
                Respond with ONLY the Darija translation, no explanations or English.

                Text to translate:
                """ + sourceText;

        String body = Json.createObjectBuilder()
                .add("model", model)
                .add("temperature", 0.2)
                .add("max_tokens", 4096)
                .add("messages", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("role", "user")
                                .add("content", prompt)))
                .build()
                .toString();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(CHAT_COMPLETIONS_URL))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            waitBeforeGroqCall();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Translation interrupted", e);
        }

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int code = response.statusCode();
            if (code == 429) {
                throw new WebApplicationException(Response.status(429)
                        .entity(Map.of(
                                "error",
                                "Groq rate limit exceeded (HTTP 429). Wait and retry, or see "
                                        + "https://console.groq.com/docs/rate-limits"))
                        .build());
            }
            if (code < 200 || code >= 300) {
                throw new IllegalStateException("Groq API error HTTP " + code + ": " + response.body());
            }
            String text = extractAssistantContent(response.body()).trim();
            return new TranslationOutcome(text, model);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Translation interrupted", e);
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Translation failed: " + e.getMessage(), e);
        } finally {
            markGroqCallFinished();
        }
    }

    private void waitBeforeGroqCall() throws InterruptedException {
        long gapMs = resolveMinIntervalMs();
        synchronized (groqRateLock) {
            long now = System.currentTimeMillis();
            if (lastGroqRequestEndMs > 0) {
                long elapsed = now - lastGroqRequestEndMs;
                if (elapsed < gapMs) {
                    Thread.sleep(gapMs - elapsed);
                }
            }
        }
    }

    private void markGroqCallFinished() {
        synchronized (groqRateLock) {
            lastGroqRequestEndMs = System.currentTimeMillis();
        }
    }

    private static long resolveMinIntervalMs() {
        String env = System.getenv("GROQ_MIN_INTERVAL_MS");
        if (env != null && !env.isBlank()) {
            try {
                long v = Long.parseLong(env.trim());
                return v >= 0 ? v : DEFAULT_MIN_INTERVAL_MS;
            } catch (NumberFormatException ignored) {
                return DEFAULT_MIN_INTERVAL_MS;
            }
        }
        try (InputStream in = GroqTranslationService.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) {
                Properties p = new Properties();
                p.load(in);
                String prop = p.getProperty("groq.min.interval.ms");
                if (prop != null && !prop.isBlank()) {
                    return Math.max(0, Long.parseLong(prop.trim()));
                }
            }
        } catch (NumberFormatException ignored) {
            return DEFAULT_MIN_INTERVAL_MS;
        } catch (Exception ignored) {
        }
        return DEFAULT_MIN_INTERVAL_MS;
    }

    private static String resolveGroqModel() {
        Optional<String> fromEnv = Optional.ofNullable(System.getenv("GROQ_MODEL")).filter(s -> !s.isBlank());
        if (fromEnv.isPresent()) {
            return fromEnv.get().trim();
        }
        String fromSys = Optional.ofNullable(System.getProperty("groq.model")).filter(s -> !s.isBlank()).orElse(null);
        if (fromSys != null) {
            return fromSys.trim();
        }
        try (InputStream in = GroqTranslationService.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) {
                Properties p = new Properties();
                p.load(in);
                String fromFile = p.getProperty("groq.model");
                if (fromFile != null && !fromFile.isBlank()) {
                    return fromFile.trim();
                }
            }
        } catch (Exception ignored) {
        }
        return DEFAULT_MODEL;
    }

    private static String resolveGroqApiKey() {
        Optional<String> fromEnv = Optional.ofNullable(System.getenv("GROQ_API_KEY")).filter(s -> !s.isBlank());
        if (fromEnv.isPresent()) {
            return fromEnv.get();
        }
        Properties props = new Properties();
        try (InputStream in = GroqTranslationService.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not load config.properties", e);
        }
        String fromFile = props.getProperty("groq.api.key");
        if (fromFile != null && !fromFile.isBlank()) {
            return fromFile.trim();
        }
        String fromSys = Optional.ofNullable(System.getProperty("groq.api.key")).filter(s -> !s.isBlank()).orElse(null);
        if (fromSys != null) {
            return fromSys;
        }
        throw new IllegalStateException(
                "Set Groq API key: groq.api.key in config.properties or GROQ_API_KEY (https://console.groq.com/keys)");
    }

    private static String extractAssistantContent(String jsonBody) {
        try (JsonReader reader = Json.createReader(new StringReader(jsonBody))) {
            JsonObject root = reader.readObject();
            if (root.containsKey("error")) {
                JsonObject err = root.getJsonObject("error");
                String msg = err != null && err.containsKey("message")
                        ? err.getString("message")
                        : root.toString();
                throw new IllegalStateException(msg);
            }
            return root.getJsonArray("choices").stream()
                    .findFirst()
                    .map(JsonValue::asJsonObject)
                    .map(c -> c.getJsonObject("message"))
                    .map(m -> m.getString("content"))
                    .orElseThrow(() -> new IllegalStateException("Unexpected Groq response shape"));
        }
    }
}
