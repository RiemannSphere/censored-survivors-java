package com.censoredsurvivors.data.generator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.censoredsurvivors.util.ProjectConfig;

import tech.tablesaw.api.DateColumn;
import tech.tablesaw.api.Table;


public class SocialMediaCustomerGeneratorTest {
    private static final double EPSILON = 0.01;
    private SocialMediaCustomerGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new SocialMediaCustomerGenerator();
    }

    @ParameterizedTest
    @CsvSource({
        "100, 0.0, 0.8, 0",
        "0, 0.1, 0.8, 5",
        "10000, 0.0, 0.1, 1",
        "0, 0.1, 0.5, 1",
        "10000, 0.0, 0.0, 15",
        "1, 0.5, 0.8, 15",
        "100, 0.5, 1.0, 5",
        "100, 0.0, 0.1, 15",
        "10000, 0.5, 0.8, 5"
    })
    void shouldGenerateCustomers(
        int numberOfCustomers, 
        double percentOfLeftCensoredCustomers, 
        double percentOfRightCensoredCustomers, 
        int observationPeriodInYears
    ) {
        Table customers = generator.generateCustomers(
            numberOfCustomers, 
            percentOfLeftCensoredCustomers, 
            percentOfRightCensoredCustomers, 
            observationPeriodInYears
        );

        DateColumn contractStartDateColumn = customers.dateColumn(ProjectConfig.CONTRACT_START_DATE_COLUMN);
        DateColumn contractEndDateColumn = customers.dateColumn(ProjectConfig.CONTRACT_END_DATE_COLUMN);
        long leftCensored = customers.where(
            contractStartDateColumn.isBefore(ProjectConfig.OBSERVATION_START_DATE)
        ).rowCount();
        long rightCensored = customers.where(
            contractEndDateColumn.isAfter(ProjectConfig.OBSERVATION_START_DATE.plusYears(observationPeriodInYears))
        ).rowCount();

        Assertions.assertEquals(numberOfCustomers, customers.rowCount(), "Number of customers.");
        Assertions.assertEquals(leftCensored, numberOfCustomers * percentOfLeftCensoredCustomers, EPSILON * numberOfCustomers, "Number of left censored customers.");
        Assertions.assertEquals(rightCensored, numberOfCustomers * percentOfRightCensoredCustomers, EPSILON * numberOfCustomers, "Number of right censored customers.");
    }
}
