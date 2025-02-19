package com.censoredsurvivors.simulation;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;

import com.censoredsurvivors.data.generator.SocialMediaCustomerGenerator;
import com.censoredsurvivors.data.generator.SocialMediaPostsGenerator;
import com.censoredsurvivors.data.model.SocialMediaChannel;
import com.censoredsurvivors.data.model.SocialMediaChurnReason;
import com.censoredsurvivors.data.model.SocialMediaParam;
import com.censoredsurvivors.data.model.CustomDistributionParams;
import com.censoredsurvivors.data.model.SocialMediaPostRule;
import com.censoredsurvivors.data.statistics.ConfusionStatus;
import com.censoredsurvivors.data.statistics.Cusum;
import com.censoredsurvivors.data.statistics.SignalCleaner;
import com.censoredsurvivors.util.ProjectConfig;

import tech.tablesaw.aggregate.AggregateFunctions;
import tech.tablesaw.api.Table;
import tech.tablesaw.selection.Selection;


public class SocialMediaCusumChurnDetector {
    private final WeekFields weekFields = WeekFields.of(Locale.US);

    private final int OBSERVATION_PERIOD_IN_YEARS = 10;
    private final boolean ALL_CUSTOMERS_FULL_LIFETIME = true;

    private class ChannelRules {
        public static final SocialMediaPostRule FACEBOOK =  new SocialMediaPostRule(
            SocialMediaParam.CHANNEL, 
            SocialMediaChannel.FACEBOOK.getDisplayName(), 
            new CustomDistributionParams(200, 20, 0.8)
        );
        public static final SocialMediaPostRule TWITTER =  new SocialMediaPostRule(
            SocialMediaParam.CHANNEL, 
            SocialMediaChannel.TWITTER.getDisplayName(), 
            new CustomDistributionParams(100, 50, 0.8)
        );
        public static final SocialMediaPostRule INSTAGRAM =  new SocialMediaPostRule(
            SocialMediaParam.CHANNEL, 
            SocialMediaChannel.INSTAGRAM.getDisplayName(), 
            new CustomDistributionParams(10, 3, 0.8)
        );
    }

    public record RunSummary(
        ChurnResult[] churnResults,
        int numberOfTruePositives,
        int numberOfFalsePositives,
        int numberOfTrueNegatives,
        int numberOfFalseNegatives
    ) {}

    public record ChurnResult(
        String customerId,
        LocalDate churnDate,
        String churnReason,
        LocalDate detectedChurnDate,
        String detectedChurnReason,
        int detectionErrorInWeeks,
        ConfusionStatus confusionStatus
    ) {}

