package com.censoredsurvivors.data.statistics;

import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.RandomGeneratorFactory;

import com.censoredsurvivors.data.model.CustomDistributionParams;
import com.censoredsurvivors.util.ProjectConfig;

public class CustomDistribution {
    private BinomialDistribution bernoulliDistribution;
    private NormalDistribution normalDistribution;
    
    private double mean;
    private double stdDev;
    private double frequency;

    /**
     * Generates a distribution for the number of data points.
     * Uses two distributions: 
     * - Normal distribution to generate the number of data points,
     * - Bernoulli distribution to choose whether to generate a data point or not.
     * 
     * @param mean Mean number of data points.
     * @param stdDev Standard deviation of the number of data points.
     * @param frequency Frequency of generating a data point.
     */ 
    public CustomDistribution(CustomDistributionParams params) {
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
