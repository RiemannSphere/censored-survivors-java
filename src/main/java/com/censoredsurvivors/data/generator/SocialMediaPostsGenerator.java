package com.censoredsurvivors.data.generator;

import tech.tablesaw.api.Table;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.IntColumn;
import com.censoredsurvivors.util.ProjectConfig;

public class SocialMediaPostsGenerator {

    private final Table customers;

    public SocialMediaPostsGenerator(Table customers) {
        this.customers = customers;
    }

    public Table generatePosts(String tableName, int numberOfEntries) {
        Table df = Table.create(tableName);

        String[] customerIds = new String[numberOfEntries];
        String[] customerNames = new String[numberOfEntries];
        String[] channels = new String[numberOfEntries];
        int[] years = new int[numberOfEntries];
        int[] weeks = new int[numberOfEntries];
        int[] postCounts = new int[numberOfEntries];

        for (int i = 0; i < numberOfEntries; i++) {
            customerIds[i] = // TODO: get customer id
            customerNames[i] = // TODO: get customer name
            channels[i] = // TODO: get channel
            years[i] = // TODO: get year
            weeks[i] = // TODO: get week
            postCounts[i] = // TODO: get post count
        }

        df.addColumns(
            StringColumn.create(ProjectConfig.CUSTOMER_ID_COLUMN, customerIds),
            StringColumn.create(ProjectConfig.CUSTOMER_NAME_COLUMN, customerNames),
            StringColumn.create(ProjectConfig.CHANNEL_COLUMN, channels),
            IntColumn.create(ProjectConfig.YEAR_COLUMN, years),
            IntColumn.create(ProjectConfig.WEEK_COLUMN, weeks),
            IntColumn.create(ProjectConfig.POST_COUNT_COLUMN, postCounts)
        );
        
        return df;
    }
}
