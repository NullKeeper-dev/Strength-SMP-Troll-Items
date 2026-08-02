package dev.nullkeeper.strengthsmptrollitems.update;

import dev.nullkeeper.strengthsmptrollitems.config.PluginConfig;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.logging.Logger;

public final class UpdateChecker implements AutoCloseable {
    private final Supplier<PluginConfig> configSource;
    private final SemanticVersion current;
    private final String pageUrl;
    private final VersionSource source;
    private final Logger logger;
    private final AtomicReference<Optional<UpdateInfo>> available =
            new AtomicReference<>(Optional.empty());
    private final AtomicReference<CompletableFuture<String>> request =
            new AtomicReference<>();
    private final AtomicBoolean successful = new AtomicBoolean();
    private final AtomicBoolean checking = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();

    public UpdateChecker(
            Supplier<PluginConfig> configSource,
            String currentVersion,
            String pageUrl,
            VersionSource source,
            Logger logger) {
        this.configSource = Objects.requireNonNull(configSource, "configSource");
        this.current = SemanticVersion.parse(currentVersion).orElseThrow(() ->
                new IllegalArgumentException("Plugin version must use semantic versioning"));
        this.pageUrl = requireText(pageUrl, "pageUrl");
        this.source = Objects.requireNonNull(source, "source");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public void refresh() {
        if (closed.get()
                || !configSource.get().updateChecker().enabled()
                || successful.get()
                || !checking.compareAndSet(false, true)) {
            return;
        }

        try {
            CompletableFuture<String> future = Objects.requireNonNull(
                    source.fetch(),
                    "Version source returned no request");
            request.set(future);
            future.whenComplete(this::complete);
        } catch (RuntimeException exception) {
            fail(exception);
        }
    }

    public Optional<UpdateInfo> availableUpdate() {
        if (closed.get() || !configSource.get().updateChecker().enabled()) {
            return Optional.empty();
        }
        return available.get();
    }

    private void complete(String responseBody, Throwable failure) {
        try {
            if (closed.get()) {
                return;
            }
            if (failure != null) {
                warn(failure);
                return;
            }
            SemanticVersion latest = ModrinthVersionParser.latest(responseBody)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Modrinth returned no semantic versions"));
            if (latest.compareTo(current) > 0) {
                UpdateInfo info = new UpdateInfo(
                        current.toString(),
                        latest.toString(),
                        pageUrl);
                available.set(Optional.of(info));
                logger.info("Strength SMP Troll Items " + current
                        + " is outdated; version " + latest + " is available");
            } else {
                available.set(Optional.empty());
            }
            successful.set(true);
        } catch (RuntimeException exception) {
            warn(exception);
        } finally {
            request.set(null);
            checking.set(false);
        }
    }

    private void fail(RuntimeException exception) {
        request.set(null);
        checking.set(false);
        warn(exception);
    }

    private void warn(Throwable failure) {
        Throwable cause = unwrap(failure);
        String detail = cause.getMessage();
        if (detail == null || detail.isBlank()) {
            detail = cause.getClass().getSimpleName();
        }
        logger.warning("Could not check Modrinth for updates: " + detail);
    }

    private static Throwable unwrap(Throwable failure) {
        if (failure instanceof CompletionException && failure.getCause() != null) {
            return failure.getCause();
        }
        return failure;
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        available.set(Optional.empty());
        CompletableFuture<String> pending = request.getAndSet(null);
        if (pending != null) {
            pending.cancel(true);
        }
    }
}
