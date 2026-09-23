package com.gabryel.taskboard.config;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseUrlParserTest {

    @Test
    void neonStyleUriWithQueryParamsIsConvertedToJdbcUrl() {
        Optional<DatabaseUrlParser.ParsedDatabaseUrl> result = DatabaseUrlParser.parse(
                "postgresql://neondb_owner:s3cr3t@ep-cool-forest-123456.eu-west-2.aws.neon.tech/taskboard?sslmode=require&channel_binding=require");

        assertThat(result).isPresent();
        DatabaseUrlParser.ParsedDatabaseUrl parsed = result.get();
        assertThat(parsed.jdbcUrl()).isEqualTo(
                "jdbc:postgresql://ep-cool-forest-123456.eu-west-2.aws.neon.tech/taskboard?sslmode=require&channel_binding=require");
        assertThat(parsed.username()).isEqualTo("neondb_owner");
        assertThat(parsed.password()).isEqualTo("s3cr3t");
    }

    @Test
    void percentEncodedCharactersInThePasswordAreDecoded() {
        Optional<DatabaseUrlParser.ParsedDatabaseUrl> result = DatabaseUrlParser.parse(
                "postgres://user:p%40ss%3Aw%2Ford%21@ep-example.eu-west-2.aws.neon.tech/taskboard?sslmode=require");

        assertThat(result).isPresent();
        assertThat(result.get().password()).isEqualTo("p@ss:w/ord!");
        assertThat(result.get().username()).isEqualTo("user");
    }

    @Test
    void explicitPortIsPreservedInTheJdbcUrl() {
        Optional<DatabaseUrlParser.ParsedDatabaseUrl> result = DatabaseUrlParser.parse(
                "postgresql://user:pass@db.example.com:6543/taskboard");

        assertThat(result).isPresent();
        assertThat(result.get().jdbcUrl()).isEqualTo("jdbc:postgresql://db.example.com:6543/taskboard");
    }

    @Test
    void jdbcUrlIsLeftForPassthrough() {
        Optional<DatabaseUrlParser.ParsedDatabaseUrl> result = DatabaseUrlParser.parse(
                "jdbc:postgresql://localhost:5432/taskboard");

        assertThat(result).isEmpty();
    }

    @Test
    void missingDatabaseUrlIsLeftForPassthrough() {
        assertThat(DatabaseUrlParser.parse(null)).isEmpty();
    }

    @Test
    void missingPasswordYieldsNullPassword() {
        Optional<DatabaseUrlParser.ParsedDatabaseUrl> result = DatabaseUrlParser.parse(
                "postgresql://user@db.example.com/taskboard");

        assertThat(result).isPresent();
        assertThat(result.get().username()).isEqualTo("user");
        assertThat(result.get().password()).isNull();
    }
}
