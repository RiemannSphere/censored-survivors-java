package com.censoredsurvivors.data.generator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.censoredsurvivors.util.ProjectConfig;

import tech.tablesaw.api.DateColumn;
import tech.tablesaw.api.Table;
import java.time.LocalDate;

public class SocialMediaCustomerGeneratorTest {
    private static final double EPSILON = 0.01;
    private SocialMediaCustomerGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new SocialMediaCustomerGenerator();
    }

    @ParameterizedTest
    @CsvSource({
        // Basic coverage of numberOfCustomers
        "'Basic small sample', 57, 0.0, 0.1, 1",
        "'Basic medium sample', 101, 0.1, 0.5, 5",
        "'Basic large sample', 10007, 0.5, 0.0, 15",
        
        // Coverage of left censoring
        "'Left censoring small', 57, 0.1, 0.8, 5",
        "'Left censoring medium', 101, 0.5, 0.5, 15",
        "'Left censoring large', 10007, 0.8, 0.1, 1",
        
        // Coverage of right censoring
        "'Right censoring small', 57, 0.1, 0.1, 15",
        "'Right censoring medium', 101, 0.0, 0.5, 1",
        "'Right censoring large', 10007, 0.1, 0.8, 5",
        
        // Edge cases
        "'Edge case full right', 57, 0.0, 1.0, 1",
        "'Edge case full left', 101, 1.0, 0.0, 5",
        "'Edge case balanced', 10007, 0.5, 0.5, 15",
    })
    void shouldGenerateCustomers(
        String scenario,
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

        Assertions.assertEquals(numberOfCustomers, customers.rowCount(), 
            String.format("[%s] Number of customers", scenario));
        Assertions.assertEquals(Math.floor(numberOfCustomers * percentOfLeftCensoredCustomers), leftCensored, EPSILON * numberOfCustomers, 
            String.format("[%s] Number of left censored customers", scenario));
        Assertions.assertEquals(Math.floor(numberOfCustomers * percentOfRightCensoredCustomers), rightCensored, EPSILON * numberOfCustomers, 
            String.format("[%s] Number of right censored customers", scenario));
        Assertions.assertEquals(customers.stringColumn(ProjectConfig.CUSTOMER_ID_COLUMN).unique().size(), customers.rowCount(), 
            String.format("[%s] Customer IDs are unique", scenario));
        Assertions.assertEquals(customers.stringColumn(ProjectConfig.CUSTOMER_NAME_COLUMN).unique().size(), customers.rowCount(), 
            String.format("[%s] Customer names are unique", scenario));

        LocalDate minContractStartDate = customers.dateColumn(ProjectConfig.CONTRACT_START_DATE_COLUMN).min();
        LocalDate extendedObservationStartDate = ProjectConfig.OBSERVATION_START_DATE.minusYears(ProjectConfig.EXTENDED_PERIOD_YEARS);
        Assertions.assertTrue(
            minContractStartDate.isAfter(extendedObservationStartDate) || minContractStartDate.equals(extendedObservationStartDate), 
            String.format("[%s] Contract start dates: min contract start date %s should be after extended observation start date %s", 
                scenario, minContractStartDate, extendedObservationStartDate)
        );

        LocalDate maxContractEndDate = customers.dateColumn(ProjectConfig.CONTRACT_END_DATE_COLUMN).max();
        LocalDate extendedObservationEndDate = ProjectConfig.OBSERVATION_START_DATE.plusYears(observationPeriodInYears + ProjectConfig.EXTENDED_PERIOD_YEARS);
        Assertions.assertTrue(
            maxContractEndDate.isBefore(extendedObservationEndDate) || maxContractEndDate.equals(extendedObservationEndDate), 
            String.format("[%s] Contract end dates: max contract end date %s should be before extended observation end date %s", 
                scenario, maxContractEndDate, extendedObservationEndDate)
        );
    }
    
    @Test
    void shouldThrowExceptionIfPercentOfLeftCensoredCustomersIsNegative() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            generator.generateCustomers(100, -0.1, 0.1, 5);
        });
    }

    @Test
    void shouldThrowExceptionIfPercentOfRightCensoredCustomersIsNegative() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            generator.generateCustomers(100, 0.1, -0.1, 5);
        });
    }

    @Test
    void shouldThrowExceptionIfObservationPeriodIsNegative() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            generator.generateCustomers(100, 0.1, 0.1, -5);
        });
    }

    @Test
    void shouldThrowExceptionIfObservationPeriodIsZero() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            generator.generateCustomers(100, 0.1, 0.1, 0);
        });
    }

    @Test
    void shouldThrowExceptionIfPercentOfLeftCensoredCustomersAndPercentOfRightCensoredCustomersSumIsGreaterThanOne() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            generator.generateCustomers(100, 0.6, 0.6, 5);
        });
    }
}