    public RunSummary run(
        int numberOfCustomers,
        double churnProbability,
        double cusumSmoothing,
        int threshold,
        SignalCleaner.SignalCleaningType signalCleaningType
    ) {
        Table customers = new SocialMediaCustomerGenerator(ALL_CUSTOMERS_FULL_LIFETIME)
            .generateUncensoredCustomers(
                numberOfCustomers,
                OBSERVATION_PERIOD_IN_YEARS,
                churnProbability
            );
        List<SocialMediaPostRule> postRules = List.of(
            ChannelRules.FACEBOOK
            // ChannelRules.TWITTER,
            // ChannelRules.INSTAGRAM
        );
        List<SocialMediaChannel> channels = List.of(
            SocialMediaChannel.FACEBOOK
            // SocialMediaChannel.TWITTER,
            // SocialMediaChannel.INSTAGRAM
        );
        Table posts = new SocialMediaPostsGenerator(customers)
            .generatePosts("Platform Posts", postRules, channels);

        ChurnResult[] churnResults = customers.stream().map(customer -> {
            String customerId = customer.getString(ProjectConfig.CUSTOMER_ID_COLUMN);
            LocalDate churnDate = customer.getDate(ProjectConfig.CHURN_DATE_COLUMN);
            String churnReason = customer.getString(ProjectConfig.CHURN_REASON_COLUMN);

            Selection selectPostsByCustomerId = posts.stringColumn(ProjectConfig.CUSTOMER_ID_COLUMN).isEqualTo(customerId);
            Table customerPosts = posts.where(selectPostsByCustomerId);
            Table weeklyPosts = customerPosts.summarize(
                ProjectConfig.POST_COUNT_COLUMN, 
                AggregateFunctions.sum
            ).by(ProjectConfig.YEAR_COLUMN, ProjectConfig.WEEK_COLUMN);

            double[] postCounts = weeklyPosts.doubleColumn("Sum [" + ProjectConfig.POST_COUNT_COLUMN + "]")
                .asDoubleArray();
            double[] postCountsCleaned = SignalCleaner.clean(postCounts, signalCleaningType);
            double reference = 200;
            double thresholdValue = reference + threshold * 20;

            Cusum cusum = new Cusum(cusumSmoothing);
            Cusum.Result cusumResult = cusum.compute(postCountsCleaned, reference, thresholdValue, true);

            int detectedChurnIndex = cusumResult.anomalyIndex();
            int detectedChurnYear;
            int detectedChurnWeek;
            LocalDate detectedChurnDate;
            // TODO: in the future there will be more reasons for churn
            String detectedChurnReason = SocialMediaChurnReason.POST_COUNT_DROP.getDisplayName();
            
            if (detectedChurnIndex != -1) {
                detectedChurnYear = weeklyPosts.row(detectedChurnIndex).getInt(ProjectConfig.YEAR_COLUMN);
                detectedChurnWeek = weeklyPosts.row(detectedChurnIndex).getInt(ProjectConfig.WEEK_COLUMN);
                detectedChurnDate = LocalDate.now()
                    .withYear(detectedChurnYear)
                    .with(weekFields.weekOfWeekBasedYear(), detectedChurnWeek)
                    .with(weekFields.dayOfWeek(), 1);
            } else {
                detectedChurnDate = null;
            }

            int detectionErrorInWeeks;
            ConfusionStatus confusionStatus;

            if (churnDate != null && detectedChurnDate != null) {
                detectionErrorInWeeks = (int) ChronoUnit.WEEKS.between(churnDate, detectedChurnDate);
                confusionStatus = ConfusionStatus.TRUE_POSITIVE;
            } else if (churnDate == null && detectedChurnDate != null) {
                detectionErrorInWeeks = Integer.MIN_VALUE;
                confusionStatus = ConfusionStatus.FALSE_POSITIVE;
            } else if (churnDate != null && detectedChurnDate == null) {
                detectionErrorInWeeks = Integer.MIN_VALUE;
                confusionStatus = ConfusionStatus.FALSE_NEGATIVE;
            } else {
                detectionErrorInWeeks = Integer.MIN_VALUE;
                confusionStatus = ConfusionStatus.TRUE_NEGATIVE;
            }
            
            return new ChurnResult(
                customerId,
                churnDate,
                churnReason,
                detectedChurnDate,
                detectedChurnReason,
                detectionErrorInWeeks,
                confusionStatus
            );
        }).toArray(ChurnResult[]::new);

        int numberOfTruePositives = 0;
        int numberOfFalsePositives = 0;
        int numberOfTrueNegatives = 0;
        int numberOfFalseNegatives = 0;
        for (ChurnResult churnResult : churnResults) {
            if (churnResult.confusionStatus() == ConfusionStatus.TRUE_POSITIVE) {
                numberOfTruePositives++;
            } else if (churnResult.confusionStatus() == ConfusionStatus.FALSE_POSITIVE) {
                numberOfFalsePositives++;
            } else if (churnResult.confusionStatus() == ConfusionStatus.TRUE_NEGATIVE) {
                numberOfTrueNegatives++;
            } else if (churnResult.confusionStatus() == ConfusionStatus.FALSE_NEGATIVE) {
                numberOfFalseNegatives++;
            }
        }

        return new RunSummary(
            churnResults,
            numberOfTruePositives,
            numberOfFalsePositives,
            numberOfTrueNegatives,
            numberOfFalseNegatives
        );
    }
}
