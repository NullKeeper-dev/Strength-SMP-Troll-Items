package dev.nullkeeper.strengthsmptrollitems.update;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface VersionSource {
    CompletableFuture<String> fetch();
}
