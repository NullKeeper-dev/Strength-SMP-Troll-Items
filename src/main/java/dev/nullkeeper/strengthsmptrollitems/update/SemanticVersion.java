package dev.nullkeeper.strengthsmptrollitems.update;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

record SemanticVersion(
        int major,
        int minor,
        int patch,
        List<String> prerelease) implements Comparable<SemanticVersion> {
    private static final Pattern FORMAT = Pattern.compile(
            "^v?(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)"
                    + "(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?"
                    + "(?:\\+[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?$");

    SemanticVersion {
        prerelease = List.copyOf(prerelease);
    }

    static Optional<SemanticVersion> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }
        Matcher matcher = FORMAT.matcher(value.trim());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        List<String> prerelease = matcher.group(4) == null
                ? List.of()
                : List.of(matcher.group(4).split("\\."));
        if (prerelease.stream().anyMatch(SemanticVersion::invalidNumericIdentifier)) {
            return Optional.empty();
        }
        try {
            return Optional.of(new SemanticVersion(
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)),
                    prerelease));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    private static boolean invalidNumericIdentifier(String identifier) {
        return numeric(identifier) && identifier.length() > 1 && identifier.startsWith("0");
    }

    @Override
    public int compareTo(SemanticVersion other) {
        Objects.requireNonNull(other, "other");
        int core = compareCore(other);
        if (core != 0) {
            return core;
        }
        if (prerelease.isEmpty() || other.prerelease.isEmpty()) {
            return Boolean.compare(prerelease.isEmpty(), other.prerelease.isEmpty());
        }
        for (int index = 0; index < Math.min(prerelease.size(), other.prerelease.size()); index++) {
            int identifier = compareIdentifier(prerelease.get(index), other.prerelease.get(index));
            if (identifier != 0) {
                return identifier;
            }
        }
        return Integer.compare(prerelease.size(), other.prerelease.size());
    }

    private int compareCore(SemanticVersion other) {
        int comparison = Integer.compare(major, other.major);
        if (comparison == 0) {
            comparison = Integer.compare(minor, other.minor);
        }
        return comparison == 0 ? Integer.compare(patch, other.patch) : comparison;
    }

    private static int compareIdentifier(String left, String right) {
        boolean leftNumeric = numeric(left);
        boolean rightNumeric = numeric(right);
        if (leftNumeric && rightNumeric) {
            return new BigInteger(left).compareTo(new BigInteger(right));
        }
        if (leftNumeric != rightNumeric) {
            return leftNumeric ? -1 : 1;
        }
        return left.compareTo(right);
    }

    private static boolean numeric(String value) {
        return value.chars().allMatch(Character::isDigit);
    }

    @Override
    public String toString() {
        String core = major + "." + minor + "." + patch;
        return prerelease.isEmpty() ? core : core + "-" + String.join(".", prerelease);
    }
}
