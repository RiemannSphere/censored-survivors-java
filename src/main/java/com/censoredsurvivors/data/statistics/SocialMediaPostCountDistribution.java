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
        RandomGenerator randomGenerator = RandomGeneratorFactory.createRandomGenerator(ProjectConfig.RANDOM);
        // Binomial distribution with n = 1 is equivalent to a Bernoulli distribution.
        this.bernoulliDistribution = new BinomialDistribution(randomGenerator, 1, params.frequency());
        this.normalDistribution = new NormalDistribution(randomGenerator, params.mean(), params.stdDev());
    }

    public int sample() {
        return (int) Math.round(
            this.bernoulliDistribution.sample() * 
            Math.max(0, this.normalDistribution.sample())
        );
    }
}
