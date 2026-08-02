package dev.nullkeeper.strengthsmptrollitems.update;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticVersionTest {
    @Test
    void stableReleaseSortsAfterPrerelease() {
        SemanticVersion beta = SemanticVersion.parse("2.1.0-beta.2").orElseThrow();
        SemanticVersion stable = SemanticVersion.parse("2.1.0").orElseThrow();

        assertTrue(stable.compareTo(beta) > 0);
    }

    @Test
    void numericPrereleaseIdentifiersCompareNumerically() {
        SemanticVersion second = SemanticVersion.parse("2.1.0-beta.2").orElseThrow();
        SemanticVersion tenth = SemanticVersion.parse("2.1.0-beta.10").orElseThrow();

        assertTrue(tenth.compareTo(second) > 0);
    }

    @Test
    void parsesOptionalVPrefixAndIgnoresBuildMetadataForPrecedence() {
        assertEquals(
                SemanticVersion.parse("2.1.0").orElseThrow(),
                SemanticVersion.parse("v2.1.0+paper.26.2").orElseThrow());
    }

    @Test
    void rejectsNonSemanticVersionAndLeadingZeroes() {
        assertTrue(SemanticVersion.parse("August release").isEmpty());
        assertTrue(SemanticVersion.parse("02.1.0").isEmpty());
        assertTrue(SemanticVersion.parse("2.1").isEmpty());
    }
}
