package com.censoredsurvivors.data.generator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.censoredsurvivors.util.ProjectConfig;

import tech.tablesaw.api.DateColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import java.time.LocalDate;

public class SocialMediaCustomerGeneratorTest {
    private SocialMediaCustomerGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new SocialMediaCustomerGenerator();
    }

    @ParameterizedTest
    @CsvSource({
        // Basic coverage of numberOfCustomers
        "'Basic small sample', 57, 0.0, 0.1, 1",
        "'Basic medium sample', 100, 0.1, 0.5, 5",
        "'Basic large sample', 10007, 0.5, 0.0, 15",
        
        // Coverage of left censoring
        "'Left censoring small', 57, 0.1, 0.8, 5",
        "'Left censoring medium', 100, 0.5, 0.5, 15",
        "'Left censoring large', 10007, 0.8, 0.1, 1",
        
        // Coverage of right censoring
        "'Right censoring small', 57, 0.1, 0.1, 15",
        "'Right censoring medium', 100, 0.0, 0.5, 1",
        "'Right censoring large', 10007, 0.1, 0.8, 5",
        
        // Edge cases
        "'Edge case full right', 57, 0.0, 1.0, 1",
        "'Edge case full left', 100, 1.0, 0.0, 5",
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
            contractStartDateColumn.isOnOrBefore(ProjectConfig.OBSERVATION_START_DATE)
        ).rowCount();
        long rightCensored = customers.where(
            contractEndDateColumn.isOnOrAfter(ProjectConfig.OBSERVATION_START_DATE.plusYears(observationPeriodInYears))
        ).rowCount();

        Assertions.assertEquals(numberOfCustomers, customers.rowCount(), 
            String.format("[%s] Number of customers", scenario));
        Assertions.assertEquals((int)(numberOfCustomers * percentOfLeftCensoredCustomers), leftCensored, 
            String.format("[%s] Number of left censored customers", scenario));
        Assertions.assertEquals((int)(numberOfCustomers * percentOfRightCensoredCustomers), rightCensored, 
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

    @ParameterizedTest
    @CsvSource({
        "'Churn zero', 10_000, 1, 0.0",
        "'Churn small', 10_000, 1, 0.1",
        "'Churn medium', 10_000, 5, 0.5",
        "'Churn large', 10_000, 15, 0.8",
        "'Churn one', 10_000, 1, 1.0",
    })
    void shouldGenerateCustomersWithChurn(
        String scenario,
        int numberOfCustomers, 
        int observationPeriodInYears,
        double churnProbability
    ) {
        int delta = (int)(0.05 * numberOfCustomers);
        // ensure the contract duration is over the minimum duration for churn
        boolean allCustomersFullLifetime = true;
        SocialMediaCustomerGenerator generator = new SocialMediaCustomerGenerator(allCustomersFullLifetime);
        Table customers = generator.generateUncensoredCustomers(
            numberOfCustomers, 
            observationPeriodInYears,
            churnProbability
        );

        int expectedChurnedCustomers = (int)(numberOfCustomers * churnProbability);

        DateColumn churnDateColumn = customers.where(
            customers.dateColumn(ProjectConfig.CHURN_DATE_COLUMN).isNotMissing()
        ).dateColumn(ProjectConfig.CHURN_DATE_COLUMN);
        StringColumn churnReasonColumn = customers.where(
            customers.stringColumn(ProjectConfig.CHURN_REASON_COLUMN).isNotMissing()
        ).stringColumn(ProjectConfig.CHURN_REASON_COLUMN);

        Assertions.assertEquals(expectedChurnedCustomers, churnDateColumn.size(), delta,
            String.format("[%s] Number of churn dates", scenario));
        Assertions.assertEquals(expectedChurnedCustomers, churnReasonColumn.size(), delta,
            String.format("[%s] Number of churn reasons", scenario));

        for (int i = 0; i < churnDateColumn.size(); i++) {
            Assertions.assertTrue(churnDateColumn.get(i).isAfter(ProjectConfig.OBSERVATION_START_DATE), 
            String.format("[%s] Churn date %s should be after observation start date %s", 
                scenario, churnDateColumn.get(i), ProjectConfig.OBSERVATION_START_DATE));
        }
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

    @Test
    void shouldThrowExceptionIfChurnProbabilityIsNegative() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            generator.generateCustomers(100, 0.1, 0.1, 5, -0.1);
        });
    }

    @Test
    void shouldThrowExceptionIfChurnProbabilityIsGreaterThanOne() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            generator.generateCustomers(100, 0.1, 0.1, 5, 1.1);
        });
    }   


}
