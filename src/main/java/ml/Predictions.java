package ml;

public class Predictions {
    private final double[] predictedValues;

    public Predictions(double[] predictedValues) {
        this.predictedValues = predictedValues;
    }

    public double[] getPredictedValues() { return predictedValues; }
} 