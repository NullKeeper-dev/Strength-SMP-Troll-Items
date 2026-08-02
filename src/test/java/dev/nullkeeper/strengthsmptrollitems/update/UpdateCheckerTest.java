package dev.nullkeeper.strengthsmptrollitems.update;

import dev.nullkeeper.strengthsmptrollitems.config.ConfigLoader;
import dev.nullkeeper.strengthsmptrollitems.config.PluginConfig;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateCheckerTest {
    private static final String PAGE_URL =
            "https://modrinth.com/project/strength-smp-troll-items";

    @Test
    void cachesOutdatedResultFromCompletedSource() {
        VersionSource source = () -> CompletableFuture.completedFuture(
                "[{\"version_number\":\"2.1.0\"}]");
        UpdateChecker checker = checker(true, "2.0.0", source);

        checker.refresh();

        UpdateInfo update = checker.availableUpdate().orElseThrow();
        assertEquals("2.0.0", update.current());
        assertEquals("2.1.0", update.latest());
        assertEquals(PAGE_URL, update.pageUrl());
    }

    @Test
    void disabledCheckerDoesNotCallSource() {
        AtomicInteger calls = new AtomicInteger();
        VersionSource source = () -> {
            calls.incrementAndGet();
            return CompletableFuture.completedFuture("[]");
        };

        checker(false, "2.0.0", source).refresh();

        assertEquals(0, calls.get());
    }

    @Test
    void currentOrNewerInstallHasNoAvailableUpdate() {
        UpdateChecker checker = checker(true, "2.1.0", () ->
                CompletableFuture.completedFuture("[{\"version_number\":\"2.1.0\"}]"));

        checker.refresh();

        assertTrue(checker.availableUpdate().isEmpty());
    }

    @Test
    void onlyOneRequestRunsAtATimeAndSuccessfulResultIsCached() {
        AtomicInteger calls = new AtomicInteger();
        CompletableFuture<String> response = new CompletableFuture<>();
        VersionSource source = () -> {
            calls.incrementAndGet();
            return response;
        };
        UpdateChecker checker = checker(true, "2.0.0", source);

        checker.refresh();
        checker.refresh();
        response.complete("[{\"version_number\":\"2.1.0\"}]");
        checker.refresh();

        assertEquals(1, calls.get());
        assertEquals("2.1.0", checker.availableUpdate().orElseThrow().latest());
    }

    @Test
    void failedRequestCanBeRetried() {
        AtomicInteger calls = new AtomicInteger();
        VersionSource source = () -> calls.getAndIncrement() == 0
                ? CompletableFuture.failedFuture(new IllegalStateException("offline"))
                : CompletableFuture.completedFuture("[{\"version_number\":\"2.1.0\"}]");
        UpdateChecker checker = checker(true, "2.0.0", source);

        checker.refresh();
        checker.refresh();

        assertEquals(2, calls.get());
        assertEquals("2.1.0", checker.availableUpdate().orElseThrow().latest());
    }

    @Test
    void closeSuppressesLateCompletion() {
        CompletableFuture<String> response = new CompletableFuture<>();
        UpdateChecker checker = checker(true, "2.0.0", () -> response);

        checker.refresh();
        checker.close();
        response.complete("[{\"version_number\":\"2.1.0\"}]");

        assertTrue(checker.availableUpdate().isEmpty());
    }

    private static UpdateChecker checker(
            boolean enabled,
            String current,
            VersionSource source) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("update-checker.enabled", enabled);
        PluginConfig config = new ConfigLoader().load(yaml);
        return new UpdateChecker(
                () -> config,
                current,
                PAGE_URL,
                source,
                Logger.getAnonymousLogger());
    }
}
