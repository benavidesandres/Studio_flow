package com.studioflow.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studioflow.dto.ai.ScriptBreakdownResponse;
import com.studioflow.dto.ai.ScriptBreakdownResponse.BreakdownScene;
import com.studioflow.exception.AiServiceException;
import com.studioflow.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.*;

/**
 * Desglosa un guion en escenas, personajes y locaciones con un modelo de Groq (API compatible con OpenAI).
 * La respuesta se pide con un JSON Schema estricto y además se sanea aquí: el cliente nunca recibe
 * escenas sin número ni locación, nombres escritos de dos formas o duraciones absurdas.
 */
@Service
@Slf4j
public class ScriptBreakdownService {

    static final int DEFAULT_MINUTES = 60;
    static final int MIN_MINUTES = 5;
    static final int MAX_MINUTES = 480;

    private static final List<String> SETTINGS = List.of("INT", "EXT", "INT/EXT");
    private static final List<String> TIMES_OF_DAY = List.of("DÍA", "NOCHE", "AMANECER", "ATARDECER", "CONTINUO");
    private static final Set<String> CONNECTORS = Set.of(
            "de", "del", "la", "las", "el", "los", "y", "e", "o", "u", "a", "al", "en", "con", "por", "para", "sin",
            "of", "the", "and", "in", "on", "at", "to");

