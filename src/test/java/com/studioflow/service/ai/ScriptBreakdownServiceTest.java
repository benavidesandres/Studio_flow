package com.studioflow.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studioflow.dto.ai.ScriptBreakdownResponse;
import com.studioflow.dto.ai.ScriptBreakdownResponse.BreakdownScene;
import com.studioflow.exception.AiServiceException;
import com.studioflow.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScriptBreakdownServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private ScriptBreakdownService service(String apiKey) {
        return new ScriptBreakdownService(RestClient.builder(), mapper, apiKey, "test-model", "http://localhost:1", 100, 5);
    }

    private static BreakdownScene scene(String number, String title, String location, String setting,
                                        String time, List<String> cast, Integer minutes) {
        return new BreakdownScene(number, title, location, setting, time, cast, minutes, "Resumen");
    }

    @Test
    void normalizeUnifiesNamesNumbersAndDurations() {
        ScriptBreakdownResponse raw = new ScriptBreakdownResponse(List.of(
                scene("1", "Desayuno tenso", "COCINA DE ANA", "int", "dia", List.of("ANA", "Luis"), 45),
                scene("1", "  ", "cocina de ana", "I/E", "noche", List.of("Ana", "  "), 0),
                scene(null, "Huida", "  ", "EXT", "OCASO", List.of("luis"), 2000)
        ), List.of("Ana", "Luis", "Marta"), List.of());

        ScriptBreakdownResponse result = service("key").normalize(raw);

        assertThat(result.scenes()).extracting(BreakdownScene::sceneNumber).containsExactly("1", "2", "3");
        assertThat(result.scenes()).extracting(BreakdownScene::location).containsExactly("Cocina de Ana", "Cocina de Ana", "Sin especificar");
        assertThat(result.scenes()).extracting(BreakdownScene::title).containsExactly("Desayuno tenso", "Cocina de Ana", "Huida");
        assertThat(result.scenes()).extracting(BreakdownScene::setting).containsExactly("INT", "INT/EXT", "EXT");
        assertThat(result.scenes()).extracting(BreakdownScene::timeOfDay).containsExactly("DÍA", "NOCHE", "DÍA");
        assertThat(result.scenes()).extracting(BreakdownScene::estimatedMinutes).containsExactly(45, 60, 480);
        assertThat(result.scenes().get(0).characters()).containsExactly("Ana", "Luis");
        assertThat(result.scenes().get(1).characters()).containsExactly("Ana");
        // Marta no sale en ninguna escena, así que no se puede programar.
        assertThat(result.characters()).containsExactly("Ana", "Luis");
        assertThat(result.locations()).containsExactly("Cocina de Ana", "Sin especificar");
    }

    @Test
    void normalizeRejectsABreakdownWithoutScenes() {
        ScriptBreakdownResponse empty = new ScriptBreakdownResponse(
                Arrays.asList(null, scene(null, " ", null, null, null, null, null)), List.of(), List.of());

        assertThatThrownBy(() -> service("key").normalize(empty))
                .isInstanceOf(AiServiceException.class)
                .extracting("status").isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void parseContentReadsTheModelMessage() throws Exception {
        JsonNode completion = mapper.readTree("""
                {"choices":[{"finish_reason":"stop","message":{"content":
                "{\\"scenes\\":[{\\"sceneNumber\\":\\"4\\",\\"title\\":\\"Llamada\\",\\"location\\":\\"Oficina\\",\\"setting\\":\\"INT\\",\\"timeOfDay\\":\\"NOCHE\\",\\"characters\\":[\\"Ana\\"],\\"estimatedMinutes\\":30,\\"summary\\":\\"Ana llama.\\"}],\\"characters\\":[\\"Ana\\"],\\"locations\\":[\\"Oficina\\"]}"}}]}
                """);

        ScriptBreakdownResponse result = service("key").parseContent(completion);

        assertThat(result.scenes()).singleElement().satisfies(s -> {
            assertThat(s.sceneNumber()).isEqualTo("4");
            assertThat(s.estimatedMinutes()).isEqualTo(30);
        });
    }

    @Test
    void parseContentReportsATruncatedAnswer() throws Exception {
        JsonNode completion = mapper.readTree("{\"choices\":[{\"finish_reason\":\"length\",\"message\":{\"content\":\"{\\\"sce\"}}]}");

        assertThatThrownBy(() -> service("key").parseContent(completion))
                .isInstanceOf(AiServiceException.class)
                .hasMessageContaining("por partes");
    }

    @Test
    void breakdownValidatesInputBeforeCallingGroq() {
        assertThatThrownBy(() -> service("key").breakdown("   "))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service("key").breakdown("x".repeat(101)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("máximo");
        assertThatThrownBy(() -> service("").breakdown("INT. COCINA - DÍA"))
                .isInstanceOf(AiServiceException.class)
                .extracting("status").isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }
}
