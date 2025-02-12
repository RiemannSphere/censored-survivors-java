package com.censoredsurvivors.data.model;

import lombok.Data;

@Data
public class SocialMediaPostChurnDataSet {
    private tech.tablesaw.api.Table customers;
    private tech.tablesaw.api.Table platformPosts;
    private tech.tablesaw.api.Table nativePosts;
}
