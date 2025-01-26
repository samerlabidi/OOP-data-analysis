package model;

public class DescriptiveStats {
    private final double mean;
    private final double median;
    private final double standardDeviation;
    private final double min;
    private final double max;

    public DescriptiveStats(double mean, double median, double standardDeviation, double min, double max) {
        this.mean = mean;
        this.median = median;
        this.standardDeviation = standardDeviation;
        this.min = min;
        this.max = max;
    }

    // Getters
    public double getMean() { return mean; }
    public double getMedian() { return median; }
    public double getStandardDeviation() { return standardDeviation; }
    public double getMin() { return min; }
    public double getMax() { return max; }
} 