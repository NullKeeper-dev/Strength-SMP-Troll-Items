package dev.nullkeeper.strengthsmptrollitems.update;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public final class ModrinthApiClient implements VersionSource {
    public static final String PROJECT_PAGE =
            "https://modrinth.com/project/strength-smp-troll-items";
    private static final URI VERSIONS_ENDPOINT = URI.create(
            "https://api.modrinth.com/v2/project/strength-smp-troll-items/version"
                    + "?include_changelog=false");
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(8);

    private final HttpClient client;
    private final HttpRequest request;

    public ModrinthApiClient(HttpClient client, String pluginVersion) {
        this.client = Objects.requireNonNull(client, "client");
        Objects.requireNonNull(pluginVersion, "pluginVersion");
        request = HttpRequest.newBuilder(VERSIONS_ENDPOINT)
                .timeout(REQUEST_TIMEOUT)
                .header(
                        "User-Agent",
                        "NullKeeper-dev/Strength-SMP-Troll-Items/" + pluginVersion)
                .GET()
                .build();
    }

    @Override
    public CompletableFuture<String> fetch() {
        return client.sendAsync(request, BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new IllegalStateException(
                                "Modrinth returned HTTP " + response.statusCode());
                    }
                    return response.body();
                });
    }
}
