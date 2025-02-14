package com.censoredsurvivors.data.statistics;

import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.distribution.LogNormalDistribution;

import com.censoredsurvivors.data.model.SocialMediaPostDistributionParams;

public class PostCountDistribution {
    private BinomialDistribution bernoulliDistribution;
    private LogNormalDistribution logNormalDistribution;
    
    /**
     * Generates a distribution for the number of posts for a given week.
     * 
     * @param mean Mean number of posts per week.
     * @param stdDev Standard deviation of the number of posts per week.
     * @param frequency Frequency of posting = 1 / number of weeks between posts.
     */ 
    public PostCountDistribution(SocialMediaPostDistributionParams params) {
        // Binomial distribution with n = 1 is equivalent to a Bernoulli distribution.
        this.bernoulliDistribution = new BinomialDistribution(1, params.frequency());
        double mu = params.mean();
        double sigma = params.stdDev();

        // Convert normal parameters to log-normal parameters to stay close to the normal distribution values.
        double logNormalMean = Math.log(mu * mu / Math.sqrt(mu * mu + sigma * sigma));
        double logNormalStdDev = Math.sqrt(Math.log(1 + (sigma * sigma) / (mu * mu)));

        this.logNormalDistribution = new LogNormalDistribution(logNormalMean, logNormalStdDev);
    }

    public int sample() {
        return (int) Math.round(
            this.bernoulliDistribution.sample() * 
            this.logNormalDistribution.sample()
        );
    }
}
