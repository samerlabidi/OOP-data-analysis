package model;

public class TimeSeriesAnalysis {
    private final double trend;
    private final boolean seasonal;
    private final double seasonalityStrength;

    public TimeSeriesAnalysis(double trend, boolean seasonal, double seasonalityStrength) {
        this.trend = trend;
        this.seasonal = seasonal;
        this.seasonalityStrength = seasonalityStrength;
    }

    // Getters
    public double getTrend() { return trend; }
    public boolean isSeasonal() { return seasonal; }
    public double getSeasonalityStrength() { return seasonalityStrength; }
} 