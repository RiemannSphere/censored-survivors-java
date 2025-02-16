package com.censoredsurvivors.data.model;

public record SocialMediaPostRule(
    SocialMediaParam param, 
    String paramValue,
    SocialMediaPostDistributionParams postCountDistributionParams
) {}
