## To Refactor
1. Extract all the distribution sampling to a separate util in order to test it better.

## To Unit Test
1. Do we scale the mean and the stdDev for the Log-Normal distribution correctly? Plot both and compare.
2. How Bernoulli changes the overall mean? The frequency should probably scale the mean.
