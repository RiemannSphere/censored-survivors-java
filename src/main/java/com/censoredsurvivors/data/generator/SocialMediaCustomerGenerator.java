package com.censoredsurvivors.data.generator;

import tech.tablesaw.api.Table;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.DateColumn;
import java.time.LocalDate;
import java.util.Random;
import com.censoredsurvivors.util.ProjectConfig;

public class SocialMediaCustomerGenerator {
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
            industries[i] = INDUSTRY_VALUES[i % INDUSTRY_VALUES.length];
            countries[i] = COUNTRY_VALUES[i % COUNTRY_VALUES.length];
            plans[i] = PLAN_VALUES[i % PLAN_VALUES.length];

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
            StringColumn.create(CUSTOMER_ID_COLUMN, customerIds),
            StringColumn.create(CUSTOMER_NAME_COLUMN, customerNames),
            StringColumn.create("industry", industries),
            StringColumn.create("country", countries),
            DateColumn.create("contractStartDate", contractStartDates),
            DateColumn.create("contractEndDate", contractEndDates),
            StringColumn.create("plan", plans)
        );
    }
}
