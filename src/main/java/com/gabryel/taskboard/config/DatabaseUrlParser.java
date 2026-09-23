package com.gabryel.taskboard.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Pure parsing of a Postgres connection URI (as Neon and most managed
 * Postgres providers hand it out, e.g. {@code postgresql://user:pass@host/db?sslmode=require})
 * into the pieces Spring's datasource properties need.
 *
 * <p>A value that is not a {@code postgres://} or {@code postgresql://} URI
 * (for example an already-JDBC {@code jdbc:postgresql://...} URL, or no value
 * at all) is left alone: {@link #parse} returns {@link Optional#empty()} so
 * the caller keeps whatever is already configured.
 */
public final class DatabaseUrlParser {

    private DatabaseUrlParser() {
    }

    public record ParsedDatabaseUrl(String jdbcUrl, String username, String password) {
    }

    public static Optional<ParsedDatabaseUrl> parse(String databaseUrl) {
        if (databaseUrl == null) {
            return Optional.empty();
        }
        if (!databaseUrl.startsWith("postgres://") && !databaseUrl.startsWith("postgresql://")) {
            return Optional.empty();
        }

        URI uri;
        try {
            uri = new URI(databaseUrl);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("DATABASE_URL is not a valid URI: " + e.getMessage(), e);
        }

        String username = null;
        String password = null;
        String rawUserInfo = uri.getRawUserInfo();
        if (rawUserInfo != null) {
            int separator = rawUserInfo.indexOf(':');
            if (separator >= 0) {
                username = decode(rawUserInfo.substring(0, separator));
                password = decode(rawUserInfo.substring(separator + 1));
            } else {
                username = decode(rawUserInfo);
            }
        }

        StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://").append(uri.getHost());
        if (uri.getPort() >= 0) {
            jdbcUrl.append(':').append(uri.getPort());
        }
        if (uri.getRawPath() != null) {
            jdbcUrl.append(uri.getRawPath());
        }
        if (uri.getRawQuery() != null && !uri.getRawQuery().isEmpty()) {
            jdbcUrl.append('?').append(uri.getRawQuery());
        }

        return Optional.of(new ParsedDatabaseUrl(jdbcUrl.toString(), username, password));
    }

    /**
     * Percent-decodes a URI component without {@link URLDecoder}'s
     * form-encoding quirk of also turning a literal {@code +} into a space.
     */
    private static String decode(String value) {
        return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
    }
}
