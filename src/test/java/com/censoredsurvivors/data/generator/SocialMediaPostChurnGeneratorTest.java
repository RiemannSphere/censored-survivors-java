package com.censoredsurvivors.data.generator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tech.tablesaw.api.Table;

public class SocialMediaPostChurnGeneratorTest {
    private SocialMediaPostChurnGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new SocialMediaPostChurnGenerator();
    }

    @Nested
    @DisplayName("Customer Generation")
    class CustomerGenerationTest {
        
        @Test
        @DisplayName("should generate customers with correct ranges")
        void testGenerateCustomers() {
            Table customers = generator.generateCustomers(100, 0.1, 0.1, 5);
        }
    }
}
