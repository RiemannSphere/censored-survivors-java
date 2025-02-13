package com.censoredsurvivors.data.generator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import tech.tablesaw.api.Table;

public class SocialMediaPostsGeneratorTest {

    private SocialMediaPostsGenerator postsGenerator;

    private Table customers;

    @BeforeAll
    public void setUp() {
        SocialMediaCustomerGenerator customerGenerator = new SocialMediaCustomerGenerator();
        customers = customerGenerator.generateCustomers(100, 0.1, 0.1, 5);
        postsGenerator = new SocialMediaPostsGenerator(customers);
    }

    @Test
    public void testGeneratePosts() {
        Table posts = postsGenerator.generatePosts("Platform Posts");
        // TODO: add assertions
    }
}
