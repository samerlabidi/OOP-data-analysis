package analysis;

import java.util.List;

import model.DescriptiveStats;
import model.RegressionModel;
import model.TimeSeriesAnalysis;
import model.TimeSeriesData;

public interface DataAnalyzer {
    DescriptiveStats calculateDescriptiveStats(List<Double> data);
    double calculateCorrelation(List<Double> x, List<Double> y);
    RegressionModel performRegression(List<Double> x, List<Double> y);
    TimeSeriesAnalysis analyzeTimeSeries(List<TimeSeriesData> data);
}