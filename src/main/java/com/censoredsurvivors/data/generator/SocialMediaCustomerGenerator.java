package com.censoredsurvivors.data.generator;

import tech.tablesaw.api.Table;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.DateColumn;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Random;

import org.apache.commons.math3.util.Pair;

import com.censoredsurvivors.util.ProjectConfig;


public class SocialMediaCustomerGenerator {
    private final Random random = ProjectConfig.RANDOM;

    // percentage of the contract duration that the churn can happen, should be between 0 and 1
    private final double EARLIEST_POSSIBLE_CHURN = 0.2;
    private final double LATEST_POSSIBLE_CHURN = 0.8;
    private final long MIN_DURATION_FOR_CHURN = 360;

    private boolean allCustomersFullLifetime;

    public SocialMediaCustomerGenerator(boolean allCustomersFullLifetime) {
        this.allCustomersFullLifetime = allCustomersFullLifetime;
    }

    public SocialMediaCustomerGenerator() {
        this(false);
    }

    /**
     * @see #generateCustomers(int, double, double, int)
     */
    public Table generateUncensoredCustomers(
        int numberOfCustomers, 
        int observationPeriodInYears
    ) {
        return generateCustomers(
            numberOfCustomers, 
            0, 
            0, 
            observationPeriodInYears,
            0d
        );
    }

    /**
     * @see #generateCustomers(int, double, double, int)
     */
    public Table generateUncensoredCustomers(
        int numberOfCustomers, 
        int observationPeriodInYears,
        double churnProbability
    ) {
        return generateCustomers(
            numberOfCustomers, 
            0, 
            0, 
            observationPeriodInYears,
            churnProbability
        );
    }

    /**
     * @see #generateCustomers(int, double, double, int)
     */
    public Table generateCustomers(
        int numberOfCustomers, 
        double percentOfLeftCensoredCustomers, 
        double percentOfRightCensoredCustomers,
        int observationPeriodInYears
    ) {
        return generateCustomers(
            numberOfCustomers, 
            percentOfLeftCensoredCustomers, 
            percentOfRightCensoredCustomers,
            observationPeriodInYears,
            0d
        );
    }

