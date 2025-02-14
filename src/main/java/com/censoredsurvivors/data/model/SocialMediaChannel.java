package com.censoredsurvivors.data.model;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.censoredsurvivors.util.ProjectConfig;

/**
 * The social media channels that are supported.
 * 
 * The popularity of the channels is used to determine the probability of a customer posting on a given channel.
 * Probabilities should be normalized so that the sum of all probabilities is 1.
 */
public enum SocialMediaChannel {
    FACEBOOK("Facebook", 0.4),
    INSTAGRAM("Instagram", 0.3),
    TWITTER("Twitter", 0.2),
    LINKEDIN("LinkedIn", 0.05),
    YOUTUBE("YouTube", 0.05);

    private String displayName;
    private double popularity;

    private static final Map<String, SocialMediaChannel> DISPLAY_NAME_MAP;

    static {
        double sum = Arrays.stream(SocialMediaChannel.getAllPopularities()).sum();
        if (sum <= 1.0) {
            throw new IllegalArgumentException("Popularity values must sum to 1");
        }

        DISPLAY_NAME_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(
                SocialMediaChannel::getDisplayName,
                channel -> channel
            ));
    }   

    SocialMediaChannel(String displayName, double popularity) {
        this.displayName = displayName;

        if (popularity < 0 || popularity > 1) {
            throw new IllegalArgumentException("Popularity must be between 0 and 1");
        }

        this.popularity = popularity;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getPopularity() {
        return popularity;
    }

    public static String[] getAllDisplayNames() {
        return Arrays.stream(values())
            .map(SocialMediaChannel::getDisplayName)
            .toArray(String[]::new);
    }

    public static double[] getAllPopularities() {
        return Arrays.stream(values())
            .mapToDouble(SocialMediaChannel::getPopularity)
            .toArray();
    }

    public static List<SocialMediaChannel> getRandomChannelsSubset() {
        return Arrays.stream(SocialMediaChannel.values())
                .filter(channel -> ProjectConfig.RANDOM.nextDouble() < channel.getPopularity()) // Select based on popularity
                .collect(Collectors.toList());
    }

    public static SocialMediaChannel getByDisplayName(String displayName) {
        SocialMediaChannel channel = DISPLAY_NAME_MAP.get(displayName);
        if (channel == null) {
            throw new IllegalArgumentException("Invalid display name: " + displayName);
        }

        return channel;
    }
}
