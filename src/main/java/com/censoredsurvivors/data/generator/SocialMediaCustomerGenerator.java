package com.censoredsurvivors.data.generator;

import tech.tablesaw.api.Table;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.DateColumn;
import java.time.LocalDate;
import java.util.Random;
import com.censoredsurvivors.util.ProjectConfig;

public class SocialMediaCustomerGenerator {
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
    protected Table generateCustomers(
        int numberOfCustomers, 
        double percentOfLeftCensoredCustomers, 
        double percentOfRightCensoredCustomers,
        int observationPeriodInYears
    ) {
        LocalDate observationStartDate = ProjectConfig.OBSERVATION_START_DATE;
        LocalDate observationEndDate = observationStartDate.plusYears(observationPeriodInYears);
        
        LocalDate extendedStartDate = observationStartDate.minusYears(ProjectConfig.EXTENDED_PERIOD_YEARS);
        LocalDate extendedEndDate = observationEndDate.plusYears(ProjectConfig.EXTENDED_PERIOD_YEARS);
        
        int leftCensoredCount = (int) (numberOfCustomers * percentOfLeftCensoredCustomers);
        int rightCensoredCount = (int) (numberOfCustomers * percentOfRightCensoredCustomers);

        Random random = ProjectConfig.RANDOM;

        String[] customerIds = new String[numberOfCustomers];
        String[] customerNames = new String[numberOfCustomers];
        String[] industries = new String[numberOfCustomers];
        String[] countries = new String[numberOfCustomers];
        LocalDate[] contractStartDates = new LocalDate[numberOfCustomers];
        LocalDate[] contractEndDates = new LocalDate[numberOfCustomers];
        String[] plans = new String[numberOfCustomers];

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

            if (i < leftCensoredCount) {
                // Left censored: start before observation, end within observation period
                long randomStart = extStartEpochDay + random.nextLong(observationStartDate.toEpochDay() - extStartEpochDay);
                long randomEnd = startEpochDay + random.nextLong(endEpochDay - startEpochDay);
                contractStartDates[i] = LocalDate.ofEpochDay(randomStart);
                contractEndDates[i] = LocalDate.ofEpochDay(randomEnd);
            } 
            else if (i < leftCensoredCount + rightCensoredCount) {
                // Right censored: start within observation, end after observation
                long randomStart = startEpochDay + random.nextLong(endEpochDay - startEpochDay);
                long randomEnd = endEpochDay + random.nextLong(extEndEpochDay - endEpochDay);
                contractStartDates[i] = LocalDate.ofEpochDay(randomStart);
                contractEndDates[i] = LocalDate.ofEpochDay(randomEnd);
            } 
            else {
                // Normal: both dates within observation period
                long randomStart = startEpochDay + random.nextLong(endEpochDay - startEpochDay);
                long randomEnd = randomStart + random.nextLong(endEpochDay - randomStart);
                contractStartDates[i] = LocalDate.ofEpochDay(randomStart);
                contractEndDates[i] = LocalDate.ofEpochDay(randomEnd);
            }
        }

        return Table.create("Customers",
            StringColumn.create(ProjectConfig.CUSTOMER_ID_COLUMN, customerIds),
            StringColumn.create(ProjectConfig.CUSTOMER_NAME_COLUMN, customerNames),
            StringColumn.create(ProjectConfig.INDUSTRY_COLUMN, industries),
            StringColumn.create(ProjectConfig.COUNTRY_COLUMN, countries),
            DateColumn.create(ProjectConfig.CONTRACT_START_DATE_COLUMN, contractStartDates),
            DateColumn.create(ProjectConfig.CONTRACT_END_DATE_COLUMN, contractEndDates),
            StringColumn.create(ProjectConfig.PLAN_COLUMN, plans)
        );
    }
}