    private static final String SYSTEM_PROMPT = """
            Eres un asistente de dirección de producción audiovisual. Haces el desglose de un guion: \
            lo divides en escenas para poder programar el rodaje.

            Reglas:
            - Cada encabezado de escena (por ejemplo "INT. COCINA - DÍA" o "EXT. PARQUE - NOCHE") abre una escena nueva. \
            Si el texto no tiene encabezados, divide donde cambie el lugar o el momento.
            - sceneNumber: el número que trae el guion; si no trae, numera de forma consecutiva desde 1.
            - location: solo el nombre del lugar, sin INT/EXT ni el momento del día, con mayúscula solo al inicio \
            y en nombres propios (por ejemplo "Cocina de Ana", no "COCINA DE ANA"). El mismo lugar se escribe \
            siempre igual en todas las escenas.
            - setting: INT, EXT o INT/EXT. timeOfDay: DÍA, NOCHE, AMANECER, ATARDECER o CONTINUO \
            (usa DÍA si no se indica).
            - characters: los personajes con nombre que aparecen o hablan en la escena, escritos siempre igual \
            y con mayúscula inicial (por ejemplo "Ana", no "ANA"). No incluyas extras sin nombre.
            - title: una descripción breve de lo que pasa (máximo 8 palabras), en el idioma del guion.
            - summary: una sola frase con la acción de la escena.
            - estimatedMinutes: tiempo de rodaje estimado. Como referencia, una página de guion son unos \
            60 minutos de rodaje; nunca menos de 15.
            - characters (primer nivel) lista todos los personajes y locations todas las locaciones, sin repetir.
            - No inventes escenas, personajes ni lugares que no estén en el texto.
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final int maxScriptChars;

    public ScriptBreakdownService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${studioflow.ai.groq-api-key:}") String apiKey,
            @Value("${studioflow.ai.groq-model:openai/gpt-oss-120b}") String model,
            @Value("${studioflow.ai.groq-base-url:https://api.groq.com/openai/v1}") String baseUrl,
            @Value("${studioflow.ai.max-script-chars:20000}") int maxScriptChars,
            @Value("${studioflow.ai.timeout-seconds:90}") int timeoutSeconds
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        this.restClient = restClientBuilder.baseUrl(baseUrl).requestFactory(requestFactory).build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.maxScriptChars = maxScriptChars;
    }

    public ScriptBreakdownResponse breakdown(String script) {
        String text = script == null ? "" : script.strip();
        if (text.isEmpty()) {
            throw new BadRequestException("El guion no puede estar vacío");
        }
        if (text.length() > maxScriptChars) {
            throw new BadRequestException(String.format(
                    "El guion tiene %,d caracteres y el máximo es %,d. Divídelo e impórtalo por partes.",
                    text.length(), maxScriptChars));
        }
        if (apiKey.isEmpty()) {
            throw new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                    "La IA no está configurada en el servidor: falta la variable GROQ_API_KEY.");
        }

        long started = System.currentTimeMillis();
        ScriptBreakdownResponse result = normalize(parseContent(callGroq(text)));
        log.info("Guion desglosado: {} caracteres, {} escenas, {} ms",
                text.length(), result.scenes().size(), System.currentTimeMillis() - started);
        return result;
    }

    private JsonNode callGroq(String script) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", "Guion:\n\n" + script)
        ));
        body.put("temperature", 0.2);
        body.put("reasoning_effort", "medium");
        body.put("include_reasoning", false);
        body.put("max_completion_tokens", 8192);
        body.put("response_format", Map.of(
                "type", "json_schema",
                "json_schema", Map.of("name", "script_breakdown", "strict", true, "schema", responseSchema())
        ));

        try {
            return restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException ex) {
            throw translate(ex);
        } catch (ResourceAccessException ex) {
            throw new AiServiceException(HttpStatus.GATEWAY_TIMEOUT,
                    "La IA tardó demasiado en responder. Inténtalo de nuevo o importa el guion por partes.", ex);
        }
    }

    private AiServiceException translate(RestClientResponseException ex) {
        int status = ex.getStatusCode().value();
        log.warn("Groq respondió {}: {}", status, ex.getResponseBodyAsString());
        return switch (status) {
            case 401, 403 -> new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                    "La clave de Groq del servidor no es válida o no tiene acceso al modelo.", ex);
            case 404 -> new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                    "El modelo de IA configurado (" + model + ") no existe en Groq.", ex);
            case 413 -> new AiServiceException(HttpStatus.BAD_REQUEST,
                    "El guion es demasiado largo para el plan actual de Groq. Impórtalo por partes.", ex);
            case 429 -> new AiServiceException(HttpStatus.TOO_MANY_REQUESTS,
                    "Se alcanzó el límite de uso de la IA. Espera un minuto y vuelve a intentarlo.", ex);
            default -> new AiServiceException(HttpStatus.BAD_GATEWAY,
                    "La IA no pudo procesar el guion. Vuelve a intentarlo en unos segundos.", ex);
        };
    }

    /** Extrae el JSON del mensaje del modelo. */
    ScriptBreakdownResponse parseContent(JsonNode completion) {
        JsonNode choice = completion == null ? null : completion.path("choices").path(0);
        if (choice == null || choice.isMissingNode()) {
            throw new AiServiceException(HttpStatus.BAD_GATEWAY, "La IA devolvió una respuesta vacía.");
        }
        if ("length".equals(choice.path("finish_reason").asText())) {
            throw new AiServiceException(HttpStatus.BAD_GATEWAY,
                    "El guion es demasiado largo para analizarlo de una vez. Impórtalo por partes.");
        }
        String content = choice.path("message").path("content").asText("");
        try {
            return objectMapper.readValue(content, ScriptBreakdownResponse.class);
        } catch (JsonProcessingException ex) {
            log.warn("La respuesta de la IA no es JSON válido: {}", content);
            throw new AiServiceException(HttpStatus.BAD_GATEWAY,
                    "La IA devolvió un formato inesperado. Vuelve a intentarlo.", ex);
        }
    }

    /**
     * Deja el desglose listo para programar: números y locaciones siempre presentes y únicos, nombres
     * escritos de una sola forma (el modelo a veces mezcla "ANA" y "Ana") y duraciones en un rango razonable.
     */
    ScriptBreakdownResponse normalize(ScriptBreakdownResponse raw) {
        List<BreakdownScene> source = raw == null || raw.scenes() == null ? List.of() : raw.scenes();
        Canon characters = new Canon();
        Canon locations = new Canon();
        Set<String> usedNumbers = new HashSet<>();
        List<BreakdownScene> scenes = new ArrayList<>();

        for (BreakdownScene scene : source) {
            if (scene == null) continue;
            String rawLocation = clean(scene.location(), 255);
            String title = clean(scene.title(), 255);
            if (rawLocation == null && title == null) continue;
            String location = locations.add(rawLocation != null ? rawLocation : "Sin especificar");
            if (title == null) title = location;

            String number = clean(scene.sceneNumber(), 50);
            if (number == null || !usedNumbers.add(number.toLowerCase(Locale.ROOT))) {
                number = nextFreeNumber(usedNumbers, scenes.size() + 1);
            }

            List<String> cast = new ArrayList<>();
            for (String name : scene.characters() == null ? List.<String>of() : scene.characters()) {
                String canonical = characters.add(clean(name, 120));
                if (canonical != null && !cast.contains(canonical)) cast.add(canonical);
            }

            scenes.add(new BreakdownScene(
                    number,
                    title,
                    location,
                    pick(scene.setting(), SETTINGS, "INT"),
                    pick(scene.timeOfDay(), TIMES_OF_DAY, "DÍA"),
                    List.copyOf(cast),
                    clampMinutes(scene.estimatedMinutes()),
                    clean(scene.summary(), 1000)
            ));
        }

        if (scenes.isEmpty()) {
            throw new AiServiceException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "No se encontraron escenas en el texto. Pega un guion con encabezados como «INT. COCINA - DÍA».");
        }
        // Solo se devuelven personajes y locaciones que aparecen en alguna escena: son los que se pueden programar.
        return new ScriptBreakdownResponse(List.copyOf(scenes), characters.values(), locations.values());
    }

    private static String nextFreeNumber(Set<String> used, int start) {
        int candidate = start;
        while (!used.add(String.valueOf(candidate))) candidate++;
        return String.valueOf(candidate);
    }

    private static int clampMinutes(Integer minutes) {
        if (minutes == null || minutes <= 0) return DEFAULT_MINUTES;
        return Math.max(MIN_MINUTES, Math.min(MAX_MINUTES, minutes));
    }

    private static String pick(String value, List<String> allowed, String fallback) {
        if (value == null) return fallback;
        String upper = value.strip().toUpperCase(Locale.ROOT).replace(".", "").replaceAll("\\s*/\\s*", "/");
        if (upper.equals("DIA")) upper = "DÍA";
        if (upper.equals("I/E")) upper = "INT/EXT";
        return allowed.contains(upper) ? upper : fallback;
    }

    private static String clean(String value, int maxLength) {
        if (value == null) return null;
        String trimmed = value.strip().replaceAll("\\s+", " ");
        if (trimmed.isEmpty()) return null;
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength).strip() : trimmed;
    }

    /** Agrupa variantes de mayúsculas bajo una sola forma; los nombres en mayúsculas pasan a "Título". */
    private static final class Canon {
        private final Map<String, String> byKey = new LinkedHashMap<>();

        String add(String value) {
            if (value == null) return null;
            String key = value.toLowerCase(Locale.ROOT);
            boolean shouting = value.length() > 1 && value.equals(value.toUpperCase(Locale.ROOT));
            return byKey.merge(key, shouting ? titleCase(value) : value, (existing, incoming) -> existing);
        }

        List<String> values() {
            return List.copyOf(byKey.values());
        }

        /** "COCINA DE ANA" → "Cocina de Ana": las palabras de enlace quedan en minúscula salvo al inicio. */
        private static String titleCase(String value) {
            String[] words = value.toLowerCase(Locale.ROOT).split(" ");
            for (int i = 0; i < words.length; i++) {
                String word = words[i];
                if (word.isEmpty() || (i > 0 && CONNECTORS.contains(word))) continue;
                words[i] = Character.toUpperCase(word.charAt(0)) + word.substring(1);
            }
            return String.join(" ", words);
        }
    }

    private static Map<String, Object> responseSchema() {
        Map<String, Object> text = Map.of("type", "string");
        Map<String, Object> textList = Map.of("type", "array", "items", text);

        Map<String, Object> sceneProps = new LinkedHashMap<>();
        sceneProps.put("sceneNumber", text);
        sceneProps.put("title", text);
        sceneProps.put("location", text);
        sceneProps.put("setting", Map.of("type", "string", "enum", SETTINGS));
        sceneProps.put("timeOfDay", Map.of("type", "string", "enum", TIMES_OF_DAY));
        sceneProps.put("characters", textList);
        sceneProps.put("estimatedMinutes", Map.of("type", "integer"));
        sceneProps.put("summary", text);

        Map<String, Object> scene = Map.of(
                "type", "object",
                "properties", sceneProps,
                "required", List.copyOf(sceneProps.keySet()),
                "additionalProperties", false
        );
        Map<String, Object> rootProps = new LinkedHashMap<>();
        rootProps.put("scenes", Map.of("type", "array", "items", scene));
        rootProps.put("characters", textList);
        rootProps.put("locations", textList);
        return Map.of(
                "type", "object",
                "properties", rootProps,
                "required", List.copyOf(rootProps.keySet()),
                "additionalProperties", false
        );
    }
}
