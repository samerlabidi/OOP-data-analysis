package visualization;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.jfree.chart.JFreeChart;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class CovidDataVisualizerTest {
    private CovidDataVisualizer visualizer;
    private List<Double> sampleData;
    
    @BeforeEach
    void setUp() {
        visualizer = new CovidDataVisualizer();
        sampleData = Arrays.asList(10.0, 20.0, 30.0, 40.0, 50.0);
    }

    @Test
    void testCreateHistogram() {
        JFreeChart chart = visualizer.createHistogram("Test Histogram", sampleData, 5);
        assertNotNull(chart);
        assertEquals("Test Histogram", chart.getTitle().getText());
    }

    @Test
    void testCreateScatterPlot() {
        List<Double> xData = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0);
        JFreeChart chart = visualizer.createScatterPlot("Test Scatter", xData, sampleData);
        assertNotNull(chart);
        assertEquals("Test Scatter", chart.getTitle().getText());
    }

    @Test
    void testExportChart(@TempDir Path tempDir) {
        JFreeChart chart = visualizer.createHistogram("Test Export", sampleData, 5);
        String filePath = tempDir.resolve("test_chart.png").toString();
        
        assertDoesNotThrow(() -> {
            visualizer.exportChart(chart, filePath);
        });
    }
} 