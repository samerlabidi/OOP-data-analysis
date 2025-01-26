package analysis;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import model.DescriptiveStats;
import model.RegressionModel;

public class CovidDataAnalyzerTest {
    private CovidDataAnalyzer analyzer;
    private List<Double> sampleData;
    private List<Double> xData;
    private List<Double> yData;

    @BeforeEach
    void setUp() {
        analyzer = new CovidDataAnalyzer();
        sampleData = Arrays.asList(10.0, 20.0, 30.0, 40.0, 50.0);
        xData = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0);
        yData = Arrays.asList(2.0, 4.0, 6.0, 8.0, 10.0);
    }

    @Test
    void testCalculateDescriptiveStats() {
        DescriptiveStats stats = analyzer.calculateDescriptiveStats(sampleData);
        
        assertEquals(30.0, stats.getMean(), 0.001);
        assertEquals(30.0, stats.getMedian(), 0.001);
        assertEquals(15.811, stats.getStandardDeviation(), 0.001);
        assertEquals(10.0, stats.getMin(), 0.001);
        assertEquals(50.0, stats.getMax(), 0.001);
    }

    @Test
    void testCalculateCorrelation() {
        double correlation = analyzer.calculateCorrelation(xData, yData);
        assertEquals(1.0, correlation, 0.001); // Perfect correlation
    }

    @Test
    void testPerformRegression() {
        RegressionModel model = analyzer.performRegression(xData, yData);
        
        assertEquals(2.0, model.getSlope(), 0.001);
        assertEquals(0.0, model.getIntercept(), 0.001);
        assertEquals(1.0, model.getRSquared(), 0.001);
    }

    @Test
    void testInvalidDataSizes() {
        List<Double> shorterList = Arrays.asList(1.0, 2.0);
        
        assertThrows(IllegalArgumentException.class, () -> {
            analyzer.calculateCorrelation(xData, shorterList);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            analyzer.performRegression(xData, shorterList);
        });
    }
} 