    /**
     * Generates a table of customers.
     * 
     * The timeline for generation looks like this:
     * |--extended period--|--observation period--|--extended period--|
     * 
     * Left censored customers are being generated withing the first extended period and the observation period.
     * Right censored customers are being generated withing the observation period and the second extended period.
     * 
     * @param numberOfCustomers The number of customers to generate.
     * @param percentOfLeftCensoredCustomers The percentage of customers that started before the observation period.
     * @param percentOfRightCensoredCustomers The percentage of customers that will end after the observation period.
     * @param observationPeriodInYears The number of years to observe the customers.
     * @return A table of customers.
     */
    public Table generateCustomers(
        int numberOfCustomers, 
        double percentOfLeftCensoredCustomers, 
        double percentOfRightCensoredCustomers,
        int observationPeriodInYears,
        double churnProbability
    ) {        
        if (percentOfLeftCensoredCustomers + percentOfRightCensoredCustomers > 1) {
            throw new IllegalArgumentException("The sum of percentOfLeftCensoredCustomers and percentOfRightCensoredCustomers cannot be greater than 1.");
        }
        if (percentOfLeftCensoredCustomers < 0 || percentOfRightCensoredCustomers < 0) {
            throw new IllegalArgumentException("The percent of left censored customers and the percent of right censored customers cannot be negative.");
        }
        if (observationPeriodInYears <= 0) {
            throw new IllegalArgumentException("The observation period has to be positive.");
        }
        if (churnProbability < 0 || churnProbability > 1) {
            throw new IllegalArgumentException("The churn probability has to be between 0 and 1.");
        }

        LocalDate observationStartDate = ProjectConfig.OBSERVATION_START_DATE;
        LocalDate observationEndDate = observationStartDate.plusYears(observationPeriodInYears);
        
        LocalDate extendedStartDate = observationStartDate.minusYears(ProjectConfig.EXTENDED_PERIOD_YEARS);
        LocalDate extendedEndDate = observationEndDate.plusYears(ProjectConfig.EXTENDED_PERIOD_YEARS);
        
        int leftCensoredCount = (int)(numberOfCustomers * percentOfLeftCensoredCustomers);
        int rightCensoredCount = (int)(numberOfCustomers * percentOfRightCensoredCustomers);

        String[] customerIds = new String[numberOfCustomers];
        String[] customerNames = new String[numberOfCustomers];
        String[] industries = new String[numberOfCustomers];
        String[] countries = new String[numberOfCustomers];
        LocalDate[] contractStartDates = new LocalDate[numberOfCustomers];
        LocalDate[] contractEndDates = new LocalDate[numberOfCustomers];
        String[] plans = new String[numberOfCustomers];
        LocalDate[] churnDates = new LocalDate[numberOfCustomers];
        String[] churnReasons = new String[numberOfCustomers];

        long startEpochDay = observationStartDate.toEpochDay();
        long endEpochDay = observationEndDate.toEpochDay();
        long extStartEpochDay = extendedStartDate.toEpochDay();
        long extEndEpochDay = extendedEndDate.toEpochDay();

        for (int i = 0; i < numberOfCustomers; i++) {
            customerIds[i] = String.valueOf(i);
            customerNames[i] = "Customer " + i;
            industries[i] = ProjectConfig.INDUSTRY_VALUES[i % ProjectConfig.INDUSTRY_VALUES.length];
            countries[i] = ProjectConfig.COUNTRY_VALUES[i % ProjectConfig.COUNTRY_VALUES.length];
            plans[i] = ProjectConfig.PLAN_VALUES[i % ProjectConfig.PLAN_VALUES.length];

            if (allCustomersFullLifetime) {
                contractStartDates[i] = LocalDate.ofEpochDay(startEpochDay);
                contractEndDates[i] = LocalDate.ofEpochDay(endEpochDay);
            } else if (i < leftCensoredCount) {
                // Left censored: start before observation, end within observation period
                long randomStart = extStartEpochDay + random.nextLong(startEpochDay - extStartEpochDay - 1);
                long randomEnd = startEpochDay + random.nextLong(endEpochDay - startEpochDay);
                contractStartDates[i] = LocalDate.ofEpochDay(randomStart);
                contractEndDates[i] = LocalDate.ofEpochDay(randomEnd);
            } else if (i >= numberOfCustomers - rightCensoredCount) {
                // Right censored: start within observation, end after observation
                long randomStart = startEpochDay + 1 + random.nextLong(endEpochDay - startEpochDay - 1);
                long randomEnd = endEpochDay + random.nextLong(extEndEpochDay - endEpochDay);
                contractStartDates[i] = LocalDate.ofEpochDay(randomStart);
                contractEndDates[i] = LocalDate.ofEpochDay(randomEnd);
            } else {
                // Normal: both dates within observation period
                long randomStart = startEpochDay + 1 + random.nextLong(endEpochDay - startEpochDay - 1);
                long randomEnd = randomStart + random.nextLong(endEpochDay - randomStart);
                contractStartDates[i] = LocalDate.ofEpochDay(randomStart);
                contractEndDates[i] = LocalDate.ofEpochDay(randomEnd);
            }

            Optional<Pair<LocalDate, String>> churn = simulateChurn(
                contractStartDates[i], 
                contractEndDates[i], 
                observationPeriodInYears, 
                churnProbability
            );
            churnDates[i] = churn.map(Pair::getFirst).orElse(null);
            churnReasons[i] = churn.map(Pair::getSecond).orElse(null);
        }

        return Table.create("Customers",
            StringColumn.create(ProjectConfig.CUSTOMER_ID_COLUMN, customerIds),
            StringColumn.create(ProjectConfig.CUSTOMER_NAME_COLUMN, customerNames),
            StringColumn.create(ProjectConfig.INDUSTRY_COLUMN, industries),
            StringColumn.create(ProjectConfig.COUNTRY_COLUMN, countries),
            DateColumn.create(ProjectConfig.CONTRACT_START_DATE_COLUMN, contractStartDates),
            DateColumn.create(ProjectConfig.CONTRACT_END_DATE_COLUMN, contractEndDates),
            StringColumn.create(ProjectConfig.PLAN_COLUMN, plans),
            DateColumn.create(ProjectConfig.CHURN_DATE_COLUMN, churnDates),
            StringColumn.create(ProjectConfig.CHURN_REASON_COLUMN, churnReasons)
        );
    }

    private Optional<Pair<LocalDate, String>> simulateChurn(
        LocalDate contractStartDate, 
        LocalDate contractEndDate,
        int observationPeriodInYears,
        double churnProbability
    ) {
        if (contractStartDate.isAfter(contractEndDate)) {
            throw new IllegalArgumentException("The contract start date cannot be after the contract end date.");
        }

        if (random.nextDouble() > churnProbability) {
            return Optional.empty();
        }

        long observationStart = ProjectConfig.OBSERVATION_START_DATE.toEpochDay();
        long observationEnd = observationStart + (long)(observationPeriodInYears * 365);
        long contractStart = contractStartDate.toEpochDay();
        long contractEnd = contractEndDate.toEpochDay();

        long start = contractStart > observationStart ? contractStart : observationStart;
        long end = contractEnd < observationEnd ? contractEnd : observationEnd;
        long duration = end - start;

        if (duration <= MIN_DURATION_FOR_CHURN) {
            return Optional.empty();
        }

        long earliestChurnDate = start + (long)(EARLIEST_POSSIBLE_CHURN * duration);
        long latestChurnDate = start + (long)(LATEST_POSSIBLE_CHURN * duration);

        if (earliestChurnDate > latestChurnDate) {
            throw new IllegalArgumentException("The earliest churn date cannot be after the latest churn date.");
        }

        LocalDate churnDate = LocalDate.ofEpochDay(earliestChurnDate + random.nextLong(latestChurnDate - earliestChurnDate));
        String churnReason = ProjectConfig.CHURN_REASON_VALUES[random.nextInt(ProjectConfig.CHURN_REASON_VALUES.length)];

        return Optional.of(new Pair<>(churnDate, churnReason));
    }
}
