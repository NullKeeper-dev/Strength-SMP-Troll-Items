package dev.nullkeeper.strengthsmptrollitems.update;

import java.util.Objects;

public record UpdateInfo(String current, String latest, String pageUrl) {
    public UpdateInfo {
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(latest, "latest");
        Objects.requireNonNull(pageUrl, "pageUrl");
    }
}
