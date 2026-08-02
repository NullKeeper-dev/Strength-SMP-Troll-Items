package dev.nullkeeper.strengthsmptrollitems.update;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

final class ModrinthVersionParser {
    private static final int MAX_RESPONSE_CHARACTERS = 1_048_576;
    private static final Pattern VERSION_NUMBER = Pattern.compile(
            "\\\"version_number\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");

    private ModrinthVersionParser() {}

    static Optional<SemanticVersion> latest(String responseBody) {
        Objects.requireNonNull(responseBody, "responseBody");
        if (responseBody.length() > MAX_RESPONSE_CHARACTERS) {
            throw new IllegalArgumentException("Modrinth response exceeds the size limit");
        }
        Matcher matcher = VERSION_NUMBER.matcher(responseBody);
        Stream.Builder<SemanticVersion> versions = Stream.builder();
        while (matcher.find()) {
            SemanticVersion.parse(matcher.group(1)).ifPresent(versions::add);
        }
        return versions.build().max(Comparator.naturalOrder());
    }
}
