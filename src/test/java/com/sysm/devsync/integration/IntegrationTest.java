package com.sysm.devsync.integration;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.*;

/**
 * Abstract base class for integration tests.
 *
 * Configures the test environment to:
 * - Load the full Spring application context (@SpringBootTest).
 * - Use the "test" application profile (@ActiveProfiles("test")), which should be
 *   configured to use an in-memory database like H2.
 * - Auto-configure MockMvc for simulating HTTP requests (@AutoConfigureMockMvc).
 * - Run each test within a transaction that is rolled back by default (@Transactional),
 *   ensuring tests are isolated and do not affect each other.
 */

@Inherited
@Transactional // Ensures each test runs in its own transaction and is rolled back
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("tests") // Use a specific profile for tests, e.g., "tests" or "integration"
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface IntegrationTest {
}
