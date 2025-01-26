package analysis;

import java.util.List;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.springframework.stereotype.Service;

import model.DescriptiveStats;
import model.RegressionModel;
import model.TimeSeriesAnalysis;
import model.TimeSeriesData;

@Service
public class CovidDataAnalyzer implements DataAnalyzer {
    @Override
    public DescriptiveStats calculateDescriptiveStats(List<Double> data) {
        DescriptiveStatistics stats = new DescriptiveStatistics();
        data.forEach(stats::addValue);

        return new DescriptiveStats(
            stats.getMean(),
            stats.getPercentile(50),
            stats.getStandardDeviation(),
            stats.getMin(),
            stats.getMax()
        );
    }

    @Override
    public double calculateCorrelation(List<Double> x, List<Double> y) {
        if (x.size() != y.size()) {
            throw new IllegalArgumentException("Lists must be of equal size");
        }
        
        double[] xArray = x.stream().mapToDouble(d -> d).toArray();
        double[] yArray = y.stream().mapToDouble(d -> d).toArray();
        
        return new PearsonsCorrelation().correlation(xArray, yArray);
    }

    @Override
    public RegressionModel performRegression(List<Double> x, List<Double> y) {
        if (x.size() != y.size()) {
            throw new IllegalArgumentException("Lists must be of equal size");
        }

        SimpleRegression regression = new SimpleRegression();
        for (int i = 0; i < x.size(); i++) {
            regression.addData(x.get(i), y.get(i));
        }

        return new RegressionModel(
            regression.getSlope(),
            regression.getIntercept(),
            regression.getRSquare()
        );
    }

    @Override
    public TimeSeriesAnalysis analyzeTimeSeries(List<TimeSeriesData> data) {
        // Calculate trend using simple linear regression
        SimpleRegression regression = new SimpleRegression();
        for (int i = 0; i < data.size(); i++) {
            regression.addData(i, data.get(i).getValue());
        }

        // Basic seasonality detection (simplified)
        boolean seasonal = detectSeasonality(data);
        double seasonalityStrength = calculateSeasonalityStrength(data);

        return new TimeSeriesAnalysis(
            regression.getSlope(),
            seasonal,
            seasonalityStrength
        );
    }

    private boolean detectSeasonality(List<TimeSeriesData> data) {
        // Simplified seasonality detection
        // In a real implementation, you might use more sophisticated methods
        // like spectral analysis or autocorrelation
        return false; // Placeholder
    }

    private double calculateSeasonalityStrength(List<TimeSeriesData> data) {
        // Simplified seasonality strength calculation
        // In a real implementation, you might use decomposition methods
        return 0.0; // Placeholder
    }
} 