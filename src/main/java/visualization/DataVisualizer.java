package visualization;

import java.util.List;

import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYDataset;

public interface DataVisualizer {
    JFreeChart createLineChart(String title, String xLabel, String yLabel, XYDataset dataset);
    JFreeChart createHistogram(String title, List<Double> data, int bins);
    JFreeChart createScatterPlot(String title, List<Double> x, List<Double> y);
    void exportChart(JFreeChart chart, String filePath);
} 