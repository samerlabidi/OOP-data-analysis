package visualization;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.springframework.stereotype.Service;

@Service
public class CovidDataVisualizer implements DataVisualizer {
    
    private void customizeChartAppearance(JFreeChart chart) {
        // Set background
        chart.setBackgroundPaint(Color.WHITE);
        
        // Customize plot
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(new Color(245, 245, 250));
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        plot.setOutlinePaint(Color.DARK_GRAY);
        
        // Customize renderer
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesPaint(0, new Color(41, 128, 185)); // Blue
        renderer.setSeriesStroke(0, new BasicStroke(2.5f));
        renderer.setSeriesShapesVisible(0, true);
        renderer.setSeriesShape(0, new Ellipse2D.Double(-3, -3, 6, 6));
        plot.setRenderer(renderer);
        
        // Customize title
        chart.getTitle().setFont(new Font("Arial", Font.BOLD, 18));
        
        // Customize legend
        LegendTitle legend = chart.getLegend();
        legend.setBackgroundPaint(new Color(245, 245, 245, 200));
        legend.setPosition(RectangleEdge.BOTTOM);
        legend.setBorder(0, 0, 0, 0);
        
        // Customize axes
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setTickLabelFont(new Font("Arial", Font.PLAIN, 12));
        rangeAxis.setLabelFont(new Font("Arial", Font.BOLD, 14));
        
        NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
        domainAxis.setTickLabelFont(new Font("Arial", Font.PLAIN, 12));
        domainAxis.setLabelFont(new Font("Arial", Font.BOLD, 14));
    }
    
    @Override
    public JFreeChart createLineChart(String title, String xLabel, String yLabel, XYDataset dataset) {
        JFreeChart chart = ChartFactory.createXYLineChart(
            title,
            xLabel,
            yLabel,
            dataset,
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        );
        customizeChartAppearance(chart);
        return chart;
    }

    @Override
    public JFreeChart createHistogram(String title, List<Double> data, int bins) {
        HistogramDataset dataset = new HistogramDataset();
        double[] values = data.stream().mapToDouble(Double::doubleValue).toArray();
        dataset.addSeries("Frequency", values, bins);
        
        JFreeChart chart = ChartFactory.createHistogram(
            title,
            "Value",
            "Frequency",
            dataset,
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        );
        customizeChartAppearance(chart);
        return chart;
    }

    @Override
    public JFreeChart createScatterPlot(String title, List<Double> x, List<Double> y) {
        XYSeries series = new XYSeries("Data");
        for (int i = 0; i < x.size(); i++) {
            series.add(x.get(i), y.get(i));
        }
        XYSeriesCollection dataset = new XYSeriesCollection(series);
        
        JFreeChart chart = ChartFactory.createScatterPlot(
            title,
            "X",
            "Y",
            dataset,
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        );
        customizeChartAppearance(chart);
        return chart;
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