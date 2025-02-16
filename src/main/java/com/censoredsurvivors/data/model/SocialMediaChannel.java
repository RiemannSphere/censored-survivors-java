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
 */
public enum SocialMediaChannel {
    FACEBOOK("Facebook", 0.8),
    INSTAGRAM("Instagram", 0.6),
    TIKTOK("TikTok", 0.4),
    TWITTER("Twitter", 0.3),
    LINKEDIN("LinkedIn", 0.3),
    YOUTUBE("YouTube", 0.1);

    private String displayName;
    private double popularity;

    private static final Map<String, SocialMediaChannel> DISPLAY_NAME_MAP;

    static {
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
                .filter(channel -> ProjectConfig.RANDOM.nextDouble() < channel.getPopularity())
                .collect(Collectors.collectingAndThen(
                    Collectors.toList(),
                    list -> list.isEmpty() ? Arrays.asList(FACEBOOK) : list
                ));
    }

    public static SocialMediaChannel getByDisplayName(String displayName) {
        SocialMediaChannel channel = DISPLAY_NAME_MAP.get(displayName);
        if (channel == null) {
            throw new IllegalArgumentException("Invalid display name: " + displayName);
        }

        return channel;
    }
}
