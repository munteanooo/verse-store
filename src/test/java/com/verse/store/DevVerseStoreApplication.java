package com.verse.store;

import org.springframework.boot.SpringApplication;

/** Local-only launcher that starts the application with PostgreSQL Testcontainers. */
public final class DevVerseStoreApplication {

    private DevVerseStoreApplication() {
    }

    public static void main(String[] args) {
        SpringApplication.from(VerseStoreApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
