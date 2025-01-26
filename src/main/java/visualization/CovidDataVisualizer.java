package visualization;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.springframework.stereotype.Service;

@Service
public class CovidDataVisualizer implements DataVisualizer {
    
    @Override
    public JFreeChart createLineChart(String title, String xLabel, String yLabel, XYDataset dataset) {
        return ChartFactory.createXYLineChart(
            title,
            xLabel,
            yLabel,
            dataset
        );
    }

    @Override
    public JFreeChart createHistogram(String title, List<Double> data, int bins) {
        HistogramDataset dataset = new HistogramDataset();
        double[] values = data.stream().mapToDouble(Double::doubleValue).toArray();
        dataset.addSeries("Frequency", values, bins);
        
        return ChartFactory.createHistogram(
            title,
            "Value",
            "Frequency",
            dataset
        );
    }

    @Override
    public JFreeChart createScatterPlot(String title, List<Double> x, List<Double> y) {
        XYSeries series = new XYSeries("Data");
        for (int i = 0; i < x.size(); i++) {
            series.add(x.get(i), y.get(i));
        }
        XYSeriesCollection dataset = new XYSeriesCollection(series);
        
        return ChartFactory.createScatterPlot(
            title,
            "X",
            "Y",
            dataset
        );
    }

    @Override
    public void exportChart(JFreeChart chart, String filePath) {
        try {
            ChartUtils.saveChartAsPNG(
                new File(filePath),
                chart,
                800,  // width
                600   // height
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to export chart: " + e.getMessage(), e);
        }
    }
} 