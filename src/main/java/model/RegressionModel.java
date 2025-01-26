package model;

public class RegressionModel {
    private final double slope;
    private final double intercept;
    private final double rSquared;

    public RegressionModel(double slope, double intercept, double rSquared) {
        this.slope = slope;
        this.intercept = intercept;
        this.rSquared = rSquared;
    }

    public double predict(double x) {
        return slope * x + intercept;
    }

    // Getters
    public double getSlope() { return slope; }
    public double getIntercept() { return intercept; }
    public double getRSquared() { return rSquared; }
} 