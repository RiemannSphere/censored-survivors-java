package com.censoredsurvivors.data.generator;

import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import com.censoredsurvivors.util.ProjectConfig;

public class SocialMediaPostChurnGenerator {

    protected Table generatePlatformPosts(Table customers) {
        Table df = Table.create("Platform Posts");
        df.addColumns(
            StringColumn.create(ProjectConfig.CUSTOMER_ID_COLUMN),
            StringColumn.create(ProjectConfig.CUSTOMER_NAME_COLUMN),
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
            StringColumn.create(ProjectConfig.CUSTOMER_ID_COLUMN),
            StringColumn.create(ProjectConfig.CUSTOMER_NAME_COLUMN),
            StringColumn.create("channel"),
            IntColumn.create("year"),
            IntColumn.create("week"),
            IntColumn.create("postCount")
        );

        return df;
    }
}
