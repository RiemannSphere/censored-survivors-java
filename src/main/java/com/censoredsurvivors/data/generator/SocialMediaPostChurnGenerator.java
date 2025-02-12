package com.censoredsurvivors.data.generator;

import com.censoredsurvivors.data.model.SocialMediaPostChurnDataSet;

import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.DateColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.api.Row;
import java.time.LocalDate;
import java.util.Random;
import com.censoredsurvivors.util.ProjectConfig;

public class SocialMediaPostChurnGenerator {
    private static final String CUSTOMER_ID_COLUMN = "customerId";
    private static final String CUSTOMER_NAME_COLUMN = "customerName";

    protected Table generatePlatformPosts(Table customers) {
        Table df = Table.create("Platform Posts");
        df.addColumns(
            StringColumn.create(CUSTOMER_ID_COLUMN),
            StringColumn.create(CUSTOMER_NAME_COLUMN),
            StringColumn.create("channel"),
            IntColumn.create("year"),
            IntColumn.create("week"),
            IntColumn.create("postCount")
        );

        return df;
    }

    protected Table generateNativePosts(Table customers) {
        Table df = Table.create("Native Posts");
        df.addColumns(
            StringColumn.create(CUSTOMER_ID_COLUMN),
            StringColumn.create(CUSTOMER_NAME_COLUMN),
            StringColumn.create("channel"),
            IntColumn.create("year"),
            IntColumn.create("week"),
            IntColumn.create("postCount")
        );

        return df;
    }

    public SocialMediaPostChurnDataSet generate() {
        Table customers = generateCustomers(numberOfCustomers);
        Table platformPosts = generatePlatformPosts(customers);
        Table nativePosts = generateNativePosts(customers);

        SocialMediaPostChurnDataSet dataSet = new SocialMediaPostChurnDataSet();
        dataSet.setCustomers(customers);
        dataSet.setPlatformPosts(platformPosts);
        dataSet.setNativePosts(nativePosts);

        return dataSet;
    }
}
