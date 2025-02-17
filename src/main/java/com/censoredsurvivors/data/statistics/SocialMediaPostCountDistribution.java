package com.censoredsurvivors.data.statistics;

import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.RandomGeneratorFactory;

import com.censoredsurvivors.data.model.SocialMediaPostDistributionParams;
import com.censoredsurvivors.util.ProjectConfig;

public class SocialMediaPostCountDistribution {
    private BinomialDistribution bernoulliDistribution;
    private NormalDistribution normalDistribution;
    
    private double mean;
    private double stdDev;
    private double frequency;

    /**
     * Generates a distribution for the number of posts for a given week.
     * Uses two distributions: 
     * - Normal distribution to generate the number of posts for a given week,
     * - Bernoulli distribution to choose whether to post or not for a given week.
     * 
     * @param mean Mean number of posts per week.
     * @param stdDev Standard deviation of the number of posts per week.
     * @param frequency Frequency of posting = 1 / number of weeks between posts.
     */ 
    public SocialMediaPostCountDistribution(SocialMediaPostDistributionParams params) {
        this.mean = params.mean();
        this.stdDev = params.stdDev();
        this.frequency = params.frequency();

        RandomGenerator randomGenerator = RandomGeneratorFactory.createRandomGenerator(ProjectConfig.RANDOM);
        // Binomial distribution with n = 1 is equivalent to a Bernoulli distribution.
        this.bernoulliDistribution = new BinomialDistribution(randomGenerator, 1, this.frequency);
        this.normalDistribution = new NormalDistribution(randomGenerator, this.mean, this.stdDev);
    }

    public int sample() {
        if (this.bernoulliDistribution.sample() == 0) {
            return 0;
        }

        return (int) Math.round(
            Math.max(0, this.normalDistribution.sample())
        );
    }
}
