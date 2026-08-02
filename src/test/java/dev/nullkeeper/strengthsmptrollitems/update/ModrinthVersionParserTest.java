package dev.nullkeeper.strengthsmptrollitems.update;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModrinthVersionParserTest {
    @Test
    void selectsHighestValidVersionNumber() {
        String body = """
                [{"version_number":"2.0.1"},{"version_number":"2.1.0"},{"version_number":"notes"}]
                """;

        assertEquals("2.1.0", ModrinthVersionParser.latest(body).orElseThrow().toString());
    }

    @Test
    void acceptsWhitespaceAroundJsonSeparator() {
        String body = "[{\"version_number\" : \"v3.0.0-beta.1\"}]";

        assertEquals(
                "3.0.0-beta.1",
                ModrinthVersionParser.latest(body).orElseThrow().toString());
    }

    @Test
    void emptyOrInvalidResponseHasNoLatestVersion() {
        assertTrue(ModrinthVersionParser.latest("[]").isEmpty());
        assertTrue(ModrinthVersionParser.latest("not json").isEmpty());
    }

    @Test
    void rejectsOversizedResponse() {
        String body = " ".repeat(1_048_577);

        assertThrows(IllegalArgumentException.class, () -> ModrinthVersionParser.latest(body));
    }
}
