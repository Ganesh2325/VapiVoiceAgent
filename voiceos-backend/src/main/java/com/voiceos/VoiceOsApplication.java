package com.voiceos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * VoiceOS — Multi-Agent Voice AI Operations Platform.
 *
 * <p>Spring Boot 3.x entry point with:
 * <ul>
 *   <li>Automatic .env loader for seamless development configuration</li>
 *   <li>Java 21 Virtual Threads (configured in application.yml)</li>
 *   <li>JPA Auditing for automatic created_at / updated_at management</li>
 *   <li>Async execution for agent and tool operations</li>
 *   <li>Scheduling for periodic tasks (evaluation, memory cleanup)</li>
 * </ul>
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan
public class VoiceOsApplication {

    private static final Logger log = LoggerFactory.getLogger(VoiceOsApplication.class);

    public static void main(String[] args) {
        loadDotEnv();
        SpringApplication.run(VoiceOsApplication.class, args);
    }

    /**
     * Automatically discovers and loads variables from .env file into System properties.
     */
    private static void loadDotEnv() {
        List<Path> candidatePaths = List.of(
                Paths.get(".env"),
                Paths.get("..", ".env"),
                Paths.get(System.getProperty("user.dir", "."), ".env"),
                Paths.get(System.getProperty("user.dir", "."), "..", ".env")
        );

        for (Path path : candidatePaths) {
            if (Files.exists(path) && !Files.isDirectory(path)) {
                try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
                    String line;
                    int count = 0;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) {
                            continue;
                        }
                        int eqIdx = line.indexOf('=');
                        if (eqIdx > 0) {
                            String key = line.substring(0, eqIdx).trim();
                            String val = line.substring(eqIdx + 1).trim();
                            // Strip surrounding quotes if present
                            if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                                val = val.substring(1, val.length() - 1);
                            }
                            if (System.getProperty(key) == null && System.getenv(key) == null) {
                                System.setProperty(key, val);
                                count++;
                            }
                        }
                    }
                    log.info("Loaded {} environment variables from {}", count, path.toAbsolutePath().normalize());
                    break;
                } catch (Exception e) {
                    log.warn("Could not read .env at {}: {}", path, e.getMessage());
                }
            }
        }
    }
}
